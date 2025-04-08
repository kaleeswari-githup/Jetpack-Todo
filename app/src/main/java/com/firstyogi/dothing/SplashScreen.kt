package com.firstyogi.dothing

import androidx.compose.animation.core.EaseInBack
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(navController: NavController){
    val coroutineScope = rememberCoroutineScope()
    var visible by remember {
        mutableStateOf(false)
    }

    LaunchedEffect(Unit) {

        visible = true // Set the visibility to true to trigger the animation

    }
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioHighBouncy,
            stiffness = Spring.StiffnessVeryLow
        )
    )
    val opacity by animateFloatAsState(
        targetValue = if (visible) 0f else 1f,
        animationSpec = tween(

                durationMillis =300,
            delayMillis = 500,
            easing = EaseInBack
        )

    )



    val offsetY by animateDpAsState(
        targetValue = if (visible) -400.dp else 0.dp,
        animationSpec = tween(
            durationMillis = 400,
            delayMillis = 400,
            easing = EaseInBack
        )
    )
    LaunchedEffect(Unit) {
        // Wait for a certain duration (e.g., 2 seconds) using delay()
        delay(900)

        // Navigate to the sign-in page using the provided NavController
      //  navController.navigate(Screen.Main.route)
    }
    Box(modifier = Modifier
        .fillMaxSize()
        .background(color = MaterialTheme.colors.background)) {
        ThemedGridImage(modifier = Modifier)

        Box(modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
        ) {
            val isDarkTheme = isSystemInDarkTheme()
            Canvas(modifier = Modifier.fillMaxSize()) {

                val gradientBrush = Brush.verticalGradient(
                    colors = if(isDarkTheme){
                        listOf(
                            Color(0xFF000000),
                            Color(0x00000000)
                        )
                    }else{
                        listOf(
                            Color(0xFFEDEDED),
                            Color(0x00EDEDED)
                        )
                    }
                    ,
                    startY = 0f,
                    endY = size.height.coerceAtMost(100.dp.toPx())
                )
                val opacityBrush = Brush.verticalGradient(
                    colors = if (isDarkTheme){
                        listOf(
                            Color(0x00000000),
                            Color(0xFF000000)
                        )
                    }else{
                        listOf(
                            Color(0x00EDEDED),
                            Color(0xFFEDEDED)
                        )
                    }
                    ,
                    startY = (size.height - 84.dp.toPx()).coerceAtLeast(0f),
                    endY = size.height
                )
                drawRect(brush = gradientBrush)
                drawRect(brush = opacityBrush)
                //  drawRect(brush = gradientBrush)
            }
           // ThemedImage(modifier = Modifier.scale(scale).offset(y=offsetY).alpha(opacity))
        }
    }
}
@Composable
fun ThemedGridImage(modifier: Modifier) {
    val isDarkTheme = isSystemInDarkTheme()
    val imageRes = if (isDarkTheme) {
        R.drawable.grid_line_white
    } else {
        R.drawable.grid_line_black
    }

    Image(
        painter = painterResource(id = imageRes),
        contentDescription = null,
        modifier = modifier
            .scale(2.8f,2.5f)
            .alpha(if (isDarkTheme){
                0.08f
            }else{
                0.03f
            })
    )
}