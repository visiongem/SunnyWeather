package com.pyn.sunnyweather

import com.pyn.sunnyweather.data.local.*
import com.pyn.sunnyweather.data.remote.*
import com.pyn.sunnyweather.data.repository.*
import com.pyn.sunnyweather.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException
import java.net.SocketTimeoutException
import retrofit2.HttpException
import retrofit2.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import com.squareup.moshi.JsonDataException

private class MemoryStore(var value: StoredWeather = StoredWeather(), var failWrite: Boolean=false): WeatherStore {
    override suspend fun read()=value
    override suspend fun write(value: StoredWeather) { if(failWrite) throw IOException(); this.value=value }
}
private class FakeApi(val get: suspend () -> WeatherResponseDto): WeatherApi {
    override suspend fun weather(token: String, location: String, language: String, unit: String, hours: Int, days: Int)=get()
}
@OptIn(ExperimentalCoroutinesApi::class)
class WeatherRepositoryTest {
    private fun TestScope.repository(api: WeatherApi, store: WeatherStore, token: String) =
        DefaultWeatherRepository({ api }, { store }, token, worker = StandardTestDispatcher(testScheduler))

    private val beijing=City("110100","北京市","北京市",116.4,39.9)
    @Test fun `cold start uses matching cache and network failure preserves timestamp`()=runTest {
        val cached=snapshot(); val store=MemoryStore(StoredWeather(City.Shenzhen,cached))
        val repo=repository(FakeApi { throw IOException() },store,"test-token")
        repo.initialize(); assertTrue(repo.session.value.fromCache)
        assertEquals(WeatherFailure.NETWORK,repo.refresh())
        assertEquals(cached,repo.session.value.snapshot); assertEquals(cached,store.value.weather)
        repo.selectCity(beijing); assertNull(repo.session.value.snapshot)
        assertEquals(beijing,store.value.city)
    }
    @Test fun `A B A responses only publish and save newest generation`()=runTest {
        val pending=mutableListOf<CompletableDeferred<WeatherResponseDto>>()
        val api=FakeApi { CompletableDeferred<WeatherResponseDto>().also { pending.add(it) }.await() }
        val store=MemoryStore(); val repo=repository(api,store,"test-token")
        val a1=launch { repo.refresh() }; runCurrent()
        repo.selectCity(beijing)
        val b=launch { repo.refresh() }; runCurrent()
        repo.selectCity(City.Shenzhen)
        val a2=launch { repo.refresh() }; runCurrent()
        pending[2].complete(response(30.0)); a2.join()
        pending[1].complete(response(20.0)); b.join()
        pending[0].complete(response(10.0)); a1.join()
        assertEquals(30.0,repo.session.value.snapshot!!.current.temperature,0.0)
        assertEquals(repo.session.value.snapshot,store.value.weather)
        assertEquals(City.Shenzhen,store.value.city)
    }
    @Test fun `storage failure does not discard successful online weather`()=runTest {
        val repo=repository(FakeApi { response() },MemoryStore(failWrite=true),"test-token")
        assertNull(repo.refresh()); assertNotNull(repo.session.value.snapshot); assertTrue(repo.session.value.storageFailed)
    }
    @Test fun `request cancellation propagates without writing cache`()=runTest {
        val store=MemoryStore()
        val repo=repository(FakeApi { throw CancellationException() },store,"test-token")
        try { repo.refresh(); fail("Cancellation swallowed") } catch (_:CancellationException) { }
        assertNull(store.value.weather)
    }
    @Test fun `missing token and timeout are distinct errors`()=runTest {
        val api=FakeApi { throw SocketTimeoutException() }
        assertEquals(WeatherFailure.CONFIGURATION,repository(api,MemoryStore(),"").refresh())
        assertEquals(WeatherFailure.TIMEOUT,repository(api,MemoryStore(),"test-token").refresh())
    }
    @Test fun `HTTP and decode errors are classified without leaking server messages`()=runTest {
        for ((code,expected) in listOf(401 to WeatherFailure.AUTH,403 to WeatherFailure.AUTH,429 to WeatherFailure.RATE_LIMIT,500 to WeatherFailure.SERVER)) {
            val repo=repository(FakeApi { throw HttpException(Response.error<WeatherResponseDto>(code,"private response".toResponseBody())) },MemoryStore(),"test-token")
            assertEquals(expected,repo.refresh())
        }
        val decode=repository(FakeApi { throw JsonDataException("invalid field") },MemoryStore(),"test-token")
        assertEquals(WeatherFailure.INVALID_DATA,decode.refresh())
        val business=repository(FakeApi { response().copy(status="failed") },MemoryStore(),"test-token")
        assertEquals(WeatherFailure.INVALID_DATA,business.refresh())
    }

}
