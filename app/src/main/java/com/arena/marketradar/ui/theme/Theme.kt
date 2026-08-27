package com.arena.marketradar.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

val Green = Color(0xFF1E9E6A)
val Red = Color(0xFFE1554D)
val Amber = Color(0xFFF6B93B)
val Neutral = Color(0xFF607D8B)

private val Dark = darkColorScheme(
    primary = Color(0xFF3FCE96),
    onPrimary = Color(0xFF00351F),
    background = Color(0xFF0E1419),
    surface = Color(0xFF151C23),
    surfaceVariant = Color(0xFF1E2832),
    onBackground = Color(0xFFE7EDF2),
    onSurface = Color(0xFFE7EDF2),
    onSurfaceVariant = Color(0xFF9AA7B4),
    error = Color(0xFFE1554D),
    secondary = Amber,
)

private val Light = lightColorScheme(
    primary = Color(0xFF12805A),
    onPrimary = Color(0xFFFFFFFF),
    background = Color(0xFFF3F6F8),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFE8EEF2),
    onBackground = Color(0xFF15201A),
    onSurface = Color(0xFF15201A),
    onSurfaceVariant = Color(0xFF5B6B76),
    error = Color(0xFFE1554D),
    secondary = Color(0xFFB77E12),
)

/**
 * App theme. When the device is Android 12+ and dynamic colour is enabled,
 * Material You palette from the wallpaper is used (the app "feels native").
 * @param dynamicColor enables Material You (Android 12+, default true).
 */
@Composable
fun MarketRadarTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val scheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> Dark
        else -> Light
    }
    MaterialTheme(colorScheme = scheme, typography = Typography(), content = content)
}

// Color helpers used across components
fun signalColor(signal: String): Color = when (signal) { "BULLISH" -> Green; "BEARISH" -> Red; else -> Neutral }

@Composable fun priceColor(change: Double?): Color = when {
    change == null -> Neutral; change > 0 -> Green; change < 0 -> Red; else -> Neutral
}
