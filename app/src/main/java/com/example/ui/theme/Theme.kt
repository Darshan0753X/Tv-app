package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = TvCrimsonLight,
    onPrimary = Color.White,
    primaryContainer = TvCrimsonContainer,
    onPrimaryContainer = TvOnCrimsonContainer,
    secondary = TvAccentCyan,
    onSecondary = Color(0xFF00363D),
    secondaryContainer = TvAccentCyanContainer,
    onSecondaryContainer = Color(0xFFB3F5FF),
    tertiary = TvAccentAmber,
    onTertiary = Color(0xFF3E2E04),
    background = TvObsidianBackground,
    onBackground = TvTextPrimary,
    surface = TvSurfaceDark,
    onSurface = TvTextPrimary,
    surfaceVariant = TvSurfaceVariant,
    onSurfaceVariant = TvTextSecondary,
    outline = TvBorderDark,
    outlineVariant = TvSurfaceHighlight
)

private val LightColorScheme = lightColorScheme(
    primary = TvCrimsonPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDADA),
    onPrimaryContainer = TvCrimsonContainer,
    secondary = Color(0xFF006877),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFA1EFFF),
    onSecondaryContainer = Color(0xFF001F25),
    tertiary = Color(0xFF7A5900),
    onTertiary = Color.White,
    background = TvLightBackground,
    onBackground = TvLightTextPrimary,
    surface = TvLightSurface,
    onSurface = TvLightTextPrimary,
    surfaceVariant = TvLightSurfaceVariant,
    onSurfaceVariant = TvLightTextSecondary,
    outline = TvLightBorder,
    outlineVariant = Color(0xFFCBD5E1)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    // Default to dark theme for TV/Cinema companion experience
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> DarkColorScheme // Cinema obsidian dark aesthetic fits TV apps best
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
