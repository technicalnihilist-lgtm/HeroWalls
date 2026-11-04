package com.herowalls.app.work

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.herowalls.app.HeroWallsApp
import com.herowalls.app.data.local.DownloadedWallpaper
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

/**
 * Background maintenance:
 *  - refreshes the cached daily wallpaper once a day,
 *  - keeps favourite wallpapers available offline.
 * Both run only on unmetered-capable connections.
 */
class DailyWallpaperWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? HeroWallsApp ?: return Result.success()
        return runCatching {
            app.container.wikimedia.dailyWallpaper()
            Result.success()
        }.getOrElse { Result.retry() }
    }
}

class OfflineSyncWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? HeroWallsApp ?: return Result.success()
        val container = app.container
        return runCatching {
            val favorites = container.library.observeFavorites().first()
            val existing = container.library.observeDownloads().first().map { it.wallpaper.pageId }.toSet()
            favorites.filterNot { it.pageId in existing }.take(MAX_PER_RUN).forEach { wallpaper ->
                container.downloader.download(wallpaper)
            }
            Result.success()
        }.getOrElse { Result.retry() }
    }

    private companion object {
        const val MAX_PER_RUN = 8
    }
}

object HeroWallsWork {

    private const val DAILY = "herowalls-daily-wallpaper"
    private const val OFFLINE = "herowalls-offline-sync"

    fun schedule(context: Context) {
        val manager = WorkManager.getInstance(context)
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        manager.enqueueUniquePeriodicWork(
            DAILY,
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<DailyWallpaperWorker>(6, TimeUnit.HOURS)
                .setConstraints(constraints)
                .build()
        )

        manager.enqueueUniquePeriodicWork(
            OFFLINE,
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<OfflineSyncWorker>(12, TimeUnit.HOURS)
                .setConstraints(constraints)
                .build()
        )
    }
}
