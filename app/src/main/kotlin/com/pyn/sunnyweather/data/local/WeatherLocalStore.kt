package com.pyn.sunnyweather.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.core.CorruptionException
import com.pyn.sunnyweather.model.*
import com.squareup.moshi.Moshi
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonEncodingException
import kotlinx.coroutines.flow.first
import java.io.InputStream
import java.io.EOFException
import java.io.OutputStream

// The versioned storage envelope is independent of the remote API schema.
data class WeatherCacheDto(
    val city: City, val fetchedAt: Long, val timezone: String,
    val current: CurrentWeather, val hours: List<HourForecast>, val days: List<DayForecast>,
) {
    fun model() = WeatherSnapshot(city, fetchedAt, timezone, current, hours, days)
    companion object {
        fun from(s: WeatherSnapshot) = WeatherCacheDto(s.city, s.fetchedAt, s.timezone, s.current, s.hours, s.days)
    }
}
data class CacheEnvelope(val schemaVersion: Int = 1, val city: City = City.Shenzhen, val weather: WeatherCacheDto? = null)
data class StoredWeather(val city: City = City.Shenzhen, val weather: WeatherSnapshot? = null)

class WeatherSerializer(moshi: Moshi) : Serializer<CacheEnvelope> {
    private val adapter = moshi.adapter(CacheEnvelope::class.java)
    override val defaultValue = CacheEnvelope()
    override suspend fun readFrom(input: InputStream): CacheEnvelope {
        try {
            val value = adapter.fromJson(input.readBytes().toString(Charsets.UTF_8))
                ?: throw CorruptionException("Empty cache")
            if (value.schemaVersion != 1) return defaultValue
            if (!value.city.latitude.isFinite() || !value.city.longitude.isFinite() ||
                value.weather?.current?.temperature?.isFinite() == false) throw CorruptionException("Invalid cache")
            return value
        } catch (e: JsonDataException) { throw CorruptionException("Invalid cache", e)
        } catch (e: JsonEncodingException) { throw CorruptionException("Invalid cache", e)
        } catch (e: EOFException) { throw CorruptionException("Truncated cache", e) }
    }
    override suspend fun writeTo(t: CacheEnvelope, output: OutputStream) {
        output.write(adapter.toJson(t).toByteArray(Charsets.UTF_8))
    }
}
interface WeatherStore {
    suspend fun read(): StoredWeather
    suspend fun write(value: StoredWeather)
}
class WeatherLocalStore(private val store: DataStore<CacheEnvelope>) : WeatherStore {
    override suspend fun read() = store.data.first().let { StoredWeather(it.city, it.weather?.model()) }
    override suspend fun write(value: StoredWeather) {
        store.updateData { CacheEnvelope(city = value.city, weather = value.weather?.let(WeatherCacheDto::from)) }
    }
}
