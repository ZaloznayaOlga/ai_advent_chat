package com.olgaz.aichat.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Blue80,
    onPrimary = Color.Black,
    secondary = GreenUser,
    onSecondary = Color.Black,
    secondaryContainer = GreenAssistant,
    onSecondaryContainer = Color.Black,
    tertiary = GreyMediumDark
)

private val LightColorScheme = lightColorScheme(
    primary = Blue40,
    onPrimary = Color.White,
    secondary = GreenUser,
    onSecondary = Color.Black,
    secondaryContainer = GreenAssistant,
    onSecondaryContainer = Color.Black,
    tertiary = GreyMediumLight
)

@Composable
fun AIChatTheme(
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