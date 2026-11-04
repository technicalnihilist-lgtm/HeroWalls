package com.herowalls.app.data.download

import android.content.Context
import com.herowalls.app.data.local.LibraryRepository
import com.herowalls.app.data.model.Wallpaper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException

/**
 * Streams the original-resolution Commons file to app-private storage.
 * Files are written to a temp name first so a half-finished download is never
 * mistaken for a complete wallpaper.
 */
class WallpaperDownloader(
    private val context: Context,
    private val client: OkHttpClient,
    private val library: LibraryRepository
) {

    fun wallpaperDir(): File = File(context.filesDir, "wallpapers").apply { if (!exists()) mkdirs() }

    fun fileFor(wallpaper: Wallpaper): File {
        val ext = when (wallpaper.mime) {
            "image/png" -> "png"
            "image/webp" -> "webp"
            else -> "jpg"
        }
        return File(wallpaperDir(), "${wallpaper.pageId}.$ext")
    }

    /** True when an offline copy already exists and is registered in the database. */
    suspend fun isDownloaded(wallpaper: Wallpaper): Boolean {
        val file = fileFor(wallpaper)
        return file.exists() && file.length() > 0L && library.findDownload(wallpaper.pageId) != null
    }

    suspend fun download(
        wallpaper: Wallpaper,
        onProgress: (Float) -> Unit = {}
    ): Result<File> = withContext(Dispatchers.IO) {
        val target = fileFor(wallpaper)
        if (target.exists() && target.length() > 0L && library.findDownload(wallpaper.pageId) != null) {
            onProgress(1f)
            return@withContext Result.success(target)
        }

        val temp = File(target.parentFile, "${target.name}.part")
        try {
            val request = Request.Builder()
                .url(wallpaper.fullUrl)
                .header("User-Agent", USER_AGENT)
                .header("Accept", "image/webp,image/jpeg,image/png,*/*")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("Download failed with HTTP ${response.code}")
                }
                val body = response.body ?: throw IOException("Empty response body")
                val total = body.contentLength()
                body.byteStream().use { input ->
                    temp.outputStream().use { output ->
                        val buffer = ByteArray(64 * 1024)
                        var written = 0L
                        while (true) {
                            val read = input.read(buffer)
                            if (read == -1) break
                            output.write(buffer, 0, read)
                            written += read
                            if (total > 0) onProgress((written.toFloat() / total).coerceIn(0f, 1f))
                        }
                        output.flush()
                    }
                }
            }

            if (temp.length() <= 0L) throw IOException("Downloaded file is empty")
            if (target.exists()) target.delete()
            if (!temp.renameTo(target)) {
                temp.copyTo(target, overwrite = true)
                temp.delete()
            }
            library.recordDownload(wallpaper, target.absolutePath, target.length())
            onProgress(1f)
            Result.success(target)
        } catch (e: Exception) {
            runCatching { temp.delete() }
            Result.failure(e)
        }
    }

    suspend fun delete(wallpaper: Wallpaper): Boolean = withContext(Dispatchers.IO) {
        val file = fileFor(wallpaper)
        val removed = if (file.exists()) file.delete() else true
        library.removeDownload(wallpaper.pageId)
        removed
    }

    companion object {
        const val USER_AGENT =
            "HeroWalls/1.0 (Android wallpaper app; https://commons.wikimedia.org/)"
    }
}
