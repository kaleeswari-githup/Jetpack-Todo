package com.example.dothings

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

 val OrangeRadialGradient = Brush.radialGradient(
    colors = listOf(
      // Color(0xFFFF972A),
        Color(0xFFFD7A11),
        Color(0xFFFF852F)
    ),
    center = Offset(75f,50f),
    radius = 200f,
)