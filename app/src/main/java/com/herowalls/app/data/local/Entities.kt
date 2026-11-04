package com.herowalls.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Favourites keep the full wallpaper metadata so credits work offline. */
@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val pageId: Long,
    val payload: String,
    val addedAt: Long
)

/** Offline copies of downloaded wallpapers. */
@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey val pageId: Long,
    val payload: String,
    val filePath: String,
    val bytes: Long,
    val downloadedAt: Long
)

@Entity(tableName = "recently_viewed")
data class RecentEntity(
    @PrimaryKey val pageId: Long,
    val payload: String,
    val viewedAt: Long
)

/** Cached API pages so the app opens instantly and works with no connection. */
@Entity(tableName = "api_cache")
data class CacheEntity(
    @PrimaryKey val cacheKey: String,
    val payload: String,
    val createdAt: Long
)

@Entity(tableName = "recent_searches")
data class SearchEntity(
    @PrimaryKey val query: String,
    val lastUsedAt: Long
)
