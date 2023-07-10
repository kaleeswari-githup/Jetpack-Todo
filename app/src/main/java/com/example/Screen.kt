package com.example.dothings

sealed class Screen(val route:String){
    object Splash:Screen("splash_screen")
    object Main:Screen("signin_screen")
    object Home: Screen("home_screen")


}
