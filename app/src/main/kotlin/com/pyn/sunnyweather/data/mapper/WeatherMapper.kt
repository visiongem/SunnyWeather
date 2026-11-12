package com.pyn.sunnyweather.data.mapper

import com.pyn.sunnyweather.data.remote.WeatherResponseDto
import com.pyn.sunnyweather.model.*
import java.time.OffsetDateTime

object WeatherMapper {
    private fun timestamp(value: String?) = runCatching { OffsetDateTime.parse(value).toInstant().toEpochMilli() }.getOrNull()
    private fun date(value: String?) = runCatching { OffsetDateTime.parse(value).toLocalDate().toString() }.getOrNull()

    fun map(response: WeatherResponseDto, city: City, now: Long): WeatherSnapshot {
        val result = response.result ?: throw WeatherException(WeatherFailure.INVALID_DATA)
        val realtime = result.realtime
        val temperature = realtime?.temperature
        if (response.status != "ok" || realtime?.status != "ok" || temperature == null || !temperature.isFinite()) {
            throw WeatherException(WeatherFailure.INVALID_DATA)
        }
        val hourly = result.hourly?.takeIf { it.status == "ok" }
        val hourSky = hourly?.skycon.orEmpty().mapNotNull { sky -> timestamp(sky.datetime)?.let { it to (sky.value ?: "UNKNOWN") } }.toMap()
        val hours = hourly?.temperature.orEmpty().mapNotNull {
            val time = timestamp(it.datetime)
            val value = it.value?.takeIf(Double::isFinite)
            if (time == null || value == null) null else HourForecast(time, value, hourSky[time] ?: "UNKNOWN")
        }.distinctBy { it.time }.sortedBy { it.time }
        val daily = result.daily?.takeIf { it.status == "ok" }
        val daySky = daily?.skycon.orEmpty().mapNotNull { sky -> date(sky.date)?.let { it to (sky.value ?: "UNKNOWN") } }.toMap()
        val days = daily?.temperature.orEmpty().mapNotNull {
            val day = date(it.date)
            val min = it.min?.takeIf(Double::isFinite)
            val max = it.max?.takeIf(Double::isFinite)
            if (day == null || min == null || max == null || min > max) null else DayForecast(day, min, max, daySky[day] ?: "UNKNOWN")
        }.distinctBy { it.date }.sortedBy { it.date }
        return WeatherSnapshot(city, now, response.timezone ?: "Asia/Shanghai", CurrentWeather(
            temperature, realtime.skycon ?: "UNKNOWN", realtime.apparent?.takeIf(Double::isFinite),
            realtime.humidity?.takeIf { it.isFinite() && it in 0.0..1.0 },
            realtime.wind?.speed?.takeIf { it.isFinite() && it >= 0 },
        ), hours, days)
    }
}
