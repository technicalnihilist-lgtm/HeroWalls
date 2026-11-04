package com.herowalls.app.data.local

import com.google.gson.Gson
import com.herowalls.app.data.model.Wallpaper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Favourites, offline downloads, recently viewed and recent searches. */
class LibraryRepository(
    private val db: AppDatabase,
    private val gson: Gson
) {

    fun observeFavorites(): Flow<List<Wallpaper>> =
        db.favoriteDao().observeAll().map { rows -> rows.mapNotNull { it.toWallpaper() } }

    fun observeFavoriteIds(): Flow<Set<Long>> =
        db.favoriteDao().observeIds().map { it.toSet() }

    fun observeIsFavorite(pageId: Long): Flow<Boolean> = db.favoriteDao().observeIsFavorite(pageId)

    suspend fun isFavorite(pageId: Long): Boolean = db.favoriteDao().isFavorite(pageId)

    suspend fun toggleFavorite(wallpaper: Wallpaper): Boolean {
        val currently = db.favoriteDao().isFavorite(wallpaper.pageId)
        return if (currently) {
            db.favoriteDao().delete(wallpaper.pageId)
            false
        } else {
            db.favoriteDao().insert(
                FavoriteEntity(
                    pageId = wallpaper.pageId,
                    payload = gson.toJson(wallpaper),
                    addedAt = System.currentTimeMillis()
                )
            )
            true
        }
    }

    fun observeDownloads(): Flow<List<DownloadedWallpaper>> =
        db.downloadDao().observeAll().map { rows ->
            rows.mapNotNull { row ->
                row.toWallpaper()?.let { DownloadedWallpaper(it, row.filePath, row.bytes, row.downloadedAt) }
            }
        }

    fun observeDownloadedIds(): Flow<Set<Long>> = db.downloadDao().observeIds().map { it.toSet() }

    fun observeDownload(pageId: Long): Flow<DownloadedWallpaper?> =
        db.downloadDao().observe(pageId).map { row ->
            row?.let {
                it.toWallpaper()?.let { w -> DownloadedWallpaper(w, it.filePath, it.bytes, it.downloadedAt) }
            }
        }

    suspend fun recordDownload(wallpaper: Wallpaper, filePath: String, bytes: Long) {
        db.downloadDao().insert(
            DownloadEntity(
                pageId = wallpaper.pageId,
                payload = gson.toJson(wallpaper),
                filePath = filePath,
                bytes = bytes,
                downloadedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun removeDownload(pageId: Long) = db.downloadDao().delete(pageId)

    suspend fun findDownload(pageId: Long): DownloadEntity? = db.downloadDao().find(pageId)

    fun observeRecentlyViewed(): Flow<List<Wallpaper>> =
        db.recentDao().observeAll().map { rows -> rows.mapNotNull { it.toWallpaper() } }

    suspend fun recordView(wallpaper: Wallpaper) {
        db.recentDao().insert(
            RecentEntity(
                pageId = wallpaper.pageId,
                payload = gson.toJson(wallpaper),
                viewedAt = System.currentTimeMillis()
            )
        )
    }

    fun observeRecentSearches(): Flow<List<String>> =
        db.searchDao().observeAll().map { rows -> rows.map { it.query } }

    suspend fun recordSearch(query: String) {
        val trimmed = query.trim()
        if (trimmed.length < 2) return
        db.searchDao().insert(SearchEntity(trimmed, System.currentTimeMillis()))
    }

    suspend fun clearSearches() = db.searchDao().clear()

    private fun FavoriteEntity.toWallpaper(): Wallpaper? =
        runCatching { gson.fromJson(payload, Wallpaper::class.java) }.getOrNull()

    private fun RecentEntity.toWallpaper(): Wallpaper? =
        runCatching { gson.fromJson(payload, Wallpaper::class.java) }.getOrNull()

    private fun DownloadEntity.toWallpaper(): Wallpaper? =
        runCatching { gson.fromJson(payload, Wallpaper::class.java) }.getOrNull()
}

data class DownloadedWallpaper(
    val wallpaper: Wallpaper,
    val filePath: String,
    val bytes: Long,
    val downloadedAt: Long
)
