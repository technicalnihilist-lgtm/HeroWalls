package com.herowalls.app.data.remote

import retrofit2.http.GET
import retrofit2.http.Query

/**
 * The official Wikimedia Commons (MediaWiki) action API.
 * Docs: https://commons.wikimedia.org/w/api.php
 */
interface WikimediaApi {

    @GET(
        "w/api.php?action=query&list=search&srnamespace=6" +
            "&srprop=timestamp%7Csize%7Csnippet&format=json&formatversion=2"
    )
    suspend fun searchFiles(
        @Query("srsearch") search: String,
        @Query("srlimit") limit: Int,
        @Query("sroffset") offset: Int,
        @Query("srsort") sort: String
    ): SearchResponse

    @GET(
        "w/api.php?action=query&prop=imageinfo" +
            "&iiprop=url%7Csize%7Cmime%7Cextmetadata%7Cuser%7Ctimestamp" +
            "&format=json&formatversion=2"
    )
    suspend fun imageInfo(
        @Query("titles") titles: String,
        @Query("iiurlwidth") thumbWidth: Int
    ): ImageInfoResponse

    @GET(
        "w/api.php?action=query&prop=imageinfo" +
            "&iiprop=url%7Csize%7Cmime%7Cextmetadata%7Cuser%7Ctimestamp" +
            "&format=json&formatversion=2"
    )
    suspend fun imageInfoByPageId(
        @Query("pageids") pageId: Long,
        @Query("iiurlwidth") thumbWidth: Int
    ): ImageInfoResponse
}
