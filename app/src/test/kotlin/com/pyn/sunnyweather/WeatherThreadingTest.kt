package com.pyn.sunnyweather

import com.pyn.sunnyweather.data.local.StoredWeather
import com.pyn.sunnyweather.data.local.WeatherStore
import com.pyn.sunnyweather.data.remote.WeatherApi
import com.pyn.sunnyweather.data.repository.DefaultWeatherRepository
import com.pyn.sunnyweather.model.City
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class WeatherThreadingTest {
    @Test fun `lazy construction network mapping and persistence run on repository worker`() = runTest {
        val worker = MarkedDispatcher(StandardTestDispatcher(testScheduler))
        val calls = mutableSetOf<String>()
        fun checkWorker(operation: String) {
            assertTrue("$operation must not execute in the caller context", worker.inside)
            calls.add(operation)
        }
        val repo = DefaultWeatherRepository(
            api = {
                checkWorker("create network")
                object : WeatherApi {
                    override suspend fun weather(token: String, location: String, language: String, unit: String, hours: Int, days: Int) =
                        response().also { checkWorker("network") }
                }
            },
            store = {
                checkWorker("create store")
                object : WeatherStore {
                    override suspend fun read() = StoredWeather().also { checkWorker("read") }
                    override suspend fun write(value: StoredWeather) { checkWorker("write") }
                }
            },
            token = "test-token",
            clock = { checkWorker("map response"); testNow },
            worker = worker,
        )
        assertTrue("Constructing a ViewModel must not initialize network or disk adapters", calls.isEmpty())
        assertNull(repo.refresh())
        repo.selectCity(City("110100", "北京市", "北京市", 116.4, 39.9))
        assertEquals(setOf("create network", "create store", "read", "write", "network", "map response"), calls)
    }
}

private class MarkedDispatcher(private val delegate: CoroutineDispatcher) : CoroutineDispatcher() {
    var inside = false
        private set
    override fun dispatch(context: CoroutineContext, block: Runnable) {
        delegate.dispatch(context) {
            inside = true
            try { block.run() } finally { inside = false }
        }
    }
}
