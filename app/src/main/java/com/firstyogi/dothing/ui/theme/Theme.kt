package com.firstyogi.dothing.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.firstyogi.ui.theme.DarkTypography
import com.firstyogi.ui.theme.LightTypography

private val LightColorPalette = lightColors(
    primary = Color.White,
    primaryVariant = com.firstyogi.ui.theme.FABRed,
    secondary = com.firstyogi.ui.theme.Text1,
    background = com.firstyogi.ui.theme.MarkCompleteBack

)
private val DarkColorPalette = darkColors(
    primary = com.firstyogi.ui.theme.Text1,
    primaryVariant = com.firstyogi.ui.theme.FABRed,
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
    androidx.compose.material.MaterialTheme(
        colors = colors,
        typography = typography,
        content = content
    )
}