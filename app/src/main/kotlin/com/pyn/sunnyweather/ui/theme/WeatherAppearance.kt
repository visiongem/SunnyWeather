package com.pyn.sunnyweather.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// The weather surface always uses light ink; the surrounding search screen follows the system theme.
data class SkyColors(val top: Color, val middle: Color, val bottom: Color, val card: Color, val night: Boolean)
val LocalSkyColors = staticCompositionLocalOf {
    SkyColors(Color(0xFF15172F), Color(0xFF242E50), Color(0xFF344A68), Color(0xB3333C5C), true)
}
@Composable
fun WeatherAppearance(content: @Composable () -> Unit) {
    val night = isSystemInDarkTheme()
    val sky = if (night) SkyColors(Color(0xFF15172F), Color(0xFF242E50), Color(0xFF344A68), Color(0xB3333C5C), true)
        else SkyColors(Color(0xFF204776), Color(0xFF326794), Color(0xFF224C75), Color(0xB32D5D88), false)
    val scheme = darkColorScheme(
        primary = Color(0xFFD5E7FF), onPrimary = Color(0xFF183859),
        background = sky.bottom, onBackground = Color.White,
        surface = sky.card, onSurface = Color.White,
        surfaceVariant = sky.card, onSurfaceVariant = Color(0xFFE0E7F4),
        outline = Color(0xFFA6B7CF), outlineVariant = Color(0x40DDE8FA),
        errorContainer = Color(0xFF593747), onErrorContainer = Color(0xFFFFE3EA),
    )
    CompositionLocalProvider(LocalSkyColors provides sky) {
        MaterialTheme(colorScheme = scheme, typography = MaterialTheme.typography, content = content)
    }
}
