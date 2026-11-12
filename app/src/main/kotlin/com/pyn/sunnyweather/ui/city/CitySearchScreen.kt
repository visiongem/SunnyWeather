package com.pyn.sunnyweather.ui.city

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.font.FontWeight
import com.pyn.sunnyweather.model.WeatherSnapshot
import com.pyn.sunnyweather.ui.components.SkyBackdrop
import com.pyn.sunnyweather.ui.weather.conditionText
import com.pyn.sunnyweather.ui.theme.WeatherAppearance
import kotlin.math.roundToInt
import com.pyn.sunnyweather.R
import com.pyn.sunnyweather.model.City

@Composable
fun CitySearchScreen(state: CitySearchState, current: City, onQuery: (String) -> Unit, onBack: () -> Unit, onSelect: (City) -> Unit, snapshot: WeatherSnapshot? = null) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.safeDrawingPadding().imePadding().widthIn(max = 600.dp).fillMaxWidth().padding(horizontal = 20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 12.dp)) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) }
                Text(stringResource(R.string.choose_city), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.SemiBold)
            }
            OutlinedTextField(state.query, onQuery, modifier = Modifier.fillMaxWidth().focusRequester(focus),
                placeholder = { Text(stringResource(R.string.search_hint)) }, singleLine = true,
                shape = RoundedCornerShape(16.dp), leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = { if (state.query.isNotEmpty()) IconButton(onClick = { onQuery("") }) {
                    Icon(Icons.Default.Close, stringResource(R.string.clear_query))
                } })
            Spacer(Modifier.height(20.dp))
            when {
                state.query.isBlank() -> LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 20.dp),
                ) {
                    item { Text(stringResource(R.string.current_selection), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    item { CurrentCityCard(current, snapshot?.takeIf { it.city.id == current.id }, onSelect) }
                    item { Text(stringResource(R.string.search_intro), Modifier.padding(top = 12.dp), color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
                state.loading -> LinearProgressIndicator(Modifier.fillMaxWidth())
                state.failed -> {
                    Text(stringResource(R.string.city_load_failed))
                    TextButton(onClick = { onQuery(state.query) }) { Text(stringResource(R.string.retry)) }
                }
                state.cities.isEmpty() -> Text(stringResource(R.string.no_city), color = MaterialTheme.colorScheme.onSurfaceVariant)
                else -> key(state.query) {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 20.dp)) {
                        items(state.cities, key = { it.id }) { city -> CityRow(city, city.id == current.id, onSelect) }
                    }
                }
            }
        }
    }
}

}

@Composable
private fun CityRow(city: City, selected: Boolean, onSelect: (City) -> Unit) {
    Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .65f)) {
    Column(Modifier.fillMaxWidth().clickable { onSelect(city) }.padding(20.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(city.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            if (selected) Text(stringResource(R.string.city_selected), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
        }
        Text(city.address, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 5.dp))
    }
    }
}

@Composable
private fun CurrentCityCard(city: City, snapshot: WeatherSnapshot?, onSelect: (City) -> Unit) {
    WeatherAppearance {
        Surface(shape = RoundedCornerShape(24.dp), onClick = { onSelect(city) }) {
            Box(Modifier.fillMaxWidth().heightIn(min = 148.dp)) {
                SkyBackdrop(Modifier.matchParentSize())
                Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(city.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                            Text(city.address, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (snapshot != null) Text(buildAnnotatedString {
                            append(snapshot.current.temperature.roundToInt().toString())
                            withStyle(SpanStyle(fontSize = 20.sp, baselineShift = BaselineShift.Superscript)) { append("°C") }
                        }, fontSize = 48.sp, fontWeight = FontWeight.Light)
                    }
                    if (snapshot != null) {
                        val at = remember(snapshot.fetchedAt, snapshot.timezone) {
                            java.time.Instant.ofEpochMilli(snapshot.fetchedAt).atZone(snapshot.zone())
                                .format(java.time.format.DateTimeFormatter.ofPattern("M/d HH:mm"))
                        }
                        Text(stringResource(conditionText(snapshot.current.condition)) + " · " + stringResource(R.string.updated_at, at),
                            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else Text(stringResource(R.string.city_selected), style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}
