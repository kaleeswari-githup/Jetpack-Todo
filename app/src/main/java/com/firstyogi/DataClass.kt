package com.firstyogi.dothing


import java.security.Timestamp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

data class DataClass(
    var id:String="",
    val message: String?="",
    val time:  String? = "",
    var date:  String? = "",
    var notificationTime:Long = 0,
    val repeatedTaskTime: String? = "",
    var nextDueDate:Long = 0,
    var nextDueDateForCompletedTask:String? = "",
    var formatedDateForWidget:String?="",
    //var needsSync: Boolean = false
    ){
    fun updateFormattedDateForWidget() {
        formatedDateForWidget = if (date!!.isNotBlank()) {
            val parsedDate = LocalDate.parse(date, DateTimeFormatter.ofPattern("MM/dd/yyyy"))
            formatDate(parsedDate)
        } else {
            null
        }
    }

    private fun formatDate(date: LocalDate): String {
        val formatter = DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale.ENGLISH)
        return date.format(formatter)
    }
}




