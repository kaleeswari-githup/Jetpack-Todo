package com.firstyogi.dothing

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.fillMaxSize

import androidx.compose.material.SnackbarHostState
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
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
@OptIn(ExperimentalSharedTransitionApi::class)
@SuppressLint("SuspiciousIndentation")
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun SetupNavGraph(activity: Activity){
    val navController = rememberNavController()

    val coroutineScope = rememberCoroutineScope()
    val homesnackbarHostState = remember { SnackbarHostState() }
    val completedsnackbarHostState = remember { SnackbarHostState() }
SharedTransitionLayout {
    val currentRoute = navController.currentBackStackEntry?.destination?.route


    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
    ) {
        composable(
            route = Screen.Home.route,

            ) { navBackStackEntry ->

            HomeScreen(
                animatedVisibilityScope = this,
                navController,
                homesnackbarHostState,
                coroutineScope,
                sharedTransitionScope = this@SharedTransitionLayout,
                modifier = Modifier
            )
        }
        composable(
            Screen.Update.route,

            arguments = listOf(
                navArgument(UPDATE_ID_VALUE) {
                    type = NavType.StringType
                },
                navArgument("isCheckedState") {
                    type = NavType.BoolType
                }
            ),
            deepLinks = listOf(navDeepLink {
                uriPattern = "$uri/update_screen/{$UPDATE_ID_VALUE}/{isCheckedState}"
            })

        ) { backStackEntry ->

            val isCheckedState = rememberSaveable {
                mutableStateOf(backStackEntry.arguments?.getBoolean("isCheckedState") ?: false)
            }
            UpdateTaskScreen(

                navController = navController,
                id = backStackEntry.arguments?.getString(UPDATE_ID_VALUE),
                openKeyboard = false,
                isChecked = isCheckedState,
                homesnackbarHostState,
                coroutineScope,
                animatedVisibilityScope = this,
                sharedTransitionScope = this@SharedTransitionLayout
            )
        }

        composable(
            "Screen.AddDask.route/{isCheckedState}",

            arguments = listOf(
                navArgument("isCheckedState") {
                    type = NavType.BoolType
                }
            )) { backStackEntry ->
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
                isChecked = isCheckedState,
fromWidget = false,
                activity = activity

            )
        }
        composable(
            "Screen.MarkComplete.route/{isMarkCheckedState}",

            arguments = listOf(
                navArgument("isMarkCheckedState") {
                    type = NavType.BoolType
                }
            )
        ) { backStackEntry ->
            val isCheckedState = rememberSaveable {
                mutableStateOf(backStackEntry.arguments?.getBoolean("isMarkCheckedState") ?: false)
            }
            MarkCompletedScreen(
                navController = navController,
                isChecked = isCheckedState,
                sharedTransitionScope = this@SharedTransitionLayout,
                animatedVisibilityScope = this,
                snackbarHostState = completedsnackbarHostState
            )
        }
        composable(
            Screen.UnMarkCompleted.route,
            arguments = listOf(
                navArgument(UPDATE_ID_VALUE) {
                    type = NavType.StringType
                },
                navArgument("isCheckedState") {
                    type = NavType.BoolType
                }
            )) { backStackEntry ->
            val isCheckedState = rememberSaveable {
                mutableStateOf(backStackEntry.arguments?.getBoolean("isCheckedState") ?: false)
            }
            UnMarkCompletedTaskScreen(
                snackbarHostState = completedsnackbarHostState,
                id = backStackEntry.arguments?.getString(UPDATE_ID_VALUE),
                openKeyboard = false,
                isChecked = isCheckedState,
                animatedVisibilityScope = this,
                sharedTransitionScope = this@SharedTransitionLayout,
                navController = navController
            )
        }


    }


}

}