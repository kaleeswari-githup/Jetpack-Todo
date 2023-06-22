package com.example.dothings

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode

val roundedCircleGradient = Brush.radialGradient(
    colors = listOf(

        Color(0xFFF0F0F0),
        Color(0xFFF8F8F8),
        Color(0xFFE7E5E5),
        Color(0xFFFFFFFF),


    ),
    center = Offset(90f,200f),
    radius = 400f,
    tileMode = TileMode.Repeated,
    )

val bigRoundedCircleGradient = Brush.radialGradient(
    colors = listOf(
        Color(0xFFFFFFFF),
        Color(0xFFF0F0F0),
        Color(0xFFF8F8F8),
        Color(0xFFE7E5E5),



        ),
    center = Offset(400f,300f),
    radius = 700f,
    tileMode = TileMode.Repeated,
)