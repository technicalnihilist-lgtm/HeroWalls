package com.herowalls.app.wallpaper

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

enum class WallpaperTarget(val label: String) {
    HOME("Home screen"),
    LOCK("Lock screen"),
    BOTH("Home and lock screen")
}

sealed class SetWallpaperResult {
    data object Success : SetWallpaperResult()
    data class Failure(val message: String) : SetWallpaperResult()
}

/**
 * Applies a downloaded file through the system WallpaperManager.
 * The bitmap is down-sampled to the device's suggested wallpaper size before
 * decoding finishes, which keeps memory use flat even for very large files.
 */
class WallpaperSetter(private val context: Context) {

    suspend fun set(file: File, target: WallpaperTarget): SetWallpaperResult =
        withContext(Dispatchers.IO) {
            if (!file.exists() || file.length() == 0L) {
                return@withContext SetWallpaperResult.Failure("The wallpaper file is missing. Download it again.")
            }
            val manager = WallpaperManager.getInstance(context)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && !manager.isWallpaperSupported) {
                return@withContext SetWallpaperResult.Failure("This device does not support changing the wallpaper.")
            }

            val bitmap = try {
                decodeScaled(file, manager)
            } catch (e: OutOfMemoryError) {
                return@withContext SetWallpaperResult.Failure("Not enough memory to apply this image.")
            } catch (e: Exception) {
                return@withContext SetWallpaperResult.Failure("This image could not be decoded (${e.message}).")
            }

            try {
                when (target) {
                    WallpaperTarget.HOME -> manager.setBitmap(bitmap)
                    WallpaperTarget.LOCK -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        manager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_LOCK)
                    } else {
                        manager.setBitmap(bitmap)
                    }
                    WallpaperTarget.BOTH -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        manager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK)
                    } else {
                        manager.setBitmap(bitmap)
                    }
                }
                SetWallpaperResult.Success
            } catch (e: Exception) {
                SetWallpaperResult.Failure("Android refused the wallpaper change: ${e.message ?: "unknown error"}")
            } finally {
                if (!bitmap.isRecycled) bitmap.recycle()
            }
        }

    private fun decodeScaled(file: File, manager: WallpaperManager): Bitmap {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            throw IllegalStateException("unsupported image format")
        }

        val targetWidth = maxOf(manager.desiredMinimumWidth, 1080).coerceAtLeast(1)
        val targetHeight = maxOf(manager.desiredMinimumHeight, 1920).coerceAtLeast(1)

        var sample = 1
        while (bounds.outWidth / (sample * 2) >= targetWidth &&
            bounds.outHeight / (sample * 2) >= targetHeight
        ) {
            sample *= 2
        }

        val options = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        return BitmapFactory.decodeFile(file.absolutePath, options)
            ?: throw IllegalStateException("decoder returned no pixels")
    }
}
