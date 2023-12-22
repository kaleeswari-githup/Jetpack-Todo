package com.example.dothings


const val UPDATE_ID_VALUE = "id"


sealed class Screen(val route:String){

    object Home: Screen("home_screen")

    object AddDask:Screen("adddask_screen")
    object MarkComplete:Screen("mark_complete")
    object Update:Screen("update_screen/{$UPDATE_ID_VALUE}/{isCheckedState}"){
        fun passUpdateValues(
            id:String,
            isChecked:Boolean

        ):String=
             "update_screen/${id}/${isChecked}"
    }








}
