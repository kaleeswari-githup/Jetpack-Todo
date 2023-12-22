package com.example.dothings

import android.annotation.SuppressLint
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navArgument
import androidx.navigation.navDeepLink
import com.example.Pages.*
import java.util.*
const val DEEP_LINK_UPDATE_TASK = "updateTask/{date}/{time}/{message}/{id}"
val uri = "https://www.example.com"
@SuppressLint("SuspiciousIndentation")
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun SetupNavGraph(navController: NavHostController){

    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        ){


        composable(route = Screen.Home.route){
            var visible by remember {
                mutableStateOf(false)
            }
            LaunchedEffect(visible) {
                visible = true
            }
            val random = Random(System.currentTimeMillis())
            val randomDelay = random.nextInt(300)
            val offsetY by animateDpAsState(
                targetValue = if (visible) 0.dp else 32.dp,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessVeryLow
                ),

                )
            val scale by animateFloatAsState(
                targetValue = if (visible) 1f else 0f,
                animationSpec = keyframes {
                    durationMillis = 0


                }
            )


            HomeScreen( navController,scale,offsetY)
        }

        composable("Screen.AddDask.route/{isCheckedState}",
            arguments = listOf(
                navArgument("isCheckedState") {
                    type = NavType.BoolType
                }
            )){backStackEntry ->
            val isCheckedState = rememberSaveable {
                mutableStateOf(backStackEntry.arguments?.getBoolean("isCheckedState") ?: false)
            }
            AddDaskScreen(
                navController = navController,
                selectedDate = remember {
                    mutableStateOf(null)
                },
                selectedTime = remember {
                    mutableStateOf(null)
                },
                textValue = "",
                isChecked = isCheckedState

            )
        }
        composable("Screen.MarkComplete.route/{isMarkCheckedState}",
            arguments = listOf(
                navArgument("isMarkCheckedState") {
                    type = NavType.BoolType
                }
            )
        ){backStackEntry ->
            val isCheckedState = rememberSaveable {
                mutableStateOf(backStackEntry.arguments?.getBoolean("isMarkCheckedState") ?: false)
            }
            MarkCompletedScreen(
                navController = navController,
                isChecked = isCheckedState,
            )
        }
        composable(Screen.Update.route,
            arguments = listOf(
                navArgument(UPDATE_ID_VALUE){
                    type = NavType.StringType
                },
                navArgument("isCheckedState") {
                    type = NavType.BoolType
                },


        ),
                deepLinks = listOf(navDeepLink { uriPattern = "$uri/update_screen/{$UPDATE_ID_VALUE}/{isCheckedState}" })

        ){backStackEntry->
            val isCheckedState = rememberSaveable {
                mutableStateOf(backStackEntry.arguments?.getBoolean("isCheckedState") ?: false)
            }

            val onDeleteClick = backStackEntry.arguments?.getString(("onDeleteClick"))
                UpdateTaskScreen(
                    navController = navController,
                    id = backStackEntry.arguments?.getString(UPDATE_ID_VALUE),
                    openKeyboard = false,
                    isChecked = isCheckedState,


                )
            }

      /*  composable(Screen.Test.route,
            arguments = listOf(navArgument(DETAIL_ARGUMENT_KEY){
                type = NavType.StringType
            }),
            deepLinks = listOf(navDeepLink { uriPattern = "$uri/$DETAIL_ARGUMENT_KEY = {$DETAIL_ARGUMENT_KEY}" })
        ){ backStackEntry ->
           TextComposable(navController = navController,
                backStackEntry.arguments?.getString(DETAIL_ARGUMENT_KEY).toString()
            )
        }*/

        /*composable(route = "update_screen/{date}/{time}/{message}/{id}"){navBackstackEntry ->
            val selectedDate= navBackstackEntry.arguments?.getString("date")?:""
            val selectedTime= navBackstackEntry.arguments?.getString("time")?:""
            val textValue= navBackstackEntry.arguments?.getString("message")?:""
            val userId= navBackstackEntry.arguments?.getString("id")?:""
            UpdateTaskScreen(
                navController = navController,
                selectedDate = remember {
                    mutableStateOf(selectedDate)
                },
                selectedTime = remember {
                    mutableStateOf(selectedTime)
                },
                textValue = textValue,
                id = userId,
                openKeyboard = false
            )
        }*/
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