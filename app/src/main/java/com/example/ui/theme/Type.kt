package com.example.ui.theme

import androidx.compose.material.Typography
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.dothings.interDisplayFamily

// Set of Material typography styles to start with
val LightTypography = Typography(
    h1 = TextStyle(
        fontFamily = interDisplayFamily,
        fontWeight = FontWeight.Bold,
        color = Color.White,
        fontSize = 24.sp
    ),
    h2 = TextStyle(
        fontFamily = interDisplayFamily,
        fontWeight = FontWeight.Bold,
        color = Text1,
        fontSize = 24.sp
    )

)
val DarkTypography = Typography(
    h1 = TextStyle(
        fontFamily = interDisplayFamily,
        fontWeight = FontWeight.Bold,
        color = Color.Black,
        fontSize = 24.sp
    ),
    h2 = TextStyle(
        fontFamily = interDisplayFamily,
        fontWeight = FontWeight.Bold,
        color = FABRed,
        fontSize = 24.sp
    )
)
