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
    var nextDueDate:Long? = null,
    var nextDueDateForCompletedTask:String? = "",
    var formatedDateForWidget:String?="",
    var startDate:String? = ""
    //var needsSync: Boolean = false
    )
fun parseDataClassFromSnapshot(map: Map<*, *>, id: String): DataClass {
    return DataClass(
        id = id,
        message = map["message"] as? String ?: "",
        time = map["time"] as? String ?: "",
        date = map["date"] as? String ?: "",
        notificationTime = when (val nt = map["notificationTime"]) {
            is Long -> nt
            is String -> nt.toLongOrNull() ?: 0L
            else -> 0L
        },
        repeatedTaskTime = map["repeatedTaskTime"] as? String ?: "",
        nextDueDate = when (val nd = map["nextDueDate"]) {
            is Long -> nd
            is String -> nd.toLongOrNull()
            else -> null
        },
       // nextDueDateForCompletedTask = map["nextDueDateForCompletedTask"] as? String ?: "",
        formatedDateForWidget = map["formatedDateForWidget"] as? String ?: ""
    )
}



