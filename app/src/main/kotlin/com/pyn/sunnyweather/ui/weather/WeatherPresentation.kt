package com.pyn.sunnyweather.ui.weather

import com.pyn.sunnyweather.model.CurrentWeather
import com.pyn.sunnyweather.model.WeatherSnapshot
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

data class HourPresentation(val time: Long, val label: String, val temperature: Int, val condition: String)
data class DayPresentation(
    val date: String, val label: String?, val min: Int, val max: Int, val condition: String,
    val rangeStart: Float, val rangeEnd: Float,
)
data class WeatherPresentation(
    val current: CurrentWeather,
    val updatedAt: String,
    val isOld: Boolean,
    val today: DayPresentation?,
    val hours: List<HourPresentation>,
    val days: List<DayPresentation>,
    val hoursExpired: Boolean,
    val daysExpired: Boolean,
)

/** One presenter per ViewModel; called sequentially on its computation dispatcher. */
class WeatherPresenter {
    private var previousSnapshot: WeatherSnapshot? = null
    private var previousMinute: Long? = null
    private var previous: WeatherPresentation? = null

    fun present(snapshot: WeatherSnapshot?, now: Long): WeatherPresentation? {
        val minute = Math.floorDiv(now, 60_000L)
        if (snapshot === previousSnapshot && minute == previousMinute) return previous
        val result = snapshot?.let { build(it, now) }
        previousSnapshot = snapshot
        previousMinute = minute
        previous = result
        return result
    }

    private fun build(snapshot: WeatherSnapshot, now: Long): WeatherPresentation {
        val zone = snapshot.zone()
        val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        val dated = snapshot.upcomingDays(now).map { it to LocalDate.parse(it.date) }
        val rangeMin = dated.minOfOrNull { it.first.min } ?: 0.0
        val rangeMax = dated.maxOfOrNull { it.first.max } ?: 1.0
        val span = (rangeMax - rangeMin).coerceAtLeast(1.0)
        val days = dated.map { (day, date) ->
            DayPresentation(day.date, if (date == today) null else date.format(dayFormat),
                day.min.roundToInt(), day.max.roundToInt(), day.condition,
                ((day.min - rangeMin) / span).toFloat().coerceIn(0f, 1f),
                ((day.max - rangeMin) / span).toFloat().coerceIn(0f, 1f))
        }
        val hours = snapshot.upcomingHours(now).map {
            HourPresentation(it.time, Instant.ofEpochMilli(it.time).atZone(zone).format(hourFormat), it.temperature.roundToInt(), it.condition)
        }
        return WeatherPresentation(snapshot.current,
            Instant.ofEpochMilli(snapshot.fetchedAt).atZone(zone).format(updatedFormat), snapshot.isOld(now),
            days.firstOrNull { it.label == null }, hours, days,
            hours.isEmpty() && snapshot.hours.isNotEmpty(), days.isEmpty() && snapshot.days.isNotEmpty())
    }

    companion object {
        private val hourFormat = DateTimeFormatter.ofPattern("HH:mm", Locale.CHINA)
        private val dayFormat = DateTimeFormatter.ofPattern("M/d", Locale.CHINA)
        private val updatedFormat = DateTimeFormatter.ofPattern("M月d日 HH:mm", Locale.CHINA)
    }
}
