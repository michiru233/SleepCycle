package com.example.sleepcycle.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF9CCAFF),
    onPrimary = Color(0xFF003258),
    primaryContainer = Color(0xFF00497D),
    onPrimaryContainer = Color(0xFFD1E4FF),
    secondary = Color(0xFF80D5CB),
    onSecondary = Color(0xFF003733),
    secondaryContainer = Color(0xFF004F4A),
    onSecondaryContainer = Color(0xFF9DF2E8),
    tertiary = Color(0xFFFFB4AB),
    onTertiary = Color(0xFF690005),
    tertiaryContainer = Color(0xFF93000A),
    onTertiaryContainer = Color(0xFFFFDAD6),
    background = Color(0xFF0F141C),
    onBackground = Color(0xFFE0E2EC),
    surface = Color(0xFF181C24),
    onSurface = Color(0xFFE0E2EC),
    surfaceVariant = Color(0xFF232834),
    onSurfaceVariant = Color(0xFFC3C7D4)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF1B60A5),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD1E4FF),
    onPrimaryContainer = Color(0xFF001D36),
    secondary = Color(0xFF006A63),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFF9DF2E8),
    onSecondaryContainer = Color(0xFF00201D),
    tertiary = Color(0xFFBA1A1A),
    onTertiary = Color(0xFFFFFFFF),
    background = Color(0xFFF7F9FF),
    onBackground = Color(0xFF181C22),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF181C22),
    surfaceVariant = Color(0xFFDFE2EE),
    onSurfaceVariant = Color(0xFF434751)
)

@Composable
fun SleepCycleTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
