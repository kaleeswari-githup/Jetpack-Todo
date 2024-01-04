package com.example.Pages.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.example.ui.theme.DarkTypography
import com.example.ui.theme.LightTypography

private val LightColorPalette = lightColors(
    primary = Color.White,
    primaryVariant = com.example.ui.theme.FABRed,
    secondary = com.example.ui.theme.Text1,
    background = com.example.ui.theme.MarkCompleteBack

)
private val DarkColorPalette = darkColors(
    primary = com.example.ui.theme.Text1,
    primaryVariant = com.example.ui.theme.FABRed,
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