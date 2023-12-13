package com.example.dothings

import androidx.compose.runtime.MutableState


const val UPDATE_ID_VALUE = "id"


sealed class Screen(val route:String){

    object Home: Screen("home_screen")
    object Update:Screen("update_screen/{$UPDATE_ID_VALUE}"){
        fun passUpdateValues(id:String)=
             "update_screen/$id"
    }





}
