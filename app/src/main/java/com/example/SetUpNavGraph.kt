package com.example.dothings

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.Pages.*
import java.util.*

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun SetupNavGraph(navController: NavHostController){

    NavHost(
        navController = navController,
        startDestination = Screen.Home.route ){
        composable(route = Screen.Home.route){
            HomeScreen( navController)
        }
       /* composable(route = Screen.AddNewTask.route ){

            AddDaskScreen(navController,selectedDate = null ,selectedTime = null, textValue = "")

        }
        composable(route = "${Screen.CalendarScreen.route}/{textValue}"){navBackStack ->
            val textValue = navBackStack.arguments?.getString("textValue")
            val formattedTextValue = textValue.orEmpty()
            CalendarAndTimePickerScreen(navController = navController,formattedTextValue)
        }
        composable(route = "${Screen.AddNewTask.route}/{textValue}" ){navBackStack ->
            val textValue = navBackStack.arguments?.getString("textValue")
            AddDaskScreen(navController,selectedDate = null ,selectedTime = null,textValue!! )
        }
        composable(route = "${Screen.AddNewTask.route}/{selectedDate}/{selectedTime}/{textValue}"){navBackStack ->
            val textValue = navBackStack.arguments?.getString("textValue")
            val selectedDateArg = navBackStack.arguments?.getString("selectedDate")
            val selectedTimeArg = navBackStack.arguments?.getString("selectedTime")
            val selectedDate = selectedDateArg?.let { LocalDate.parse(it)}
            val selectedTime = remember {
                mutableStateOf(selectedTimeArg?.takeUnless { it == "null" }?.let { LocalTime.parse(it) })
            }
            AddDaskScreen(navController, selectedDate,selectedTime.value,textValue!!)
        }
        composable(route = "${Screen.UpdatePageScreen.route}/{textValue}/{date}/{time}/{id}"){ navBackStack ->
            val textValue = navBackStack.arguments?.getString("textValue")
            val date = navBackStack.arguments?.getString("date")
            val time = navBackStack.arguments?.getString("time")
            val id = navBackStack.arguments?.getString("id")

            UpdateTaskScreen(navController = navController,
                textValue = textValue!!,
                selectedDate = date!!,
                selectedTime = time!!,
                id = id!!,
            openKeyboard = false)
        }
        composable(route = "${Screen.UpdatedCalendarScreen.route}/{textValue}/{id}/{date}/{time}") { navBackStack ->
            val textValue = navBackStack.arguments?.getString("textValue")
            val date = navBackStack.arguments?.getString("date")
            val time = navBackStack.arguments?.getString("time")
            val id = navBackStack.arguments?.getString("id")
            UpdatedCalendarAndTimePickerScreen(navController = navController, textValue = textValue!!, id = id ,date = date,time = time)

        }
        composable(route = "${Screen.UpdatedFromPageScreen.route}/{textValue}/{id}/{selectedDate}/{selectedTime}"){navBackStack ->
            val textValue = navBackStack.arguments?.getString("textValue")
            val selectedDateArg = navBackStack.arguments?.getString("selectedDate")
            val selectedTimeArg = navBackStack.arguments?.getString("selectedTime")
            val id = navBackStack.arguments?.getString("id")

            UpdateTaskScreen(
                navController = navController,
                selectedDate = selectedDateArg,
                selectedTime = selectedTimeArg,
                textValue = textValue!!,
                id = id!!,
                openKeyboard = true
            )

        }
        composable(route = "${Screen.UpdatedFromPageScreen.route}/{textValue}/{id}/{date}/{time}"){navBackStack ->
            val id = navBackStack.arguments?.getString("id")
            val date = navBackStack.arguments?.getString("date")
            val time = navBackStack.arguments?.getString("time")
            val textValue = navBackStack.arguments?.getString("textValue")
            UpdateTaskScreen(
                navController = navController,
                selectedDate = date,
                selectedTime = time ,
                textValue = textValue!!,
                id = id!!,
                openKeyboard = true
            )
        }*/

    }


}