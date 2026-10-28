package com.example.data.remote

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

interface RadioBrowserApi {
    @GET("json/stations/search")
    suspend fun searchStations(
        @Query("countrycode") countryCode: String? = null,
        @Query("tag") tag: String? = null,
        @Query("name") name: String? = null,
        @Query("hidebroken") hideBroken: Boolean = true,
        @Query("is_https") isHttps: Boolean? = null,
        @Query("order") order: String = "clickcount",
        @Query("reverse") reverse: Boolean = true,
        @Query("limit") limit: Int = 75
    ): List<RadioStationDto>
}

class RadioBrowserClient {
    private val mirrors = listOf(
        "https://de1.api.radio-browser.info/",
        "https://fi1.api.radio-browser.info/",
        "https://at1.api.radio-browser.info/"
    )

    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header("User-Agent", "OndaFM-Android/1.0")
                .header("Accept", "application/json")
                .build()
            chain.proceed(request)
        }
        .build()

    private val apis: List<RadioBrowserApi> = mirrors.map { baseUrl ->
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(RadioBrowserApi::class.java)
    }

    suspend fun fetchBandStations(countryCode: String?): Result<List<RadioStationDto>> {
        var lastError: Throwable? = null
        for (api in apis) {
            val attempt = runCatching {
                if (countryCode == null) {
                    api.searchStations(
                        countryCode = null,
                        tag = "fm",
                        limit = 80
                    )
                } else {
                    val fmTagged = api.searchStations(
                        countryCode = countryCode,
                        tag = "fm",
                        limit = 50
                    )
                    if (fmTagged.size >= 30) {
                        fmTagged
                    } else {
                        val topNational = api.searchStations(
                            countryCode = countryCode,
                            tag = null,
                            limit = 65
                        )
                        (fmTagged + topNational).distinctBy { it.stationUuid }
                    }
                }
            }
            attempt.onSuccess { list ->
                if (list.isNotEmpty()) return Result.success(list)
            }.onFailure { err ->
                lastError = err
            }
        }
        return Result.failure(lastError ?: IllegalStateException("No radio mirrors reachable"))
    }
}
