package com.example.Pages

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.navigation.NavController
import com.example.dothings.R
import com.example.dothings.Screen
import com.example.ui.theme.SurfaceGray
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(navController: NavController){
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        // Wait for a certain duration (e.g., 2 seconds) using delay()
        delay(1000)

        // Navigate to the sign-in page using the provided NavController
        navController.navigate(Screen.Main.route)
    }
    Box(modifier = Modifier.fillMaxSize()
        .background(color = SurfaceGray)) {
        Image(painter = painterResource(id = R.drawable.grid_lines), contentDescription = null)
        Box(modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
        ) {
            Image(painter = painterResource(id = R.drawable.todo_icon_ball), contentDescription = "")
        }
    }
}