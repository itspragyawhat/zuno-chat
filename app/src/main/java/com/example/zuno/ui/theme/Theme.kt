package com.example.zuno.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = BaseBlue,
    secondary = SoftPink,
    background = SoftCream,
    surface = SoftCream,
    onPrimary = WhiteText,
    onSecondary = WhiteText,
    onBackground = BlackText,
    onSurface = BlackText
)

private val DarkColors = darkColorScheme(
    primary = BaseBlue,
    secondary = SoftPink,
    background = BaseBlue,
    surface = BaseBlue,
    onPrimary = WhiteText,
    onSecondary = WhiteText,
    onBackground = WhiteText,
    onSurface = WhiteText
)

@Composable
fun ZunoTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme || isSystemInDarkTheme()) DarkColors else LightColors

    MaterialTheme(
        colorScheme = colors,
        typography = Typography,
        content = content
    )
}