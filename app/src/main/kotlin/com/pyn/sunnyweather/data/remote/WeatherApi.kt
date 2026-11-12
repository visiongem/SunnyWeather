package com.pyn.sunnyweather.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

interface WeatherApi {
    @GET("v2.6/{token}/{location}/weather")
    suspend fun weather(
        @Path("token") token: String, @Path("location") location: String,
        @Query("lang") language: String = "zh_CN", @Query("unit") unit: String = "metric",
        @Query("hourlysteps") hours: Int = 24, @Query("dailysteps") days: Int = 3,
    ): WeatherResponseDto
}

object NetworkClient {
    fun moshi(): Moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    fun api(moshi: Moshi): WeatherApi = Retrofit.Builder()
        .baseUrl("https://api.caiyunapp.com/")
        .client(OkHttpClient.Builder().connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS).callTimeout(20, TimeUnit.SECONDS).build())
        .addConverterFactory(MoshiConverterFactory.create(moshi)).build().create(WeatherApi::class.java)
}

data class WeatherResponseDto(val status: String? = null, val timezone: String? = null, val result: WeatherResultDto? = null)
data class WeatherResultDto(val realtime: RealtimeDto? = null, val hourly: HourlyDto? = null, val daily: DailyDto? = null)
data class RealtimeDto(
    val status: String? = null, val temperature: Double? = null, val skycon: String? = null,
    @param:Json(name = "apparent_temperature") val apparent: Double? = null,
    val humidity: Double? = null, val wind: WindDto? = null,
)
data class WindDto(val speed: Double? = null)
data class HourlyDto(val status: String? = null, val temperature: List<HourTemperatureDto>? = null, val skycon: List<HourSkyDto>? = null)
data class HourTemperatureDto(val datetime: String? = null, val value: Double? = null)
data class HourSkyDto(val datetime: String? = null, val value: String? = null)
data class DailyDto(val status: String? = null, val temperature: List<DayTemperatureDto>? = null, val skycon: List<DaySkyDto>? = null)
data class DayTemperatureDto(val date: String? = null, val min: Double? = null, val max: Double? = null)
data class DaySkyDto(val date: String? = null, val value: String? = null)
