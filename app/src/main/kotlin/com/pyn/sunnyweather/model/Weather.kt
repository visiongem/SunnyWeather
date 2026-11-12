package com.pyn.sunnyweather.model

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class City(val id: String, val name: String, val address: String, val longitude: Double, val latitude: Double) {
    companion object {
        val Shenzhen = City("440300", "深圳市", "广东省深圳市", 114.057939, 22.543527)
    }
}

data class CurrentWeather(val temperature: Double, val condition: String, val apparent: Double?, val humidity: Double?, val windKmh: Double?)
data class HourForecast(val time: Long, val temperature: Double, val condition: String)
data class DayForecast(val date: String, val min: Double, val max: Double, val condition: String)
data class WeatherSnapshot(
    val city: City, val fetchedAt: Long, val timezone: String,
    val current: CurrentWeather, val hours: List<HourForecast>, val days: List<DayForecast>,
) {
    fun zone(): ZoneId = runCatching { ZoneId.of(timezone) }.getOrDefault(ZoneId.of("Asia/Shanghai"))
    fun isOld(now: Long): Boolean = now - fetchedAt > 24 * 60 * 60 * 1000L
    fun upcomingHours(now: Long) = hours.filter { it.time + 3_600_000 > now }
    fun upcomingDays(now: Long): List<DayForecast> {
        val today = Instant.ofEpochMilli(now).atZone(zone()).toLocalDate()
        return days.filter { runCatching { !LocalDate.parse(it.date).isBefore(today) }.getOrDefault(false) }
    }
}

enum class WeatherFailure { NETWORK, TIMEOUT, AUTH, RATE_LIMIT, SERVER, INVALID_DATA, CONFIGURATION }
class WeatherException(val failure: WeatherFailure) : Exception(failure.name)
data class WeatherSession(
    val city: City = City.Shenzhen,
    val snapshot: WeatherSnapshot? = null,
    val fromCache: Boolean = false,
    val storageFailed: Boolean = false,
    val initialized: Boolean = false,
)
