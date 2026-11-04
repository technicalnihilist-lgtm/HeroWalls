package com.herowalls.app.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.herowalls.app.data.model.Wallpaper
import java.io.File

object ShareHelper {

    /**
     * Shares the offline file when it exists (full quality, no extra data use),
     * otherwise shares the Commons source link so attribution always travels with it.
     */
    fun share(context: Context, wallpaper: Wallpaper, localFile: File?) {
        val intent = if (localFile != null && localFile.exists()) {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                localFile
            )
            Intent(Intent.ACTION_SEND).apply {
                type = wallpaper.mime.ifBlank { "image/*" }
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                putExtra(Intent.EXTRA_TEXT, attributionText(wallpaper))
            }
        } else {
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, attributionText(wallpaper) + "\n" + wallpaper.filePageUrl)
            }
        }
        intent.putExtra(Intent.EXTRA_SUBJECT, "${wallpaper.displayName} - HeroWalls")
        context.startActivity(Intent.createChooser(intent, "Share wallpaper").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    fun attributionText(wallpaper: Wallpaper): String = buildString {
        append("\"${wallpaper.displayName}\"")
        append(" by ${wallpaper.artist}")
        append(" - ${wallpaper.licenseShort}")
        if (wallpaper.licenseUrl.isNotBlank()) append(" (${wallpaper.licenseUrl})")
        append("\nSource: ${wallpaper.filePageUrl}")
        append("\nShared via HeroWalls. Characters and trademarks belong to their respective owners.")
    }
}
