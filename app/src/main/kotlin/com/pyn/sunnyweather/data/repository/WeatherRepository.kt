package com.pyn.sunnyweather.data.repository

import com.pyn.sunnyweather.data.local.*
import com.pyn.sunnyweather.data.mapper.WeatherMapper
import com.pyn.sunnyweather.data.remote.WeatherApi
import com.pyn.sunnyweather.model.*
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonEncodingException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException

interface WeatherRepository {
    val session: StateFlow<WeatherSession>
    suspend fun initialize()
    suspend fun selectCity(city: City)
    suspend fun refresh(): WeatherFailure?
}

class DefaultWeatherRepository(
    api: () -> WeatherApi,
    store: () -> WeatherStore,
    private val token: String,
    private val clock: () -> Long = System::currentTimeMillis,
    private val worker: CoroutineDispatcher = Dispatchers.Default,
) : WeatherRepository {
    private val api by lazy(api)
    private val store by lazy(store)
    private val mutableSession = MutableStateFlow(WeatherSession())
    override val session = mutableSession.asStateFlow()
    private val mutex = Mutex()
    private var generation = 0L
    private var lastSnapshot: WeatherSnapshot? = null

    override suspend fun initialize(): Unit = withContext(worker) { mutex.withLock {
        if (!session.value.initialized) {
            try {
                val stored = store.read()
                lastSnapshot = stored.weather
                mutableSession.value = WeatherSession(stored.city,
                    stored.weather?.takeIf { it.city.id == stored.city.id }, fromCache = true, initialized = true)
            } catch (e: CancellationException) { throw e
            } catch (_: IOException) {
                mutableSession.value = WeatherSession(initialized = true, storageFailed = true)
            }
        }
    } }

    override suspend fun selectCity(city: City): Unit = withContext(worker) {
        initialize()
        mutex.withLock {
            if (session.value.city.id == city.id) return@withLock
            generation++
            mutableSession.value = WeatherSession(city,
                lastSnapshot?.takeIf { it.city.id == city.id }, fromCache = true, initialized = true)
            saveLocked()
        }
    }

    override suspend fun refresh(): WeatherFailure? = withContext(worker) {
        initialize()
        val (request, city) = mutex.withLock { ++generation to session.value.city }
        try {
            if (token.isBlank()) throw WeatherException(WeatherFailure.CONFIGURATION)
            val response = api.weather(token, "${city.longitude},${city.latitude}")
            val snapshot = WeatherMapper.map(response, city, clock())
            mutex.withLock {
                // Guard both publication and persistence, including A -> B -> A.
                if (request == generation && session.value.city.id == city.id) {
                    lastSnapshot = snapshot
                    mutableSession.value = WeatherSession(city, snapshot, initialized = true)
                    saveLocked()
                }
            }
            null
        } catch (e: CancellationException) { throw e
        } catch (e: Exception) {
            mutex.withLock { if (request == generation) classify(e) else null }
        }
    }

    // Called while holding the same lock used by selection and generation changes.
    private suspend fun saveLocked() {
        try {
            store.write(StoredWeather(session.value.city, lastSnapshot))
            mutableSession.value = session.value.copy(storageFailed = false)
        } catch (e: CancellationException) { throw e
        } catch (_: IOException) { mutableSession.value = session.value.copy(storageFailed = true) }
    }

    private fun classify(e: Exception): WeatherFailure = when (e) {
        is WeatherException -> e.failure
        is SocketTimeoutException -> WeatherFailure.TIMEOUT
        is HttpException -> when (e.code()) {
            401, 403 -> WeatherFailure.AUTH
            429 -> WeatherFailure.RATE_LIMIT
            else -> WeatherFailure.SERVER
        }
        is JsonDataException, is JsonEncodingException -> WeatherFailure.INVALID_DATA
        is IOException -> WeatherFailure.NETWORK
        else -> WeatherFailure.INVALID_DATA
    }
}
