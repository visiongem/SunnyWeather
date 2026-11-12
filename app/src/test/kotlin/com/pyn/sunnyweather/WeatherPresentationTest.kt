package com.pyn.sunnyweather

import com.pyn.sunnyweather.model.*
import com.pyn.sunnyweather.ui.weather.WeatherPresenter
import java.time.OffsetDateTime
import org.junit.Assert.*
import org.junit.Test

class WeatherPresentationTest {
    @Test fun `cross midnight labels keep time only and use the forecast timezone`() {
        val now = epoch("2026-11-11T23:30+08:00")
        val source = snapshot().copy(
            hours = listOf(HourForecast(epoch("2026-11-11T23:00+08:00"), 20.6, "CLOUDY"),
                HourForecast(epoch("2026-11-12T00:00+08:00"), 18.2, "CLOUDY")),
            days = listOf(DayForecast("2026-11-11", 10.0, 20.0, "CLOUDY"), DayForecast("2026-11-12", 20.0, 30.0, "CLOUDY")),
        )
        val display = WeatherPresenter().present(source, now)!!
        assertEquals(listOf("23:00", "00:00"), display.hours.map { it.label })
        assertEquals(listOf(21, 18), display.hours.map { it.temperature })
        assertNull(display.days.first().label)
        assertEquals("11/12", display.days.last().label)
        assertEquals(display.days.first(), display.today)
        assertEquals(0f, display.days.first().rangeStart, 0f)
        assertEquals(.5f, display.days.first().rangeEnd, 0f)
        assertEquals(1f, display.days.last().rangeEnd, 0f)
    }

    @Test fun `missing forecasts differ from expired cached forecasts and invalid dates are skipped`() {
        val presenter = WeatherPresenter()
        val absent = presenter.present(snapshot(), testNow)!!
        assertFalse(absent.hoursExpired); assertFalse(absent.daysExpired)
        val old = snapshot().copy(
            hours = listOf(HourForecast(testNow - 3_600_000, 20.0, "CLOUDY")),
            days = listOf(DayForecast("2026-11-10", 10.0, 20.0, "CLOUDY"), DayForecast("bad", 10.0, 20.0, "CLOUDY")),
        )
        val expired = presenter.present(old, testNow)!!
        assertTrue(expired.hoursExpired); assertTrue(expired.daysExpired)
        assertTrue(expired.hours.isEmpty()); assertTrue(expired.days.isEmpty())
        assertNull(expired.today)
        assertEquals(absent.updatedAt, expired.updatedAt)
    }

    @Test fun `refresh status can reuse presentation but new snapshot minute and city cannot`() {
        val presenter = WeatherPresenter()
        val source = snapshot()
        val first = presenter.present(source, testNow)!!
        assertSame(first, presenter.present(source, testNow + 1_000))
        assertNotSame(first, presenter.present(source, testNow + 60_000))
        val newSource = source.copy(current = source.current.copy(temperature = 30.0))
        assertEquals(30.0, presenter.present(newSource, testNow)!!.current.temperature, 0.0)
        assertNull(presenter.present(null, testNow))
        assertNotNull(presenter.present(source, testNow))
    }

    @Test fun `invalid timezone falls back and flat temperature ranges stay finite`() {
        val source = snapshot().copy(timezone = "invalid-zone", days = listOf(DayForecast("2026-11-11", 20.0, 20.0, "CLOUDY")))
        val display = WeatherPresenter().present(source, testNow)!!
        assertEquals("11月11日 15:30", display.updatedAt)
        assertEquals(0f, display.days.single().rangeStart, 0f)
        assertEquals(0f, display.days.single().rangeEnd, 0f)
        assertTrue(WeatherPresenter().present(source, testNow + 86_400_001)!!.isOld)
    }

    private fun epoch(text: String) = OffsetDateTime.parse(text).toInstant().toEpochMilli()
}
