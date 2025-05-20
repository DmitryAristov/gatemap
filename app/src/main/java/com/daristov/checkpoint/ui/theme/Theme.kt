package com.daristov.checkpoint.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import com.daristov.checkpoint.screens.settings.AppThemeMode

private val LightColors = lightColorScheme(
    primary = LightPrimary,
    secondary = LightSecondary,
    background = LightBackground
)

private val DarkColors = darkColorScheme(
    primary = DarkPrimary,
    secondary = DarkSecondary,
    background = DarkBackground
)

@Composable
fun AppTheme(
    themeMode: AppThemeMode,
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        AppThemeMode.DARK -> true
        AppThemeMode.LIGHT -> false
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    MaterialTheme(
        colorScheme = if (isDark) DarkColors else LightColors,
        typography = Typography(),
        content = content
    )
}

