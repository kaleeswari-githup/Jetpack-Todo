package com.firstyogi.dothing

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat


import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

const val channelID = "channel1"
const val messageExtra = "messageExtra"
fun getNextDueDateNotification(currentDate: String, repeatOption: String): String {
    val formatter = SimpleDateFormat("MM/dd/yyyy", Locale.ENGLISH)
    val date = formatter.parse(currentDate)
    val calendar = Calendar.getInstance()
    calendar.time = date

    when (repeatOption) {
        "Daily" -> calendar.add(Calendar.DAY_OF_YEAR, 1)
        "Weekly" -> calendar.add(Calendar.WEEK_OF_YEAR, 1)
        "Monthly" -> calendar.add(Calendar.MONTH, 1)
        "Yearly" -> calendar.add(Calendar.YEAR, 1)
        else -> return currentDate // No repeat option, return the current date
    }

    return formatter.format(calendar.time)
}


val id = System.currentTimeMillis().toString() + (0..1000).random()
  val notificationIdsMap = mutableMapOf<String, Int>()
fun calculateNextDueDate(currentDate: Long, repeatOption: String): Long {
    val calendar = Calendar.getInstance().apply { timeInMillis = currentDate }
    when (repeatOption) {
        "Daily" -> calendar.add(Calendar.DAY_OF_YEAR, 1)
        "Weekly" -> calendar.add(Calendar.WEEK_OF_YEAR, 1)
        "Monthly" -> calendar.add(Calendar.MONTH, 1)
        "Yearly" -> calendar.add(Calendar.YEAR, 1)
    }
    return calendar.timeInMillis
}
class NotificationReceiver : BroadcastReceiver() {

    val user = FirebaseAuth.getInstance().currentUser
    val email = user?.email
    val database = FirebaseDatabase.getInstance()
    val uid = user?.uid
    override fun onReceive(context: Context, intent: Intent) {
        val itemId = intent.getStringExtra("itemId") ?: ""
        val isCheckedStateValue = intent.getBooleanExtra("isCheckedState", false)

        val notificationId = itemId.hashCode()
        notificationIdsMap[itemId] = notificationId
        Log.d("notificationid","$itemId")
        val notificationBuilder = NotificationCompat.Builder(context, channelID)
        val repeatOption = intent.getStringExtra("repeatOption") ?: ""
        if (repeatOption in listOf("Daily", "Weekly", "Monthly", "Yearly")) {
           // val itemId = intent.getStringExtra("itemId") ?: ""
            val message = intent.getStringExtra("messageExtra") ?: ""
            val isCheckedState = intent.getBooleanExtra("isCheckedState", false)

            val nextNotificationDate = calculateNextDueDate(System.currentTimeMillis(), repeatOption)
           // checkAndUpdateTask(itemId,repeatOption,context)
            scheduleNotification(
                context,
                nextNotificationDate,
                itemId,
                message,
                isCheckedState,
                repeatOption
            )
        }
        val onMarkCompletedClick: (String,String) -> Unit = { clickedTaskId,repeatOption ->
            val taskRef = database.reference.child("Task").child(uid.toString()).child(clickedTaskId)
            val taskNewRef = database.reference.child("Task").child(uid.toString()).push()
            var completedTasksRef = database.reference.child("Task").child("CompletedTasks").child(uid.toString()).child(clickedTaskId)

            taskRef.addListenerForSingleValueEvent(object : ValueEventListener {
                @RequiresApi(Build.VERSION_CODES.O)
                override fun onDataChange(snapshot: DataSnapshot) {
                    val data = snapshot.getValue(DataClass::class.java)

                    if (data != null) {
                      //  updateNextDueDate(data)
                        taskRef.removeValue()
                        completedTasksRef.setValue(data)
                        cancelNotification(context, itemId)
                        cancelNotificationManger(context, itemId)


                    }

                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("FirebaseError", "Database operation cancelled: $error")
                }
            })
        }
        when (intent.action) {
            "MARK_COMPLETED" -> {
                val itemId = intent.getStringExtra("itemId") ?: ""
                val repeatOption = intent.getStringExtra("repeatOption") ?: ""
                // Call your onMarkCompletedClick function
                onMarkCompletedClick(itemId,repeatOption)
            }
            // Handle other actions if needed
        }
        val markCompletedIntent = Intent(context, NotificationReceiver::class.java)
        markCompletedIntent.action = "MARK_COMPLETED"
        markCompletedIntent.putExtra("itemId", itemId)

// Create a PendingIntent for the action
        val pendingMarkCompletedIntent = PendingIntent.getBroadcast(
            context,
            notificationId, // Unique request code
            markCompletedIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val deepLinkIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://firstyogi.page.link/update_screen/$itemId/$isCheckedStateValue"),
            context,
            MainActivity::class.java
        )


        // Create a PendingIntent for the deep link
        val pendingDeepLinkIntent = PendingIntent.getActivity(
            context,
            0,
            deepLinkIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {


            notificationBuilder.setSmallIcon(R.drawable.notification_tick)
                .setContentText("$email")
                .setContentTitle(intent.getStringExtra(messageExtra))
                .setContentIntent(pendingDeepLinkIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(Notification.DEFAULT_SOUND or Notification.DEFAULT_VIBRATE)
                .addAction(0, "Mark as Completed",pendingMarkCompletedIntent)
                .build()
        }else{
            notificationBuilder.setSmallIcon(R.drawable.notification_tick)
                .setContentText("$email")
                .setContentTitle(intent.getStringExtra(messageExtra))
                .setContentIntent(pendingDeepLinkIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(Notification.DEFAULT_SOUND or Notification.DEFAULT_VIBRATE)
                .addAction(0, "Mark as Completed",pendingMarkCompletedIntent)
                .build()
        }
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(notificationId, notificationBuilder.build())

    }

}
/*fun checkAndUpdateTaskAll(taskList: List<DataClass>, context: Context) {
    val dateFormat = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
    val currentTime = System.currentTimeMillis()

    for (data in taskList) {
        val repeatOption = data.repeatedTaskTime ?: continue
      //  val nextDueDateString = data.nextDueDateForCompletedTask ?: continue

        val storedDateInMillis = try {
           // dateFormat.parse(nextDueDateString)?.time ?: 0L
        } catch (e: ParseException) {
            e.printStackTrace()
            0L
        }

        if (storedDateInMillis <= currentTime) {
            updateTaskInFirebase(data, repeatOption, context)
        }
    }
}*/
 fun checkAndUpdateTask(itemId: String, repeatOption: String, context: Context) {
    val user = FirebaseAuth.getInstance().currentUser
    val uid = user?.uid ?: return
    val database = FirebaseDatabase.getInstance()
    val taskRef = database.reference.child("Task").child(uid).child(itemId)

    taskRef.get().addOnSuccessListener { snapshot ->
        val data = snapshot.getValue(DataClass::class.java) ?: return@addOnSuccessListener

        val currentTime = System.currentTimeMillis()
        updateTaskInFirebase(data, repeatOption, context)

    }
}

 fun updateTaskInFirebase(data: DataClass, repeatOption: String, context: Context) {
    val user = FirebaseAuth.getInstance().currentUser
    val uid = user?.uid ?: return
    val database = FirebaseDatabase.getInstance()
    val taskRef = database.reference.child("Task").child(uid).child(data.id)

    val currentTime = System.currentTimeMillis()

    if (!data.startDate.isNullOrBlank() && !repeatOption.isNullOrBlank()) {
        val result = calculateUpdatedDateFromStartDate(data.startDate!!, repeatOption)
        result?.let { (calculatedDate, calculatedMillis) ->

            // Check if existing date is behind the calculated one
            val formatter = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
            val storedDateMillis = try {
                formatter.parse(data.date)?.time ?: 0L
            } catch (e: Exception) {
                0L
            }

            if (storedDateMillis < calculatedMillis) {
                // ✅ Update only if the date is outdated
               // Toast.makeText(context, "Task ${data.id} Updated!", Toast.LENGTH_SHORT).show()

                data.date = calculatedDate
                data.nextDueDate = calculateNextDueDate(calculatedMillis, repeatOption)
                data.nextDueDateForCompletedTask = formatter.format(Date(data.nextDueDate!!))

                taskRef.setValue(data)
                val baseDateMillis = calculatedMillis // This is from your calculated new date (e.g., today)
                val originalNotificationTime = data.notificationTime ?: 0L

                val calendar = Calendar.getInstance().apply {
                    timeInMillis = baseDateMillis

                    val notifTimeCal = Calendar.getInstance().apply {
                        timeInMillis = originalNotificationTime
                    }

                    // Set hour & minute from original notificationTime
                    set(Calendar.HOUR_OF_DAY, notifTimeCal.get(Calendar.HOUR_OF_DAY))
                    set(Calendar.MINUTE, notifTimeCal.get(Calendar.MINUTE))
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)

                    // If that time is already passed today, move to the next valid day
                    if (timeInMillis < System.currentTimeMillis()) {
                        when (repeatOption.lowercase()) {
                            "daily" -> add(Calendar.DAY_OF_YEAR, 1)
                            "weekly" -> add(Calendar.WEEK_OF_YEAR, 1)
                            "monthly" -> add(Calendar.MONTH, 1)
                            "yearly" -> add(Calendar.YEAR, 1)
                        }
                    }
                }

                val newNotificationTime = calendar.timeInMillis
                data.notificationTime = newNotificationTime // update Firebase model if needed

                scheduleNotification(
                    context,
                    newNotificationTime,
                    data.id,
                    data.message ?: "",
                    false,
                    repeatOption
                )


            } else {
                Log.d("UpdateCheck", "Task ${data.id} is already up to date.")
            }
        }
    }
}
fun calculateUpdatedDateFromStartDate(startDateString: String, repeatOption: String): Pair<String, Long>? {
    return try {
        val formatter = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
        val startDate = formatter.parse(startDateString) ?: return null

        val startCalendar = Calendar.getInstance().apply {
            time = startDate
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val todayCalendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // Repeat until we go *past* today
        while (!startCalendar.after(todayCalendar)) {
            when (repeatOption.lowercase(Locale.ENGLISH)) {
                "daily" -> startCalendar.add(Calendar.DAY_OF_YEAR, 1)
                "weekly" -> startCalendar.add(Calendar.WEEK_OF_YEAR, 1)
                "monthly" -> startCalendar.add(Calendar.MONTH, 1)
                "yearly" -> startCalendar.add(Calendar.YEAR, 1)
                else -> break
            }
        }

        // Step back one repeat to get the latest valid date ≤ today
        when (repeatOption.lowercase(Locale.ENGLISH)) {
            "daily" -> startCalendar.add(Calendar.DAY_OF_YEAR, -1)
            "weekly" -> startCalendar.add(Calendar.WEEK_OF_YEAR, -1)
            "monthly" -> startCalendar.add(Calendar.MONTH, -1)
            "yearly" -> startCalendar.add(Calendar.YEAR, -1)
        }

        val updatedDateString = formatter.format(startCalendar.time)
        val updatedMillis = startCalendar.timeInMillis
        Pair(updatedDateString, updatedMillis)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
fun updateCompletedTaskInFirebase(data: DataClass) {
    val user = FirebaseAuth.getInstance().currentUser
    val uid = user?.uid ?: return
    val database = FirebaseDatabase.getInstance()
    val completedTaskRef = database.reference.child("Task").child("CompletedTasks").child(uid).child(data.id)

    if (!data.startDate.isNullOrBlank() && !data.repeatedTaskTime.isNullOrBlank()) {
        val result = calculateUpdatedDateFromStartDate(data.startDate!!, data.repeatedTaskTime!!)
        result?.let { (calculatedDate, calculatedMillis) ->

            val formatter = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
            val storedDateMillis = try {
                formatter.parse(data.date)?.time ?: 0L
            } catch (e: Exception) {
                0L
            }

            if (storedDateMillis < calculatedMillis) {
                data.date = calculatedDate
                data.nextDueDate = calculateNextDueDate(calculatedMillis, data.repeatedTaskTime!!)
                data.nextDueDateForCompletedTask = formatter.format(Date(data.nextDueDate!!))

                completedTaskRef.setValue(data)
            } else {
                Log.d("CompletedTaskUpdate", "Completed Task ${data.id} is already up to date.")
            }
        }
    }
}
fun calculateNextNotificationTime(originalNotificationTime: Long, newNextDueDate: Long): Long {
    val originalCalendar = Calendar.getInstance().apply { timeInMillis = originalNotificationTime }
    val newCalendar = Calendar.getInstance().apply { timeInMillis = newNextDueDate }

    // Set the time of the new calendar to match the original notification time
    newCalendar.set(Calendar.HOUR_OF_DAY, originalCalendar.get(Calendar.HOUR_OF_DAY))
    newCalendar.set(Calendar.MINUTE, originalCalendar.get(Calendar.MINUTE))
    newCalendar.set(Calendar.SECOND, 0)
    newCalendar.set(Calendar.MILLISECOND, 0)

    return newCalendar.timeInMillis
}


