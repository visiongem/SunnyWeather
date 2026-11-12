package com.pyn.sunnyweather

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.core.view.WindowCompat
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.pyn.sunnyweather.ui.city.CitySearchScreen
import com.pyn.sunnyweather.ui.city.CitySearchViewModel
import com.pyn.sunnyweather.ui.theme.SunnyWeatherTheme
import com.pyn.sunnyweather.ui.weather.WeatherScreen
import com.pyn.sunnyweather.ui.weather.WeatherViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as SunnyWeatherApplication
        setContent {
            SunnyWeatherTheme {
                val weatherFactory = remember(app) { viewModelFactory {
                    initializer { WeatherViewModel(app.weatherRepository) }
                } }
                val searchFactory = remember(app) { viewModelFactory {
                    initializer { CitySearchViewModel(app.cityCatalog, createSavedStateHandle()) }
                } }
                val weather: WeatherViewModel = viewModel(factory = weatherFactory)
                val search: CitySearchViewModel = viewModel(factory = searchFactory)
                val weatherState by weather.state.collectAsStateWithLifecycle()
                val searchState by search.state.collectAsStateWithLifecycle()
                val weatherScroll = rememberLazyListState()
                var searching by rememberSaveable { mutableStateOf(false) }
                val closeSearch = { searching = false }
                val dark = isSystemInDarkTheme()
                SideEffect {
                    WindowCompat.getInsetsController(window, window.decorView).apply {
                        isAppearanceLightStatusBars = searching && !dark
                        isAppearanceLightNavigationBars = searching && !dark
                    }
                }
                BackHandler(searching, closeSearch)
                if (searching) {
                    CitySearchScreen(searchState, weatherState.session.city, search::search,
                        onBack = closeSearch, onSelect = { city ->
                            weather.selectCity(city)
                            closeSearch()
                        }, snapshot = weatherState.session.snapshot)
                } else {
                    WeatherScreen(weatherState, onSearch = { search.search(""); searching = true }, onRefresh = weather::refresh, listState = weatherScroll)
                }
            }
        }
    }
}
