package com.firstyogi.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
private val LightColorPalette = lightColors(
    primary = textLight,
    primaryVariant = FABRed,
    secondary = cardBackgroundLight,
    background = backgroundLight,


)
private val DarkColorPalette = darkColors(
    primary = textDark,
    primaryVariant = FABRed,
    secondary = cardBackgroundDark,
    background = backgroundDark
)



@Composable
fun AppJetpackComposeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) {
        DarkColorPalette
    } else {
        LightColorPalette
    }
    val typography = if (darkTheme) {
        DarkTypography
    } else {
        LightTypography
    }
    MaterialTheme(
        colors = colors,
        typography = typography,
        content = content
    )
}