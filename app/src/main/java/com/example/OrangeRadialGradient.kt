package com.example.dothings

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

 val OrangeRadialGradient = Brush.radialGradient(
    colors = listOf(
        Color(0xFFFF972A),
        Color(0xFFFD7A11),
        Color(0xFFFF852F)
    ),
    center = Offset(0.2031f,0.0938f),
    radius = 90.62f,
)