package com.pyn.sunnyweather

import android.app.Application
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.dataStoreFile
import com.pyn.sunnyweather.data.local.*
import com.pyn.sunnyweather.data.remote.NetworkClient
import com.pyn.sunnyweather.data.repository.DefaultWeatherRepository
import com.pyn.sunnyweather.data.repository.WeatherRepository

class SunnyWeatherApplication : Application() {
    // One app-scoped store and client; explicit constructor injection keeps tests independent.
    private val moshi by lazy { NetworkClient.moshi() }
    private val store by lazy {
        DataStoreFactory.create(
            serializer = WeatherSerializer(moshi),
            corruptionHandler = ReplaceFileCorruptionHandler { CacheEnvelope() },
            produceFile = { dataStoreFile("weather.json") },
        )
    }
    val weatherRepository: WeatherRepository by lazy {
        DefaultWeatherRepository({ NetworkClient.api(moshi) }, { WeatherLocalStore(store) }, BuildConfig.CAIYUN_API_TOKEN)
    }
    val cityCatalog by lazy { CityCatalog { assets.open("cities.tsv").bufferedReader().use { it.readText() } } }
}
