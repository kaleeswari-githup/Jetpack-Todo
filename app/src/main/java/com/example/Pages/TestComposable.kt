package com.example.Pages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@Composable
fun  TextComposable(navController: NavController,text:String){
   Box(modifier = Modifier
        .fillMaxSize()
        .background(color = MaterialTheme.colors.background),
contentAlignment = Alignment.Center
    ){
        Box(modifier = Modifier
            .size(300.dp)
            .background(color = MaterialTheme.colors.secondary, shape = CircleShape),
            contentAlignment = Alignment.Center){
           Text(text = text,
               fontSize = 24.sp,
            color = Color.White)
        }
    }
}