package com.pyn.sunnyweather.ui.city

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pyn.sunnyweather.data.local.CityCatalog
import com.pyn.sunnyweather.model.City
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CitySearchState(val query: String = "", val cities: List<City> = emptyList(), val loading: Boolean = false, val failed: Boolean = false)
class CitySearchViewModel(private val catalog: CityCatalog, private val savedState: SavedStateHandle) : ViewModel() {
    private val mutableState = MutableStateFlow(CitySearchState())
    val state = mutableState.asStateFlow()
    private var searchJob: Job? = null
    private var generation = 0L
    init { search(savedState["query"] ?: "") }
    fun search(query: String) {
        savedState["query"] = query
        val id = ++generation
        searchJob?.cancel()
        mutableState.value = CitySearchState(query, loading = query.isNotBlank())
        if (query.isBlank()) return
        searchJob = viewModelScope.launch {
            try {
                val results = catalog.search(query)
                if (id == generation) mutableState.value = CitySearchState(query, results)
            } catch (e: CancellationException) { throw e
            } catch (_: Exception) {
                if (id == generation) mutableState.value = CitySearchState(query, failed = true)
            }
        }
    }
}
