package com.pyn.sunnyweather

import androidx.datastore.core.CorruptionException
import com.pyn.sunnyweather.data.local.*
import com.pyn.sunnyweather.data.mapper.WeatherMapper
import com.pyn.sunnyweather.data.remote.*
import com.pyn.sunnyweather.model.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.time.OffsetDateTime

internal fun response(temp: Double = 25.0) = WeatherResponseDto("ok", "Asia/Shanghai", WeatherResultDto(RealtimeDto("ok", temp, "CLEAR_DAY")))
internal val testNow = OffsetDateTime.parse("2026-11-11T15:30+08:00").toInstant().toEpochMilli()
internal fun snapshot() = WeatherMapper.map(response(), City.Shenzhen, testNow)

class WeatherDataTest {
    private val moshi = NetworkClient.moshi()
    @Test fun `joins forecast by timestamps instead of array index and skips invalid rows`() {
        val json = """{"status":"ok","result":{"realtime":{"status":"ok","temperature":25,"humidity":5},
            "hourly":{"status":"ok","temperature":[{"datetime":"2026-11-11T16:00+08:00","value":22},{"datetime":"bad","value":9},{"datetime":"2026-11-11T15:00+08:00","value":24}],
            "skycon":[{"datetime":"2026-11-11T15:00+08:00","value":"CLOUDY"},{"datetime":"2026-11-11T16:00+08:00","value":"LIGHT_RAIN"}]},
            "daily":{"status":"ok","temperature":[{"date":"2026-11-12T00:00+08:00","min":18,"max":26},{"date":"2026-11-11T00:00+08:00","min":20,"max":28}],
            "skycon":[{"date":"2026-11-11T00:00+08:00","value":"CLEAR_DAY"}]}}}"""
        val mapped = WeatherMapper.map(moshi.adapter(WeatherResponseDto::class.java).fromJson(json)!!, City.Shenzhen, testNow)
        assertEquals(listOf(24.0,22.0), mapped.hours.map { it.temperature })
        assertEquals(listOf("CLOUDY","LIGHT_RAIN"), mapped.hours.map { it.condition })
        assertEquals(listOf("CLEAR_DAY","UNKNOWN"), mapped.days.map { it.condition })
        assertNull(mapped.current.humidity)
    }
    @Test fun `null or failed forecast sections preserve realtime`() {
        val dto = moshi.adapter(WeatherResponseDto::class.java).fromJson("""{"status":"ok","unused":1,"result":{"realtime":{"status":"ok","temperature":25},"hourly":{"status":"ok","temperature":null},"daily":{"status":"failed"}}}""")!!
        val mapped = WeatherMapper.map(dto, City.Shenzhen, testNow)
        assertEquals(25.0, mapped.current.temperature, 0.0)
        assertTrue(mapped.hours.isEmpty()); assertTrue(mapped.days.isEmpty())
        assertEquals("UNKNOWN", mapped.current.condition)
    }
    @Test fun `invalid realtime cannot become weather`() {
        for (dto in listOf(WeatherResponseDto("ok"),response(Double.NaN),response().copy(status="failed"))) {
            assertThrows(WeatherException::class.java) { WeatherMapper.map(dto,City.Shenzhen,testNow) }
        }
    }
    @Test fun `expired forecasts use city timezone and do not renew cached timestamp`() {
        val s = snapshot().copy(hours=listOf(HourForecast(testNow-3_600_000,10.0,"CLEAR_DAY"),HourForecast(testNow-1_800_000,20.0,"CLEAR_DAY")),
            days=listOf(DayForecast("2026-11-10",10.0,20.0,"CLEAR_DAY"),DayForecast("2026-11-11",10.0,20.0,"CLEAR_DAY")))
        assertEquals(1,s.upcomingHours(testNow).size); assertEquals(1,s.upcomingDays(testNow).size)
        assertTrue(s.isOld(testNow+86_400_001)); assertFalse(s.isOld(testNow))
        assertEquals(testNow,s.fetchedAt)
    }
    @Test fun `Moshi cache round trips and rejects truncated content`() = runTest {
        val serializer = WeatherSerializer(moshi)
        val envelope = CacheEnvelope(weather=WeatherCacheDto.from(snapshot()))
        val output=ByteArrayOutputStream(); serializer.writeTo(envelope,output)
        assertEquals(envelope,serializer.readFrom(output.toByteArray().inputStream()))
        for (bad in listOf("{", "{\"city\":null}", "not json")) {
            try { serializer.readFrom(bad.byteInputStream()); fail("Corruption must be reported") }
            catch (_: CorruptionException) { }
        }
        assertEquals(CacheEnvelope(),serializer.readFrom("{\"schemaVersion\":99}".byteInputStream()))
    }
    @Test fun `catalog removes duplicates and malformed coordinates and ranks exact city first`() = runTest {
        val catalog=CityCatalog { """440301	福田区	广东省深圳市福田区	114.0	22.0
440300	深圳市	广东省深圳市	114.057939	22.543527
440300	重复	重复地址	114	22
broken	错误	错误地址	999	22""" }
        val results=catalog.search(" 深圳 ")
        assertEquals(listOf("440300","440301"),results.map { it.id })
        assertTrue(catalog.search("无此城市").isEmpty()); assertTrue(catalog.search("").isEmpty())
    }
    @Test fun `bundled catalog has valid named locations including autonomous prefectures`() {
        val rows=java.io.File("src/main/assets/cities.tsv").readLines().map { it.split('\t') }
        assertEquals(3184,rows.size)
        assertTrue(rows.all { it.size == 7 && it[0].isNotBlank() && it[1].isNotBlank() && it[2].isNotBlank() })
        assertTrue(rows.all { row -> row.drop(5).all { it.matches(Regex("[a-z]+( [a-z]+)*")) } })
        val cities=CityCatalog.parse(rows.joinToString("\n") { it.joinToString("\t") })
        assertEquals(rows.size,cities.size)
        assertEquals("延边朝鲜族自治州",cities.single { it.id == "222400" }.name)
        assertEquals(City.Shenzhen,cities.single { it.id == "440300" })
    }

    @Test fun `bundled pronunciation index finds polyphonic cities and same name districts`() = runTest {
        val text = java.io.File("src/main/assets/cities.tsv").readText()
        val dispatcher = kotlinx.coroutines.test.StandardTestDispatcher(testScheduler)
        val catalog = CityCatalog(dispatcher, dispatcher) { text }
        val examples = mapOf(
            "shenzhen" to "440300", "chongqing" to "500100", "changsha" to "430100",
            "changchun" to "220100", "xiamen" to "350200", "luan" to "341500",
            "shanxian" to "371722", "yueqing" to "330382", "changzhi" to "140400",
            "zhangzi" to "140428", "changning" to "310105", "laoling" to "371481",
            "panyu" to "440113", "yuli" to "652823", "lvliang" to "141100",
        )
        for ((query, id) in examples) assertTrue(query, catalog.search(query).any { it.id == id })
        assertTrue(catalog.search("chaoyang").map { it.id }.containsAll(listOf("110105", "220104", "211300", "211321")))
        assertTrue(catalog.search("cq").any { it.id == "500100" })
        assertFalse(catalog.search("zhongqing").any { it.id == "500100" })
        // Parent addresses must use the same corrected pronunciation as city names.
        assertTrue(catalog.search("changzhi").any { it.id == "140403" })
        assertTrue(catalog.search("chongqing").any { it.id == "500105" })
    }

}
