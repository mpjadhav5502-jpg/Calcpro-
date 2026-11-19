package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.example.data.preferences.AppThemeMode

private val DarkScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    secondary = DarkSecondary,
    onSecondary = DarkOnSecondary,
    secondaryContainer = DarkSecondaryContainer,
    onSecondaryContainer = DarkOnSecondaryContainer,
    tertiary = DarkTertiary,
    onTertiary = DarkOnTertiary,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    surfaceContainer = DarkSurfaceVariant,
    surfaceContainerHigh = DarkSurfaceHigh,
    outline = DarkOutline,
    error = DarkError
)

private val LightScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = LightOnPrimaryContainer,
    secondary = LightSecondary,
    onSecondary = LightOnSecondary,
    secondaryContainer = LightSecondaryContainer,
    onSecondaryContainer = LightOnSecondaryContainer,
    tertiary = LightTertiary,
    onTertiary = LightOnTertiary,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    surfaceContainer = LightSurfaceVariant,
    surfaceContainerHigh = LightSurfaceHigh,
    outline = LightOutline,
    error = LightError
)

private val ObsidianVioletScheme = darkColorScheme(
    primary = AmoledPrimary,
    onPrimary = DarkBackground,
    primaryContainer = AmoledPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    secondary = DarkSecondary,
    onSecondary = DarkOnSecondary,
    secondaryContainer = AmoledSurfaceVariant,
    onSecondaryContainer = DarkOnSecondaryContainer,
    tertiary = AmoledTertiary,
    onTertiary = DarkBackground,
    background = AmoledBackground,
    onBackground = DarkOnBackground,
    surface = AmoledSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = AmoledSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    surfaceContainer = AmoledSurface,
    surfaceContainerHigh = AmoledSurfaceVariant,
    outline = DarkOutline,
    error = DarkError
)

private val RoyalGoldScheme = darkColorScheme(
    primary = GoldPrimary,
    onPrimary = GoldOnPrimary,
    primaryContainer = GoldPrimaryContainer,
    onPrimaryContainer = GoldOnPrimaryContainer,
    secondary = GoldSecondary,
    onSecondary = DarkOnSecondary,
    secondaryContainer = GoldSurfaceVariant,
    onSecondaryContainer = DarkOnSecondaryContainer,
    tertiary = DarkTertiary,
    onTertiary = DarkOnTertiary,
    background = GoldBackground,
    onBackground = DarkOnBackground,
    surface = GoldSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = GoldSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    surfaceContainer = GoldSurface,
    surfaceContainerHigh = GoldSurfaceVariant,
    outline = DarkOutline,
    error = DarkError
)

val CalcProShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(30.dp)
)

@Composable
fun CalcProTheme(
    themeMode: AppThemeMode = AppThemeMode.DARK,
    isPremium: Boolean = false,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val effectiveMode = when (themeMode) {
        AppThemeMode.OBSIDIAN_VIOLET, AppThemeMode.ROYAL_GOLD -> {
            if (isPremium) themeMode else AppThemeMode.DARK
        }
        else -> themeMode
    }

    val isDark = when (effectiveMode) {
        AppThemeMode.LIGHT -> false
        AppThemeMode.SYSTEM -> systemDark
        else -> true
    }

    val colorScheme = when (effectiveMode) {
        AppThemeMode.DARK -> DarkScheme
        AppThemeMode.LIGHT -> LightScheme
        AppThemeMode.SYSTEM -> if (systemDark) DarkScheme else LightScheme
        AppThemeMode.OBSIDIAN_VIOLET -> ObsidianVioletScheme
        AppThemeMode.ROYAL_GOLD -> RoyalGoldScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = !isDark
                    isAppearanceLightNavigationBars = !isDark
                }
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = CalcProShapes,
        content = content
    )
}
