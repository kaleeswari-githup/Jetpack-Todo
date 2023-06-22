package com.example.dothings

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.Pages.HomeScreen

class MainActivity : ComponentActivity() {
    lateinit var navController: NavHostController
    @SuppressLint("SuspiciousIndentation")
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
           // CalendarAndTimePickerScreen()
          /*  Calendar(

                selectedDate = remember {
                    mutableStateOf(LocalDate.now())
                },
                onDateSelected = {},
                selectedTime = remember {
                    mutableStateOf(LocalTime.now())
                }
            )*/
              navController = rememberNavController()
                SetupNavGraph(navController = navController)
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
@Preview(showBackground = true)
fun SomePreview(){
      HomeScreen(navController = rememberNavController() )
}



