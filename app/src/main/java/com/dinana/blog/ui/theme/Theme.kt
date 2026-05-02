package com.dinana.blog.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = GitHubBlue,
    onPrimary = Gray00,
    primaryContainer = GitHubBlueLight,
    onPrimaryContainer = GitHubHeader,

    secondary = Gray500,
    onSecondary = Gray00,
    secondaryContainer = Gray100,
    onSecondaryContainer = Gray700,

    tertiary = GitHubGreen,
    onTertiary = Gray00,

    background = Gray50,
    onBackground = Gray900,

    surface = Gray00,
    onSurface = Gray900,
    surfaceVariant = Gray50,
    onSurfaceVariant = Gray500,

    outline = Gray200,
    outlineVariant = Gray100,

    error = GitHubRed,
    onError = Gray00,
    errorContainer = Color(0xFFFFDFE0),
    onErrorContainer = Color(0xFF8B1A1A),
)

private val DarkColorScheme = darkColorScheme(
    primary = GitHubBlueDark,
    onPrimary = Gray900,
    primaryContainer = Color(0xFF0D419D),
    onPrimaryContainer = GitHubBlueLight,

    secondary = Gray400,
    onSecondary = Gray900,
    secondaryContainer = Gray700,
    onSecondaryContainer = Gray200,

    tertiary = GitHubGreenDark,
    onTertiary = Gray900,

    background = DarkBg,
    onBackground = Gray200,

    surface = DarkSurface,
    onSurface = Gray200,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = Gray400,

    outline = DarkBorder,
    outlineVariant = Gray700,

    error = GitHubRedDark,
    onError = Gray900,
    errorContainer = Color(0xFF5C1010),
    onErrorContainer = Color(0xFFFFDFE0),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun appBarColors(darkTheme: Boolean = isSystemInDarkTheme()): TopAppBarColors =
    TopAppBarDefaults.topAppBarColors(
        containerColor = if (darkTheme) DarkBg else Gray50,
        scrolledContainerColor = if (darkTheme) DarkSurface else Gray00,
        titleContentColor = if (darkTheme) Gray200 else Gray900,
        navigationIconContentColor = if (darkTheme) Gray200 else Gray900,
        actionIconContentColor = if (darkTheme) Gray200 else Gray900,
    )

@Composable
fun DinanaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as android.app.Activity).window
            window.statusBarColor = if (darkTheme) DarkBg.toArgb() else Gray50.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
