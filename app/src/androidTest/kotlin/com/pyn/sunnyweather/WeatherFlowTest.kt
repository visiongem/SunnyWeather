package com.pyn.sunnyweather

import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import androidx.test.platform.app.InstrumentationRegistry
import com.pyn.sunnyweather.data.local.*
import com.pyn.sunnyweather.data.remote.*
import com.pyn.sunnyweather.data.repository.DefaultWeatherRepository
import com.pyn.sunnyweather.model.City
import com.pyn.sunnyweather.ui.city.*
import com.pyn.sunnyweather.ui.theme.SunnyWeatherTheme
import com.pyn.sunnyweather.ui.weather.*
import org.junit.After
import org.junit.Rule
import org.junit.Test
import java.io.IOException

class WeatherFlowTest {
    @get:Rule val compose = createComposeRule()
    private val models = ViewModelStore()
    @After fun clearModels() { compose.runOnUiThread { models.clear() } }

    @Test fun chineseSearchSelectRefreshFailureAndClearQuery() {
        var fail = false
        val api = object : WeatherApi {
            override suspend fun weather(token: String, location: String, language: String, unit: String, hours: Int, days: Int): WeatherResponseDto {
                if (fail) throw IOException("offline")
                return WeatherResponseDto("ok", "Asia/Shanghai", WeatherResultDto(RealtimeDto("ok", 25.0, "CLEAR_DAY")))
            }
        }
        val store = object : WeatherStore {
            var saved = StoredWeather()
            override suspend fun read() = saved
            override suspend fun write(value: StoredWeather) { saved = value }
        }
        val repo = DefaultWeatherRepository({ api }, { store }, "test-token")
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        lateinit var weather: WeatherViewModel
        lateinit var searchModel: CitySearchViewModel
        compose.runOnUiThread {
            weather = WeatherViewModel(repo)
            models.put("weather",weather)
        }
        compose.setContent {
            val state by weather.state.collectAsState()
            val search = remember { CitySearchViewModel(CityCatalog { context.assets.open("cities.tsv").bufferedReader().use { it.readText() } }, SavedStateHandle()).also { models.put("search",it); searchModel = it } }
            val searchState by search.state.collectAsState()
            var searching by remember { mutableStateOf(false) }
            SunnyWeatherTheme {
                if (searching) CitySearchScreen(searchState,state.session.city,search::search,{ searching=false },{
                    weather.selectCity(it); searching=false
                }) else WeatherScreen(state,{ search.search(""); searching=true },weather::refresh)
            }
        }
        compose.waitUntil(10_000) { !weather.state.value.refreshing }
        compose.onNodeWithContentDescription("25 摄氏度").assertIsDisplayed()
        compose.onNodeWithContentDescription("广东省深圳市，切换城市").performClick()
        compose.onNode(hasSetTextAction()).performTextInput("北京")
        compose.waitUntil(10_000) { compose.onAllNodesWithText("北京市",substring=false).fetchSemanticsNodes().isNotEmpty() }
        compose.onAllNodesWithText("北京市",substring=false).onFirst().performClick()
        compose.waitUntil(10_000) { weather.state.value.session.city.name == "北京市" && !weather.state.value.refreshing }
        compose.onNodeWithContentDescription("北京市，切换城市").assertIsDisplayed()
        compose.runOnIdle { fail=true }
        compose.onNodeWithContentDescription("刷新天气").assertDoesNotExist()
        compose.onNodeWithTag("weather_pull_refresh").performTouchInput { swipeDown(durationMillis = 600) }
        compose.waitUntil(10_000) { weather.state.value.error != null }
        compose.onNodeWithContentDescription("25 摄氏度").assertIsDisplayed()
        compose.onNodeWithText("更新失败，显示上次获取的数据",substring=true).assertIsDisplayed()
        compose.onNodeWithContentDescription("北京市，切换城市").performClick()
        compose.onNode(hasSetTextAction()).performTextInput("SHEN ZHEN")
        compose.waitUntil(10_000) { compose.onAllNodesWithText("深圳市",substring=false).fetchSemanticsNodes().isNotEmpty() }
        compose.onNode(hasSetTextAction()).performTextReplacement("SZ")
        compose.waitUntil(10_000) { searchModel.state.value.query == "SZ" && !searchModel.state.value.loading }
        compose.onNode(hasScrollToNodeAction()).performScrollToNode(hasText("苏州市",substring=false))
        compose.onNodeWithText("苏州市",substring=false).assertIsDisplayed()
        compose.onNode(hasScrollToNodeAction()).performScrollToNode(hasText("深圳市",substring=false))
        compose.onNodeWithText("深圳市",substring=false).assertIsDisplayed()
        compose.onNode(hasSetTextAction()).performTextReplacement("chongqing")
        compose.waitUntil(10_000) { compose.onAllNodesWithText("重庆市",substring=false).fetchSemanticsNodes().isNotEmpty() }
        compose.onAllNodesWithText("重庆市",substring=false).onFirst().performClick()
        compose.waitUntil(10_000) { weather.state.value.session.city.name == "重庆市" && !weather.state.value.refreshing }
        compose.onNodeWithContentDescription("重庆市，切换城市").assertIsDisplayed()
        compose.onNodeWithContentDescription("重庆市，切换城市").performClick()
        compose.onNode(hasSetTextAction()).performTextInput("无此地点")
        compose.waitUntil(10_000) { compose.onAllNodesWithText("未找到相关地点，试试完整城市或区县名称").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithContentDescription("清空搜索").performClick()
        compose.onNodeWithText("当前选择").assertIsDisplayed()
    }
}
