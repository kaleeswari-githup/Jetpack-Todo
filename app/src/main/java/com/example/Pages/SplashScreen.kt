package com.example.Pages

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.dothings.R
import com.example.dothings.Screen
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(navController: NavController){
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        // Wait for a certain duration (e.g., 2 seconds) using delay()
        delay(300)

        // Navigate to the sign-in page using the provided NavController
        navController.navigate(Screen.Main.route)
    }
    Box(modifier = Modifier
        .fillMaxSize()
        .background(color = MaterialTheme.colors.background)) {
        ThemedGridImage()
        Box(modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center){
            Image(painter = painterResource(id = R.drawable.shadowcenter), contentDescription = null,
                modifier = Modifier
                    .graphicsLayer(alpha = 0.06f)
                    .blur(radius = 84.dp)
                    .align(Alignment.Center))
        }
        Box(modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
        ) {
            ThemedImage()
        }
    }
}
@Composable
fun ThemedGridImage() {
    val isDarkTheme = isSystemInDarkTheme()
    val imageRes = if (isDarkTheme) {
        R.drawable.dark_grid_lines
    } else {
        R.drawable.light_grid_lines
    }

    Image(
        painter = painterResource(id = imageRes),
        contentDescription = null,
    )
}