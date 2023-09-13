package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
private val LightColorPalette = lightColors(
    primary = Color.White,
    primaryVariant = FABRed,
    secondary = Text1,
    background = MarkCompleteBack

)
private val DarkColorPalette = darkColors(
    primary = Text1,
    primaryVariant = FABRed,
    secondary = Color.White,
    background = Color.Black
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