package com.firstyogi

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.glance.material3.ColorProviders


object MyAppWidgetGlanceColorScheme {

    private val lightColors = lightColorScheme(
        primary = Color(0xFFD8DFE7),
        secondary = Color(0xFFC8102F),
        background = Color(0xFF151515),
        surface = Color(0x75151515),
        onPrimary = Color.White,
        onSecondary = Color(0xFFE9EEF4)
    )

    private val darkColors = darkColorScheme(
        primary = Color(0xFF000000),
        secondary = Color(0xFFC8102F),
        background = Color(0xFFD3D3D3),
        surface = Color(0x75D3D3D3),
        onPrimary = Color.Black,
        onSecondary = Color(0xFF151515)
    )

    val colors = ColorProviders(
        light = lightColors,
        dark = darkColors
    )
}