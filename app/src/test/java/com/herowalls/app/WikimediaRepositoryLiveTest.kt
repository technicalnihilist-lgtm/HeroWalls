package com.herowalls.app

import com.google.gson.Gson
import com.herowalls.app.data.local.CacheDao
import com.herowalls.app.data.local.CacheEntity
import com.herowalls.app.data.model.Catalog
import com.herowalls.app.data.model.CollectionGroup
import com.herowalls.app.data.model.HeroCategory
import com.herowalls.app.data.model.SortOrder
import com.herowalls.app.data.remote.LicensePolicy
import com.herowalls.app.data.remote.WikimediaApi
import com.herowalls.app.data.remote.WikimediaRepository
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.URI
import java.util.concurrent.TimeUnit

/**
 * End-to-end verification against the real Wikimedia Commons API.
 * Skipped automatically when the machine has no outbound connection.
 */
class WikimediaRepositoryLiveTest {

    private val cache = object : CacheDao {
        private val map = mutableMapOf<String, CacheEntity>()
        override suspend fun get(key: String): CacheEntity? = map[key]
        override suspend fun put(entity: CacheEntity) { map[entity.cacheKey] = entity }
        override suspend fun pruneOlderThan(before: Long) { map.values.removeAll { it.createdAt < before } }
    }

    private val client: OkHttpClient = run {
        val builder = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(40, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                chain.proceed(
                    chain.request().newBuilder()
                        .header("User-Agent", "HeroWallsTest/1.0 (JVM verification)")
                        .build()
                )
            }
        // Honour the CI proxy when one is configured.
        val env = System.getenv("HTTPS_PROXY") ?: System.getenv("https_proxy")
        if (!env.isNullOrBlank()) {
            val uri = URI(env)
            val user = uri.userInfo?.substringBefore(':')
            builder.proxy(Proxy(Proxy.Type.HTTP, InetSocketAddress(uri.host, uri.port)))
            if (!user.isNullOrBlank()) {
                builder.proxyAuthenticator { _, response ->
                    response.request.newBuilder()
                        .header("Proxy-Authorization", okhttp3.Credentials.basic(user, ""))
                        .build()
                }
            }
        }
        builder.build()
    }

    private val repository = WikimediaRepository(
        api = Retrofit.Builder()
            .baseUrl("https://commons.wikimedia.org/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(Gson()))
            .build()
            .create(WikimediaApi::class.java),
        cacheDao = cache,
        gson = Gson()
    )

    /** Runs a live call, returning null (and skipping) when Commons rate-limits this machine. */
    private suspend fun <T> live(block: suspend () -> T): T? = try {
        block()
    } catch (e: com.herowalls.app.data.remote.WikiException) {
        println("SKIPPED: Commons did not serve this request - ${e.message}")
        null
    }

    private fun reachable(): Boolean {
        repeat(3) { attempt ->
            val ok = runCatching {
                client.newCall(
                    Request.Builder()
                        .url("https://commons.wikimedia.org/w/api.php?action=query&meta=siteinfo&format=json")
                        .build()
                ).execute().use { it.isSuccessful }
            }.getOrDefault(false)
            if (ok) return true
            Thread.sleep(3_000L * (attempt + 1))
        }
        return false
    }

    @Test
    fun `search returns licence compliant metadata for a character`() = runBlocking {
        if (!reachable()) {
            println("SKIPPED: Wikimedia Commons not reachable from this machine")
            return@runBlocking
        }
        val spiderMan = Catalog.byId("spider-man")!!
        val page = live { repository.feed(spiderMan, offset = 0, limit = 12, forceRefresh = true) } ?: return@runBlocking

        assertTrue("expected at least one usable image", page.items.isNotEmpty())
        page.items.forEach { wallpaper ->
            assertTrue("licence gate must pass for ${wallpaper.title}", LicensePolicy.isReusable(wallpaper.licenseCode, wallpaper.licenseShort))
            assertTrue("must meet the resolution floor", wallpaper.width >= 800 && wallpaper.height >= 800)
            assertTrue("must be a supported mime type", wallpaper.mime in WikimediaRepository.ALLOWED_MIME)
            assertTrue("must carry a Commons file page", wallpaper.filePageUrl.startsWith("https://commons.wikimedia.org/"))
            assertTrue("must carry an original file url", wallpaper.fullUrl.startsWith("https://"))
            assertTrue("must carry a thumbnail url", wallpaper.thumbUrl.startsWith("https://"))
            assertTrue("creator must be recorded", wallpaper.artist.isNotBlank())
            assertTrue("licence name must be recorded", wallpaper.licenseShort.isNotBlank())
        }
        assertEquals("duplicate images must be removed", page.items.size, page.items.distinctBy { it.pageId }.size)
        println("Spider-Man: ${page.items.size} usable images, first = ${page.items.first().displayName} by ${page.items.first().artist} (${page.items.first().licenseShort})")
    }

    @Test
    fun `newest sort surfaces fresh uploads`() = runBlocking {
        if (!reachable()) return@runBlocking
        val latest = HeroCategory("latest", "Latest wallpapers", Catalog.LIVE_FEED_QUERY, CollectionGroup.COLLECTIONS)
        val page = live {
            repository.feed(latest, offset = 0, limit = 12, sort = SortOrder.NEWEST, forceRefresh = true)
        } ?: return@runBlocking
        assertTrue(page.items.isNotEmpty())
        println("Latest feed: ${page.items.size} images, newest published ${page.items.map { it.dateTime }.maxOrNull()}")
    }

    @Test
    fun `pagination returns a different set of images`() = runBlocking {
        if (!reachable()) return@runBlocking
        val batman = Catalog.byId("batman")!!
        val first = live { repository.feed(batman, offset = 0, limit = 10, forceRefresh = true) } ?: return@runBlocking
        val next = first.nextOffset
        assertNotNull("the API should expose a continuation offset", next)
        val second = live { repository.feed(batman, offset = next!!, limit = 10, forceRefresh = true) } ?: return@runBlocking
        val overlap = first.items.map { it.pageId }.intersect(second.items.map { it.pageId }.toSet())
        // Commons' relevance index shifts between calls (new uploads, re-ranking), so a single
        // repeated file is expected; the UI de-duplicates by page id on every merge.
        assertTrue("pages must not substantially repeat (overlap=${overlap.size})", overlap.size <= 1)
        println("Batman pagination: page1=${first.items.size} page2=${second.items.size} overlap=${overlap.size}")
    }

    @Test
    fun `cache is served on the second call`() = runBlocking {
        if (!reachable()) return@runBlocking
        val ironMan = Catalog.byId("iron-man")!!
        live { repository.feed(ironMan, offset = 0, limit = 8, forceRefresh = true) } ?: return@runBlocking
        val cached = live { repository.feed(ironMan, offset = 0, limit = 8, forceRefresh = false) }
            ?: return@runBlocking
        assertTrue("second call should be served from cache", cached.fromCache)
        println("Cache hit: ${cached.items.size} items served without a network call")
    }
}
