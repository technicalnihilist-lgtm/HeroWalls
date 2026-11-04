package com.herowalls.app.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Text {

    private val tag = Regex("<[^>]*>")

    fun stripHtml(input: String?): String {
        if (input.isNullOrBlank()) return ""
        return decodeEntities(tag.replace(input, " ")).replace(Regex("\\s+"), " ").trim()
    }

    fun decodeEntities(input: String): String = input
        .replace("&nbsp;", " ")
        .replace("&amp;", "&")
        .replace("&quot;", "\"")
        .replace("&#039;", "'")
        .replace("&#39;", "'")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&ndash;", "–")
        .replace("&mdash;", "—")

    fun fileTitleToName(title: String): String = title
        .removePrefix("File:")
        .substringBeforeLast('.', missingDelimiterValue = title.removePrefix("File:"))
        .replace('_', ' ')
        .trim()

    fun humanBytes(bytes: Long): String = when {
        bytes <= 0L -> "—"
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> String.format(Locale.US, "%.0f KB", bytes / 1024.0)
        else -> String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0))
    }

    fun prettyDate(iso: String?): String {
        if (iso.isNullOrBlank()) return ""
        return runCatching {
            val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
            val date: Date = parser.parse(iso) ?: return iso
            SimpleDateFormat("d MMM yyyy", Locale.US).format(date)
        }.getOrDefault(iso)
    }

    fun relativeTime(epochMillis: Long): String {
        val diff = System.currentTimeMillis() - epochMillis
        val mins = diff / 60_000
        return when {
            mins < 1 -> "just now"
            mins < 60 -> "$mins min ago"
            mins < 1440 -> "${mins / 60} h ago"
            else -> "${mins / 1440} d ago"
        }
    }

    /** First meaningful token of a character name, used for the relevance guard. */
    fun relevanceToken(name: String): String {
        val cleaned = name.substringBefore('(').trim()
        return cleaned.lowercase(Locale.US)
    }
}
