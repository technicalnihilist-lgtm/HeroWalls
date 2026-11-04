package com.herowalls.app.data.model

import java.io.Serializable

/**
 * A single wallpaper backed by a Wikimedia Commons file.
 *
 * Every field here comes from the live Commons API (imageinfo + extmetadata).
 * Nothing is hard-coded or invented: if the API does not provide a value the
 * field stays empty and the UI shows an explicit "not stated" state.
 */
data class Wallpaper(
    val pageId: Long,
    val title: String,
    val displayName: String,
    val characterName: String,
    val categoryId: String,
    val thumbUrl: String,
    val previewUrl: String,
    val fullUrl: String,
    val width: Int,
    val height: Int,
    val mime: String,
    val sizeBytes: Long,
    val artist: String,
    val credit: String,
    val description: String,
    val licenseShort: String,
    val licenseCode: String,
    val licenseUrl: String,
    val restrictions: String,
    val modifications: String,
    val filePageUrl: String,
    val uploader: String,
    val dateTime: String,
    val attributionRequired: Boolean
) : Serializable {

    val isPortrait: Boolean get() = height >= width
    val aspectRatio: Float get() = if (height == 0) 1f else width.toFloat() / height.toFloat()
    val megapixels: Double get() = (width.toLong() * height.toLong()) / 1_000_000.0

    /** Commons requires the file page (not the raw upload URL) as the source link. */
    val sourceLink: String get() = filePageUrl

    val hasLicense: Boolean get() = licenseCode.isNotBlank() || licenseShort.isNotBlank()

    val resolutionLabel: String get() = "$width × $height"

    companion object {
        const val UNKNOWN = "Not stated on Commons"
    }
}

enum class SortOrder(val apiValue: String, val label: String) {
    RELEVANCE("relevance", "Best match"),
    NEWEST("create_timestamp_desc", "Newest first"),
    OLDEST("create_timestamp_asc", "Oldest first");

    companion object {
        fun fromLabel(label: String): SortOrder = entries.firstOrNull { it.label == label } ?: RELEVANCE
    }
}

/** Result of one page of a Commons search. */
data class FeedPage(
    val items: List<Wallpaper>,
    val nextOffset: Int?,
    val endReached: Boolean,
    val fromCache: Boolean,
    val totalScanned: Int
)
