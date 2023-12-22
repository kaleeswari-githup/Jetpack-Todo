package com.example.dothings

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.ui.theme.AppJetpackComposeTheme
import com.google.accompanist.systemuicontroller.rememberSystemUiController


class MainActivity : ComponentActivity() {
    lateinit var navController: NavHostController

    @SuppressLint("SuspiciousIndentation")
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {

            AppJetpackComposeTheme {
               // SetStatusBarColor(color = MaterialTheme.colorScheme.background)
                val systemUiController = rememberSystemUiController()
                Log.d("StatusBarColor", "Color: ${MaterialTheme.colorScheme.background}")

                // Set the status bar color

                MyMainApp()
            }
        }
    }
   /* override fun onBackPressed() {

            if (navController.currentBackStackEntry?.destination?.route == Screen.Home.route) {
                // Exit the app if back button is pressed on screen A
                finish()
            } else {
                super.onBackPressed()
            }


    }*/
}


@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun MyMainApp() {


    val navController = rememberNavController()
    SetupNavGraph(navController = navController)
    // HomeScreen(navController = navController)
}


@Composable
fun ThemedBackground() {
    val isDarkTheme = isSystemInDarkTheme()
    if (isDarkTheme) {
        (LocalView.current.parent as DialogWindowProvider)?.window?.setDimAmount(0.1f)
    } else {
        (LocalView.current.parent as DialogWindowProvider)?.window?.setDimAmount(0.1f)
    }


}



