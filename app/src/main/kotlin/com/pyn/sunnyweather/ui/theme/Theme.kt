package com.pyn.sunnyweather.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Light = lightColorScheme(
    primary = Color(0xFF185BC4), onPrimary = Color.White,
    background = Color(0xFFF4F8FC), onBackground = Color(0xFF172F49),
    surface = Color.White, onSurface = Color(0xFF172F49),
    surfaceVariant = Color(0xFFE9F0F7), onSurfaceVariant = Color(0xFF52677C),
    outline = Color(0xFF6E8399), outlineVariant = Color(0xFFDCE5EF),
    errorContainer = Color(0xFFFDECEF), onErrorContainer = Color(0xFFA52332),
)
private val Dark = darkColorScheme(
    primary = Color(0xFF9CC7FF), onPrimary = Color(0xFF102440),
    background = Color(0xFF101322), onBackground = Color(0xFFE8F2FC),
    surface = Color(0xFF202538), onSurface = Color(0xFFE8F2FC),
    surfaceVariant = Color(0xFF292F46), onSurfaceVariant = Color(0xFFA6BACF),
    outline = Color(0xFF8AA4BE), outlineVariant = Color(0xFF31475D),
    errorContainer = Color(0xFF44232B), onErrorContainer = Color(0xFFFFB3BB),
)
@Composable
fun SunnyWeatherTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (darkTheme) Dark else Light, typography = Typography, content = content)
}
