package com.pyn.sunnyweather.ui.weather

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pyn.sunnyweather.R
import com.pyn.sunnyweather.model.CurrentWeather
import com.pyn.sunnyweather.model.WeatherFailure
import com.pyn.sunnyweather.ui.components.SkyBackdrop
import com.pyn.sunnyweather.ui.components.WeatherIcon
import com.pyn.sunnyweather.ui.theme.LocalSkyColors
import com.pyn.sunnyweather.ui.theme.WeatherAppearance
import kotlin.math.roundToInt

@Composable
fun WeatherScreen(state: WeatherUiState, onSearch: () -> Unit, onRefresh: () -> Unit,
                  listState: LazyListState = rememberLazyListState()) {
    WeatherAppearance { WeatherContent(state, onSearch, onRefresh, listState) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WeatherContent(state: WeatherUiState, onSearch: () -> Unit, onRefresh: () -> Unit, listState: LazyListState) {
    val colors = MaterialTheme.colorScheme
    val presentation = state.presentation
    val refreshLabel = stringResource(R.string.refresh_weather)
    CompositionLocalProvider(LocalContentColor provides colors.onBackground) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            SkyBackdrop(Modifier.matchParentSize())
            PullToRefreshBox(isRefreshing = state.refreshing && presentation != null, onRefresh = onRefresh,
                modifier = Modifier.widthIn(max = 600.dp).fillMaxSize().safeDrawingPadding().testTag("weather_pull_refresh")
                    .semantics { customActions = listOf(CustomAccessibilityAction(refreshLabel) { onRefresh(); true }) }) {
                LazyColumn(state = listState, contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    item(key = "hero") { WeatherHero(state, onSearch) }
                    if (presentation == null) {
                        item(key = "empty") { EmptyWeather(state, onRefresh) }
                    } else {
                        state.error?.let { failure -> item(key = "error") {
                            Notice(stringResource(R.string.cached_error) + "\n" + stringResource(failureText(failure)), onRefresh, state.refreshing)
                        } }
                        if (state.session.storageFailed) item(key = "storage") { Notice(stringResource(R.string.storage_failed)) }
                        item(key = "hourly") { HourlyCard(presentation) }
                        item(key = "daily") { DailyCard(presentation) }
                        item(key = "details") { DetailsCard(presentation.current) }
                        item(key = "source") {
                            Text(stringResource(R.string.data_source), Modifier.fillMaxWidth().padding(vertical = 16.dp),
                                textAlign = TextAlign.Center, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WeatherHero(state: WeatherUiState, onSearch: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val city = state.session.city
    val presentation = state.presentation
    Column(Modifier.fillMaxWidth().padding(top = 22.dp, bottom = 22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        val changeCity = stringResource(R.string.change_city, city.address)
        TextButton(onClick = onSearch, shape = RectangleShape, contentPadding = PaddingValues(8.dp),
            modifier = Modifier.semantics { contentDescription = changeCity },
            colors = ButtonDefaults.textButtonColors(contentColor = colors.onBackground)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f, fill = false)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(city.name, fontSize = 30.sp, lineHeight = 38.sp, fontWeight = FontWeight.Normal,
                        textAlign = TextAlign.Center, modifier = Modifier.weight(1f, fill = false))
                    Icon(Icons.Default.ArrowDropDown, null, Modifier.size(24.dp))
                }
                Text(city.address, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant, textAlign = TextAlign.Center)
            }
        }
        if (presentation != null) {
            val current = presentation.current
            val accessible = stringResource(R.string.temperature_accessible, current.temperature.roundToInt())
            Text(buildAnnotatedString {
                append(current.temperature.roundToInt().toString())
                withStyle(SpanStyle(fontSize = 36.sp, letterSpacing = 0.sp, baselineShift = BaselineShift.Superscript)) { append("°C") }
            }, Modifier.semantics { contentDescription = accessible },
                fontSize = 104.sp, lineHeight = 116.sp, fontWeight = FontWeight.Light, letterSpacing = (-3).sp)
            Text(stringResource(conditionText(current.condition)), style = MaterialTheme.typography.titleLarge, color = colors.onSurfaceVariant)
            presentation.today?.let {
                Text(stringResource(R.string.high_low, it.max, it.min),
                    Modifier.padding(top = 8.dp), style = MaterialTheme.typography.bodyLarge)
            }
            Text(stringResource(R.string.updated_at, presentation.updatedAt), Modifier.padding(top = 20.dp),
                style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            val status = when {
                presentation.isOld -> R.string.old_record
                state.refreshing && state.session.fromCache -> R.string.cache_refreshing
                state.refreshing -> R.string.refreshing
                else -> R.string.pull_to_refresh
            }
            Text(stringResource(status), Modifier.padding(top = 4.dp),
                style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun WeatherCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Surface(shape = RoundedCornerShape(22.dp), color = LocalSkyColors.current.card,
        border = BorderStroke(1.dp, Color.White.copy(alpha = .09f))) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            content()
        }
    }
}

@Composable
private fun HourlyCard(presentation: WeatherPresentation) {
    val hours = presentation.hours
    WeatherCard(stringResource(R.string.hourly_title)) {
        if (hours.isEmpty()) Text(stringResource(if (presentation.hoursExpired) R.string.expired_forecast else R.string.no_hourly), color = MaterialTheme.colorScheme.onSurfaceVariant)
        else LazyRow(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            items(hours, key = { it.time }) { hour ->
                val desc = stringResource(conditionText(hour.condition))
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(hour.label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    WeatherIcon(hour.condition, Modifier.size(32.dp).semantics { contentDescription = desc })
                    Text(stringResource(R.string.temperature_celsius, hour.temperature), style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

@Composable
private fun DailyCard(presentation: WeatherPresentation) {
    val days = presentation.days
    WeatherCard(if (days.isEmpty()) stringResource(R.string.daily_title_empty) else stringResource(R.string.daily_title, days.size)) {
        if (days.isEmpty()) Text(stringResource(if (presentation.daysExpired) R.string.expired_forecast else R.string.no_daily), color = MaterialTheme.colorScheme.onSurfaceVariant)
        days.forEachIndexed { index, day ->
            val label = day.label ?: stringResource(R.string.today)
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text(label, style = MaterialTheme.typography.bodyLarge)
                        Text(stringResource(conditionText(day.condition)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    WeatherIcon(day.condition, Modifier.size(30.dp))
                    Text(stringResource(R.string.temperature_celsius, day.min), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (LocalDensity.current.fontScale <= 1.2f) TemperatureRange(day.rangeStart, day.rangeEnd, Modifier.width(64.dp).height(6.dp))
                    Text(stringResource(R.string.temperature_celsius, day.max), style = MaterialTheme.typography.titleMedium)
                }
                if (index < days.lastIndex) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
}

@Composable
private fun TemperatureRange(rangeStart: Float, rangeEnd: Float, modifier: Modifier) {
    val gradient = remember { Brush.horizontalGradient(listOf(Color(0xFF8ED4D5), Color(0xFFF2CF7F))) }
    Canvas(modifier) {
        val start = rangeStart * size.width
        val end = rangeEnd * size.width
        drawLine(Color.White.copy(alpha = .12f), Offset(0f, size.height/2), Offset(size.width,size.height/2), size.height, StrokeCap.Round)
        drawLine(gradient,
            Offset(start,size.height/2), Offset(end.coerceAtLeast(start + 1f),size.height/2), size.height, StrokeCap.Round)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DetailsCard(current: CurrentWeather) {
    WeatherCard(stringResource(R.string.details_title)) {
        val missing = stringResource(R.string.no_value)
        val values = listOf(
            stringResource(R.string.feels_like) to (current.apparent?.roundToInt()?.let { stringResource(R.string.temperature_celsius, it) } ?: missing),
            stringResource(R.string.humidity) to (current.humidity?.let { "${(it * 100).roundToInt()}%" } ?: missing),
            stringResource(R.string.wind_speed) to (current.windKmh?.roundToInt()?.let { "$it km/h" } ?: missing))
        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(28.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            values.forEach { (label, value) -> Column(Modifier.widthIn(min = 64.dp)) {
                Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(value, Modifier.padding(top = 8.dp), style = MaterialTheme.typography.titleMedium)
            } }
        }
    }
}

@Composable
private fun EmptyWeather(state: WeatherUiState, onRefresh: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(vertical = 64.dp), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)) {
        if (state.refreshing) {
            CircularProgressIndicator(Modifier.size(36.dp))
            Text(stringResource(R.string.loading_weather))
        } else {
            WeatherIcon("CLOUDY", Modifier.size(80.dp))
            Text(stringResource(failureText(state.error ?: WeatherFailure.SERVER)))
            Button(onClick = onRefresh) { Text(stringResource(R.string.retry)) }
        }
    }
}

@Composable
private fun Notice(message: String, onRetry: (() -> Unit)? = null, busy: Boolean = false) {
    Surface(color = MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text(message, color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodyMedium)
            if (onRetry != null) TextButton(onClick = onRetry, enabled = !busy) { Text(stringResource(R.string.retry)) }
        }
    }
}
