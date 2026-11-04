package com.herowalls.app.data.remote

import com.google.gson.Gson
import com.herowalls.app.data.local.CacheDao
import com.herowalls.app.data.local.CacheEntity
import com.herowalls.app.data.model.CollectionGroup
import com.herowalls.app.data.model.FeedPage
import com.herowalls.app.data.model.HeroCategory
import com.herowalls.app.data.model.SortOrder
import com.herowalls.app.data.model.Wallpaper
import com.herowalls.app.util.Text
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.net.URLEncoder
import java.util.Locale

class WikiException(message: String, val retryable: Boolean = true, cause: Throwable? = null) :
    Exception(message, cause)

/**
 * Live Wikimedia Commons data source.
 *
 * Responsibilities:
 *  - turn a category into a Commons search (never a hard-coded image list),
 *  - fetch imageinfo + extmetadata in batches,
 *  - enforce the licence gate, resolution floor and relevance guard,
 *  - de-duplicate by Commons page id,
 *  - retry with backoff (Commons rate-limits aggressive clients with HTTP 429),
 *  - cache pages in Room so browsing works offline.
 */
class WikimediaRepository(
    private val api: WikimediaApi,
    private val cacheDao: CacheDao,
    private val gson: Gson
) {

    /** Commons asks clients not to hammer the API; all calls are serialised through one gate. */
    private val gate = Mutex()

    /** Wall-clock time of the last outbound call, used to keep a polite gap between requests. */
    private var lastCallAt = 0L

    suspend fun feed(
        category: HeroCategory,
        offset: Int,
        limit: Int = PAGE_SIZE,
        sort: SortOrder = SortOrder.RELEVANCE,
        minSide: Int = DEFAULT_MIN_SIDE,
        forceRefresh: Boolean = false,
        ttlMillis: Long = CACHE_TTL
    ): FeedPage {
        val cacheKey = buildKey(category.searchTerm, offset, limit, sort, minSide)
        if (!forceRefresh) {
            readCache(cacheKey, ttlMillis)?.let { return it.copy(fromCache = true) }
        }
        return try {
            val page = fetchFromNetwork(category, offset, limit, sort, minSide)
            writeCache(cacheKey, page)
            page
        } catch (e: Exception) {
            // Network failed: serve stale cache if we have any, otherwise report the error.
            readCache(cacheKey, Long.MAX_VALUE)?.copy(fromCache = true)?.let { return it }
            throw e
        }
    }

    /** A deterministic "wallpaper of the day" that still comes from live search results. */
    suspend fun dailyWallpaper(): Wallpaper? {
        val day = (System.currentTimeMillis() / 86_400_000L).toInt()
        val cacheKey = "daily:$day"
        readCache(cacheKey, 20 * 60 * 60 * 1000L)?.items?.firstOrNull()?.let { return it }
        val offset = (day % 6) * PAGE_SIZE
        val page = feed(
            category = HeroCategory("daily", "Daily Hero", "superhero comic", CollectionGroup.COLLECTIONS),
            offset = offset,
            limit = PAGE_SIZE,
            sort = SortOrder.RELEVANCE,
            minSide = 1200,
            ttlMillis = 20 * 60 * 60 * 1000L
        )
        val pick = page.items.firstOrNull() ?: return null
        writeCache(cacheKey, page.copy(items = listOf(pick)))
        return pick
    }

    /** Rebuild a wallpaper straight from its Commons page id (used for deep links). */
    suspend fun byPageId(pageId: Long): Wallpaper? = gate.withLock {
        runCatching {
            val info = retrying { paced { api.imageInfoByPageId(pageId, THUMB_WIDTH) } }
            info.query?.pages?.firstOrNull()?.toWallpaper(
                HeroCategory("lookup", "Wikimedia Commons", "", CollectionGroup.COLLECTIONS),
                minSide = 0
            )
        }.getOrNull()
    }

    private suspend fun fetchFromNetwork(
        category: HeroCategory,
        offset: Int,
        limit: Int,
        sort: SortOrder,
        minSide: Int
    ): FeedPage = gate.withLock {
        val search = retrying { paced { api.searchFiles(searchQuery(category), limit, offset, sort.apiValue) } }
        search.error?.let { throw WikiException("Commons error: ${it.info ?: it.code}") }

        val hits = search.query?.search.orEmpty()
        if (hits.isEmpty()) {
            return@withLock FeedPage(emptyList(), null, true, false, 0)
        }

        val titles = hits.mapNotNull { it.title }.distinct()
        val wallpapers = ArrayList<Wallpaper>(titles.size)

        titles.chunked(MAX_TITLES_PER_CALL).forEachIndexed { index, chunk ->
            if (index > 0) delay(120) // stay polite between batches
            val info = retrying { paced { api.imageInfo(chunk.joinToString("|"), THUMB_WIDTH) } }
            info.error?.let { throw WikiException("Commons error: ${it.info ?: it.code}") }
            val pages = info.query?.pages.orEmpty()
            pages.forEach { dto ->
                dto.toWallpaper(category, minSide)?.let { wallpapers.add(it) }
            }
        }

        // Character categories need the character's name to appear in the file title or
        // description, otherwise broad searches return unrelated photos. Collection
        // categories are thematic and keep everything that passes the licence gate.
        val token = Text.relevanceToken(category.name)
        val strict = category.group != CollectionGroup.COLLECTIONS

        val filtered = wallpapers
            .asSequence()
            .filter { it.mime in ALLOWED_MIME }
            .filter { it.width >= minSide && it.height >= minSide }
            .filter { w ->
                if (!strict || token.isBlank()) true
                else (w.title + " " + w.description).lowercase(Locale.US).contains(token)
            }
            .distinctBy { it.pageId }
            .toList()

        val next = search.continuation?.srOffset
        val endReached = next == null || next <= offset || hits.size < limit
        FeedPage(
            items = filtered,
            nextOffset = if (endReached) null else next,
            endReached = endReached,
            fromCache = false,
            totalScanned = wallpapers.size
        )
    }

    private fun searchQuery(category: HeroCategory): String = buildString {
        append(category.searchTerm)
        append(" filetype:bitmap")
    }

    /** Keeps at least [MIN_GAP_MS] between outbound requests so Commons never sees a burst. */
    private suspend fun <T> paced(block: suspend () -> T): T {
        val gap = MIN_GAP_MS - (System.currentTimeMillis() - lastCallAt)
        if (gap > 0) delay(gap)
        lastCallAt = System.currentTimeMillis()
        return block()
    }

    private suspend fun <T> retrying(block: suspend () -> T): T {
        var lastError: Exception? = null
        repeat(MAX_ATTEMPTS) { attempt ->
            try {
                return block()
            } catch (e: Exception) {
                lastError = e
                val isRateLimited = e.message?.contains("429") == true ||
                    e.message?.contains("Too Many Requests", ignoreCase = true) == true
                if (attempt == MAX_ATTEMPTS - 1) throw WikiException(
                    friendlyMessage(e), retryable = true, cause = e
                )
                // HTTP 429 needs a real pause; anything else gets a short backoff.
                delay(if (isRateLimited) RATE_LIMIT_BACKOFF_MS * (attempt + 1) else 700L * (attempt + 1))
            }
        }
        throw WikiException(friendlyMessage(lastError), true, lastError)
    }

    private fun friendlyMessage(e: Exception?): String = when {
        e == null -> "Unknown error"
        e.message?.contains("429") == true ->
            "Wikimedia is rate-limiting requests right now. Please try again in a few seconds."
        e.message?.contains("Unable to resolve host") == true ||
            e.message?.contains("timeout", true) == true ->
            "No connection to Wikimedia Commons. Check your network and retry."
        else -> "Could not reach Wikimedia Commons. ${e.message ?: ""}".trim()
    }

    private fun PageDto.toWallpaper(
        category: HeroCategory,
        minSide: Int
    ): Wallpaper? {
        val info = imageinfo?.firstOrNull() ?: return null
        val pageId = pageid ?: return null
        val title = title ?: return null
        val em = info.extmetadata.orEmpty()

        val licenseCode = em.meta("License")
        val licenseShort = em.meta("LicenseShortName")
        if (!LicensePolicy.isReusable(licenseCode, licenseShort)) return null

        val full = info.url?.substringBefore('?') ?: return null
        val thumb = info.thumburl?.substringBefore('?') ?: full
        val width = info.width ?: 0
        val height = info.height ?: 0
        if (width < minSide || height < minSide) return null

        val description = Text.stripHtml(em.meta("ImageDescription"))
        val credit = Text.stripHtml(em.meta("Credit"))
        val artist = Text.stripHtml(em.meta("Artist")).ifBlank { Wallpaper.UNKNOWN }
        val filePage = info.descriptionurl?.substringBefore('?')
            ?: "https://commons.wikimedia.org/wiki/" + URLEncoder.encode(title.replace(' ', '_'), "UTF-8")
        val restrictions = LicensePolicy.restrictionNote(em.meta("Restrictions"))

        val derived = credit.contains("extracted", ignoreCase = true) ||
            description.contains("extracted from", ignoreCase = true)
        val modifications = buildString {
            append("No edits applied by HeroWalls - the file is displayed as published on Wikimedia Commons.")
            if (derived) append(" Commons notes this file is derived (extracted) from another Commons file.")
        }

        return Wallpaper(
            pageId = pageId,
            title = title,
            displayName = Text.fileTitleToName(title),
            characterName = category.name,
            categoryId = category.id,
            thumbUrl = thumb,
            previewUrl = thumb,
            fullUrl = full,
            width = width,
            height = height,
            mime = info.mime.orEmpty(),
            sizeBytes = info.size ?: 0L,
            artist = artist,
            credit = credit,
            description = description,
            licenseShort = licenseShort.ifBlank { licenseCode },
            licenseCode = licenseCode,
            licenseUrl = em.meta("LicenseUrl"),
            restrictions = restrictions,
            modifications = modifications,
            filePageUrl = filePage,
            uploader = info.user.orEmpty(),
            dateTime = info.timestamp ?: "",
            attributionRequired = em.meta("AttributionRequired").equals("true", ignoreCase = true)
        )
    }

    private suspend fun readCache(key: String, ttl: Long): FeedPage? {
        val row = cacheDao.get(key) ?: return null
        val age = System.currentTimeMillis() - row.createdAt
        if (age > ttl) return null
        return runCatching { gson.fromJson(row.payload, FeedPage::class.java) }.getOrNull()
    }

    private suspend fun writeCache(key: String, page: FeedPage) {
        runCatching {
            cacheDao.put(
                CacheEntity(
                    cacheKey = key,
                    payload = gson.toJson(page),
                    createdAt = System.currentTimeMillis()
                )
            )
        }
    }

    private fun buildKey(term: String, offset: Int, limit: Int, sort: SortOrder, minSide: Int) =
        "search|$term|$offset|$limit|${sort.apiValue}|$minSide"

    companion object {
        const val PAGE_SIZE = 24
        const val THUMB_WIDTH = 1280
        const val DEFAULT_MIN_SIDE = 800
        const val CACHE_TTL = 15 * 60 * 1000L
        private const val MAX_ATTEMPTS = 4
        private const val MIN_GAP_MS = 350L
        private const val RATE_LIMIT_BACKOFF_MS = 4_000L
        private const val MAX_TITLES_PER_CALL = 40
        val ALLOWED_MIME = setOf("image/jpeg", "image/png", "image/webp")
    }
}

/** Read a Commons extmetadata field as plain text, tolerating mixed JSON types. */
private fun Map<String, ExtMeta>?.meta(key: String): String = this?.get(key)?.value.asPlainText()
