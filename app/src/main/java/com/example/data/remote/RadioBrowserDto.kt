package com.example.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class RadioStationDto(
    @Json(name = "stationuuid") val stationUuid: String = "",
    @Json(name = "name") val name: String = "",
    @Json(name = "url") val url: String = "",
    @Json(name = "url_resolved") val urlResolved: String = "",
    @Json(name = "favicon") val favicon: String = "",
    @Json(name = "tags") val tags: String = "",
    @Json(name = "country") val country: String = "",
    @Json(name = "countrycode") val countryCode: String = "",
    @Json(name = "state") val state: String = "",
    @Json(name = "codec") val codec: String = "",
    @Json(name = "bitrate") val bitrate: Int = 0,
    @Json(name = "hls") val hls: Int = 0,
    @Json(name = "votes") val votes: Int = 0,
    @Json(name = "clickcount") val clickCount: Int = 0
) {
    val playableUrl: String
        get() = urlResolved.trim().ifEmpty { url.trim() }
}
