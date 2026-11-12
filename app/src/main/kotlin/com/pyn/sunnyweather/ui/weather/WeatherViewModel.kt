package com.pyn.sunnyweather.ui.weather

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pyn.sunnyweather.data.repository.WeatherRepository
import com.pyn.sunnyweather.model.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class WeatherUiState(
    val session: WeatherSession = WeatherSession(),
    val refreshing: Boolean = true,
    val error: WeatherFailure? = null,
    val presentation: WeatherPresentation? = null,
)
class WeatherViewModel(
    private val repository: WeatherRepository,
    computation: CoroutineDispatcher = Dispatchers.Default,
    clock: () -> Long = System::currentTimeMillis,
) : ViewModel() {
    private data class RequestState(val refreshing: Boolean = true, val error: WeatherFailure? = null)
    private val requestState = MutableStateFlow(RequestState())
    private val presenter = WeatherPresenter()
    private val minuteTicks = flow {
        while (true) {
            val now = clock()
            emit(now)
            delay(60_000L - Math.floorMod(now, 60_000L))
        }
    }
    val state = combine(repository.session, requestState, minuteTicks) { session, request, now ->
        WeatherUiState(session, request.refreshing, request.error, presenter.present(session.snapshot, now))
    }.flowOn(computation).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WeatherUiState())
    private var requestJob: Job? = null
    private var requestId = 0L
    private var desiredCityId: String? = null
    init {
        request(null)
    }
    fun refresh() { if (requestJob?.isActive != true) request(null) }
    fun selectCity(city: City) {
        if (city.id != (desiredCityId ?: repository.session.value.city.id)) {
            desiredCityId = city.id
            request(city)
        }
    }
    private fun request(city: City?) {
        val id = ++requestId
        requestJob?.cancel()
        requestJob = viewModelScope.launch {
            requestState.value = RequestState()
            try {
                repository.initialize()
                if (city != null) repository.selectCity(city)
                val error = repository.refresh()
                if (id == requestId) requestState.update { it.copy(error = error) }
            } catch (e: CancellationException) { throw e
            } finally {
                if (id == requestId) requestState.update { it.copy(refreshing = false) }
            }
        }
    }
}
