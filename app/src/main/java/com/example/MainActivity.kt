package com.example.dothings

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController

class MainActivity : ComponentActivity() {
    lateinit var navController: NavHostController

    @SuppressLint("SuspiciousIndentation")
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {

                MyMainApp()
//HomeScreen(navController = navController)

        }
    }
    override fun onBackPressed() {
        if (navController.currentBackStackEntry?.destination?.route == Screen.Home.route) {
            // Exit the app if back button is pressed on screen A
            finish()
        } else {
            super.onBackPressed()
        }
    }
}
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun MyMainApp() {
    val navController = rememberNavController()
    SetupNavGraph(navController = navController)
    // HomeScreen(navController = navController)
}






