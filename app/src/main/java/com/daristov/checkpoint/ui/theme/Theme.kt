package com.daristov.checkpoint.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.daristov.checkpoint.screens.settings.AppThemeMode

//private val LightColors = lightColorScheme(
//    primary = LightPrimary,
//    secondary = LightSecondary,
//    background = LightBackground,
//    surface = LightSurface
//)
//
//private val DarkColors = darkColorScheme(
//    primary = DarkPrimary,
//    secondary = DarkSecondary,
//    background = DarkBackground,
//    surface = DarkSurface
//)

val LightColors = lightColorScheme(
    primary = LightPrimary,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface
)

val DarkColors = darkColorScheme(
    primary = DarkPrimary,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface
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
        content = {
            ApplySystemBarColors(
                darkIcons = !isDark,
                statusBarColor = Color.Transparent, // Или твой цвет
                navigationBarColor = if (isDark) DarkBackground else LightBackground
            )
            content()
        }
    )
}

@Composable
fun ApplySystemBarColors(
    darkIcons: Boolean,
    statusBarColor: Color = Color.Transparent,
    navigationBarColor: Color = Color.Black
) {
    val window = (LocalView.current.context as? Activity)?.window ?: return
    SideEffect {
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = darkIcons
            isAppearanceLightNavigationBars = darkIcons
        }
        window.statusBarColor = statusBarColor.toArgb()
        window.navigationBarColor = navigationBarColor.toArgb()
    }
}
