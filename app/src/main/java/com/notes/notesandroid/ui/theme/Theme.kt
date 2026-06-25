package com.notes.notesandroid.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.notes.notesandroid.data.model.AppThemeMode

private val DarkColorScheme = darkColorScheme(
    primary = Gold,
    secondary = GoldSoft,
    tertiary = Lilac,
    background = Obsidian,
    surface = NightSurface,
    surfaceContainerLowest = Obsidian,
    surfaceContainerLow = Wine.copy(alpha = 0.58f),
    secondaryContainer = Wine.copy(alpha = 0.72f),
    onPrimary = Obsidian,
    onSecondary = NightInk,
    onBackground = NightInk,
    onSurface = NightInk,
    onSurfaceVariant = GoldSoft.copy(alpha = 0.86f),
)

private val LightColorScheme = lightColorScheme(
    primary = LilacDeep,
    secondary = Beige,
    tertiary = Wine,
    background = Ivory,
    surface = Mist,
    surfaceContainerLowest = Ivory,
    surfaceContainerLow = Beige.copy(alpha = 0.7f),
    secondaryContainer = Lilac.copy(alpha = 0.55f),
    onPrimary = Ivory,
    onSecondary = Ink,
    onBackground = Ink,
    onSurface = Ink,
    onSurfaceVariant = Color(0xFF62555D),
)

@Composable
fun NotesAndroidTheme(
    themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themeMode) {
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
    }

    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        content = content,
    )
}
