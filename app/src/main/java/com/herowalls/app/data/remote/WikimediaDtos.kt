package com.herowalls.app.data.remote

import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName

data class SearchResponse(
    val query: SearchQueryBlock? = null,
    @SerializedName("continue") val continuation: ContinueBlock? = null,
    val error: ApiErrorBlock? = null
)

data class SearchQueryBlock(
    val search: List<SearchHit>? = null,
    val searchinfo: SearchInfo? = null
)

data class SearchInfo(val totalhits: Int? = null)

data class SearchHit(
    val ns: Int? = null,
    val title: String? = null,
    val pageid: Long? = null,
    val size: Long? = null,
    val snippet: String? = null,
    val timestamp: String? = null
)

data class ContinueBlock(
    @SerializedName("sroffset") val srOffset: Int? = null,
    @SerializedName("continue") val continueToken: String? = null
)

data class ImageInfoResponse(
    val query: ImageInfoQueryBlock? = null,
    val error: ApiErrorBlock? = null
)

data class ImageInfoQueryBlock(val pages: List<PageDto>? = null)

data class PageDto(
    val pageid: Long? = null,
    val ns: Int? = null,
    val title: String? = null,
    val missing: Boolean? = null,
    val imageinfo: List<ImageInfoDto>? = null
)

data class ImageInfoDto(
    val timestamp: String? = null,
    val user: String? = null,
    val size: Long? = null,
    val width: Int? = null,
    val height: Int? = null,
    val url: String? = null,
    val descriptionurl: String? = null,
    val mime: String? = null,
    val thumburl: String? = null,
    val thumbwidth: Int? = null,
    val extmetadata: Map<String, ExtMeta>? = null
)

data class ExtMeta(val value: JsonElement? = null)

data class ApiErrorBlock(
    val code: String? = null,
    val info: String? = null
)

/** Commons returns mixed JSON types in extmetadata; read whatever is there as text. */
internal fun JsonElement?.asPlainText(): String {
    val el = this ?: return ""
    if (el.isJsonNull) return ""
    return when {
        el.isJsonPrimitive -> el.asJsonPrimitive.asString
        el.isJsonArray -> el.asJsonArray.joinToString(" ") { part -> part.asPlainText() }
        el.isJsonObject -> el.asJsonObject.get("value")?.asPlainText().orEmpty()
        else -> ""
    }.trim()
}
