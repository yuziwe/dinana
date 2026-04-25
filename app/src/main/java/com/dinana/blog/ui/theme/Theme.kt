package com.dinana.blog.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = Black,
    onPrimary = White,
    primaryContainer = Gray100,
    onPrimaryContainer = Black,

    secondary = Gray700,
    onSecondary = White,
    secondaryContainer = Gray100,
    onSecondaryContainer = Black,

    tertiary = Gray500,
    onTertiary = White,

    background = White,
    onBackground = Black,

    surface = White,
    onSurface = Black,
    surfaceVariant = Gray50,
    onSurfaceVariant = Gray500,

    error = Black,
    onError = White,
)

private val DarkColorScheme = darkColorScheme(
    primary = White,
    onPrimary = Black,
    primaryContainer = Gray800,
    onPrimaryContainer = White,

    secondary = Gray300,
    onSecondary = Black,
    secondaryContainer = Gray700,
    onSecondaryContainer = White,

    tertiary = Gray500,
    onTertiary = Black,

    background = Black,
    onBackground = White,

    surface = Gray900,
    onSurface = White,
    surfaceVariant = Gray800,
    onSurfaceVariant = Gray300,

    error = White,
    onError = Black,
)

@Composable
fun DinanaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
