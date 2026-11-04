package com.herowalls.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {
    @Query("SELECT * FROM favorites ORDER BY addedAt DESC")
    fun observeAll(): Flow<List<FavoriteEntity>>

    @Query("SELECT pageId FROM favorites")
    fun observeIds(): Flow<List<Long>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE pageId = :pageId)")
    fun observeIsFavorite(pageId: Long): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE pageId = :pageId)")
    suspend fun isFavorite(pageId: Long): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE pageId = :pageId")
    suspend fun delete(pageId: Long)
}

@Dao
interface DownloadDao {
    @Query("SELECT * FROM downloads ORDER BY downloadedAt DESC")
    fun observeAll(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE pageId = :pageId LIMIT 1")
    suspend fun find(pageId: Long): DownloadEntity?

    @Query("SELECT * FROM downloads WHERE pageId = :pageId LIMIT 1")
    fun observe(pageId: Long): Flow<DownloadEntity?>

    @Query("SELECT pageId FROM downloads")
    fun observeIds(): Flow<List<Long>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: DownloadEntity)

    @Query("DELETE FROM downloads WHERE pageId = :pageId")
    suspend fun delete(pageId: Long)
}

@Dao
interface RecentDao {
    @Query("SELECT * FROM recently_viewed ORDER BY viewedAt DESC LIMIT :limit")
    fun observeAll(limit: Int = 30): Flow<List<RecentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: RecentEntity)
}

@Dao
interface CacheDao {
    @Query("SELECT * FROM api_cache WHERE cacheKey = :key LIMIT 1")
    suspend fun get(key: String): CacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(entity: CacheEntity)

    @Query("DELETE FROM api_cache WHERE createdAt < :before")
    suspend fun pruneOlderThan(before: Long)
}

@Dao
interface SearchDao {
    @Query("SELECT * FROM recent_searches ORDER BY lastUsedAt DESC LIMIT 12")
    fun observeAll(): Flow<List<SearchEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: SearchEntity)

    @Query("DELETE FROM recent_searches")
    suspend fun clear()
}
