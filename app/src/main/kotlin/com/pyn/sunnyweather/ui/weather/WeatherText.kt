package com.pyn.sunnyweather.ui.weather

import com.pyn.sunnyweather.R
import com.pyn.sunnyweather.model.WeatherFailure

fun conditionText(code: String): Int = when (code) {
    "CLEAR_DAY", "CLEAR_NIGHT" -> R.string.weather_clear
    "PARTLY_CLOUDY_DAY", "PARTLY_CLOUDY_NIGHT" -> R.string.weather_partly
    "CLOUDY" -> R.string.weather_cloudy
    "WIND" -> R.string.weather_wind
    "LIGHT_RAIN" -> R.string.weather_light_rain
    "MODERATE_RAIN" -> R.string.weather_rain
    "HEAVY_RAIN", "STORM_RAIN" -> R.string.weather_heavy_rain
    "THUNDER_SHOWER" -> R.string.weather_thunder
    "SLEET" -> R.string.weather_sleet
    "LIGHT_SNOW", "MODERATE_SNOW", "HEAVY_SNOW", "STORM_SNOW" -> R.string.weather_snow
    "HAIL" -> R.string.weather_hail
    "LIGHT_HAZE", "MODERATE_HAZE", "HEAVY_HAZE" -> R.string.weather_haze
    "FOG" -> R.string.weather_fog
    "DUST" -> R.string.weather_dust
    else -> R.string.weather_unknown
}
fun failureText(failure: WeatherFailure): Int = when (failure) {
    WeatherFailure.NETWORK -> R.string.error_network
    WeatherFailure.TIMEOUT -> R.string.error_timeout
    WeatherFailure.AUTH -> R.string.error_auth
    WeatherFailure.RATE_LIMIT -> R.string.error_limit
    WeatherFailure.CONFIGURATION -> R.string.error_configuration
    WeatherFailure.INVALID_DATA -> R.string.error_data
    WeatherFailure.SERVER -> R.string.error_server
}
