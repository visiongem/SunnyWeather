package com.pyn.sunnyweather

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import com.pyn.sunnyweather.data.local.CityCatalog
import com.pyn.sunnyweather.data.repository.WeatherRepository
import com.pyn.sunnyweather.model.*
import com.pyn.sunnyweather.ui.city.CitySearchViewModel
import com.pyn.sunnyweather.ui.weather.WeatherViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ViewModelTest {
    @Test fun `clear query invalidates pending search and restores blank state`()=runTest {
        val dispatcher=StandardTestDispatcher(testScheduler); Dispatchers.setMain(dispatcher)
        val store=ViewModelStore()
        try {
            val saved=SavedStateHandle()
            val vm=CitySearchViewModel(CityCatalog(dispatcher,dispatcher) { "440300\t深圳市\t广东省深圳市\t114\t22" },saved)
            store.put("search",vm)
            vm.search("深圳"); vm.search(""); advanceUntilIdle()
            assertEquals("",vm.state.value.query); assertTrue(vm.state.value.cities.isEmpty()); assertFalse(vm.state.value.loading)
            assertEquals("",saved.get<String>("query"))
            vm.search("深圳"); advanceUntilIdle(); assertEquals("440300",vm.state.value.cities.single().id)
        } finally { store.clear(); Dispatchers.resetMain() }
    }
    @Test fun `rapid selection before queued coroutine runs still ends on last city`()=runTest {
        val dispatcher=StandardTestDispatcher(testScheduler); Dispatchers.setMain(dispatcher)
        val store=ViewModelStore()
        try {
            val repo=object:WeatherRepository {
                override val session=MutableStateFlow(WeatherSession(initialized=true))
                var refreshCount=0
                override suspend fun initialize() {}
                override suspend fun selectCity(city:City) { session.value=WeatherSession(city,initialized=true) }
                override suspend fun refresh():WeatherFailure? { refreshCount++; return null }
            }
            val vm=WeatherViewModel(repo, dispatcher); store.put("weather",vm)
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect() }
            runCurrent()
            val other=City("110100","北京市","北京市",116.4,39.9)
            vm.selectCity(other); vm.selectCity(City.Shenzhen); runCurrent()
            assertEquals(City.Shenzhen,vm.state.value.session.city); assertFalse(vm.state.value.refreshing)
            vm.refresh(); vm.refresh(); runCurrent(); assertEquals(3,repo.refreshCount)
        } finally { store.clear(); Dispatchers.resetMain() }
    }

    @Test fun `presentation expires on minute ticks and stops ticking without observers`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val store = ViewModelStore()
        try {
            val cached = snapshot().copy(hours = listOf(HourForecast(testNow - 3_600_000 + 30_000, 20.0, "CLOUDY")))
            val repo = object : WeatherRepository {
                override val session = MutableStateFlow(WeatherSession(snapshot = cached, fromCache = true, initialized = true))
                override suspend fun initialize() {}
                override suspend fun selectCity(city: City) {}
                override suspend fun refresh(): WeatherFailure? = null
            }
            var clockCalls = 0
            val vm = WeatherViewModel(repo, dispatcher) { clockCalls++; testNow + testScheduler.currentTime }
            store.put("weather", vm)
            val observer = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect() }
            runCurrent()
            val original = vm.state.value.presentation!!
            assertEquals(1, original.hours.size)
            advanceTimeBy(60_000); runCurrent()
            assertTrue(vm.state.value.presentation!!.hoursExpired)
            assertEquals(original.updatedAt, vm.state.value.presentation!!.updatedAt)
            observer.cancel()
            advanceTimeBy(5_001); runCurrent()
            val stoppedAt = clockCalls
            advanceTimeBy(120_000); runCurrent()
            assertEquals(stoppedAt, clockCalls)
        } finally { store.clear(); Dispatchers.resetMain() }
    }
}
