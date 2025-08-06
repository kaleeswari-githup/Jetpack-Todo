package com.firstyogi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.navigation.compose.rememberNavController
import com.firstyogi.dothing.AddDaskScreen
import com.firstyogi.ui.theme.AppJetpackComposeTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AddTaskActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val fromWidget = intent.getBooleanExtra("from_widget", false)
        setContent {
            val navController = rememberNavController()
            AppJetpackComposeTheme {
                AddDaskScreen(
                    navController = navController,
                    selectedDate = remember { mutableStateOf(null) },
                    selectedTime = remember { mutableStateOf(null) },
                    textValue = "",
                    isChecked = remember { mutableStateOf(false) },
                    fromWidget = fromWidget,
                    activity = this
                )

            }
        }
    }
}