package com.example.dothings

sealed class Screen(val route:String){
    object Home: Screen("home_screen")
    object AddNewTask: Screen("add_new_task")

}
