package com.firstyogi.dothing

import android.annotation.SuppressLint
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.material.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.firstyogi.dothing.*
import java.util.*
const val DEEP_LINK_UPDATE_TASK = "updateTask/{date}/{time}/{message}/{id}"
val uri = "https://firstyogi.page.link"
@OptIn(ExperimentalAnimationApi::class)
@SuppressLint("SuspiciousIndentation")
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun SetupNavGraph(){
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        ){
        composable(route = Screen.Home.route,
enterTransition = {
    EnterTransition.None
},
            exitTransition = {
                ExitTransition.None
            },
            popEnterTransition = {
                EnterTransition.None
            },
            popExitTransition = {
                ExitTransition.None
            }
            ){navBackStackEntry ->
            HomeScreen( navController,snackbarHostState,coroutineScope)
        }

        composable("Screen.AddDask.route/{isCheckedState}",
            enterTransition = {
                EnterTransition.None
            },
            exitTransition = {
                ExitTransition.None
            },
            popEnterTransition = {
                EnterTransition.None
            },
            popExitTransition = {
                ExitTransition.None
            },
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
            enterTransition = {
                EnterTransition.None
            },
            exitTransition = {
                ExitTransition.None
            },
            popEnterTransition = {
                EnterTransition.None
            },
            popExitTransition = {
                ExitTransition.None
            },
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
            enterTransition = {
                EnterTransition.None
            },
            exitTransition = {
                ExitTransition.None
            },
            popEnterTransition = {
                EnterTransition.None
            },
            popExitTransition = {
                ExitTransition.None
            },
            arguments = listOf(
                navArgument(UPDATE_ID_VALUE){
                    type = NavType.StringType
                },
                navArgument("isCheckedState") {
                    type = NavType.BoolType
                }
                ),
            deepLinks = listOf(navDeepLink { uriPattern = "$uri/update_screen/{$UPDATE_ID_VALUE}/{isCheckedState}" })

        ){backStackEntry->
            val isCheckedState = rememberSaveable {
                mutableStateOf(backStackEntry.arguments?.getBoolean("isCheckedState") ?: false)
            }
            UpdateTaskScreen(
                    navController = navController,
                    id = backStackEntry.arguments?.getString(UPDATE_ID_VALUE),
                    openKeyboard = false,
                    isChecked = isCheckedState,
                    snackbarHostState,
                    coroutineScope
                )
            }
    }
}