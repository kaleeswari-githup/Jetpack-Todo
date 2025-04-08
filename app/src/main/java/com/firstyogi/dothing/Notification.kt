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

        val notificationBuilder = NotificationCompat.Builder(context, channelID)
        val repeatOption = intent.getStringExtra("repeatOption") ?: ""
        if (repeatOption in listOf("Daily", "Weekly", "Monthly", "Yearly")) {
            val itemId = intent.getStringExtra("itemId") ?: ""
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
                        cancelNotification(context, data.id)
                        cancelNotificationManger(context, data.id)


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

private fun updateTaskInFirebase(data: DataClass, repeatOption: String, context: Context) {
    val user = FirebaseAuth.getInstance().currentUser
    val uid = user?.uid ?: return
    val database = FirebaseDatabase.getInstance()
    val taskRef = database.reference.child("Task").child(uid).child(data.id)

    // Calculate the new next due date
    val newNextDueDate = data.nextDueDate?.let {
        val calculatedNextDueDate = calculateNextDueDate(it, repeatOption)

        // Only update if the calculated date is in the future
        if (calculatedNextDueDate > System.currentTimeMillis()) {
            calculatedNextDueDate
        } else {
            it // Keep the existing date if it’s already correct
        }
    }

    data.apply {
        // Update date only if nextDueDate is not null
        date = newNextDueDate?.let { SimpleDateFormat("MM/dd/yyyy", Locale.ENGLISH).format(Date(it)) }
        nextDueDate = newNextDueDate
        nextDueDateForCompletedTask = newNextDueDate?.let {
            SimpleDateFormat("MM/dd/yyyy", Locale.getDefault()).format(Date(it))
        }
    }

    // Save the updated data back to Firebase
    taskRef.setValue(data).addOnSuccessListener {
        // Schedule the next notification
        //val nextNotificationTime = calculateNextNotificationTime(data.notificationTime!!, newNextDueDate)
        scheduleNotification(
            context,
            data.notificationTime!!, // Use the original notification time
            data.id,
            data.message ?: "",
            false, // Assuming isCheckedState should be false for a new cycle
            repeatOption
        )
       // updateWidget( context)
       // Log.d("NotificationCheck", "Scheduled notification for task ${data.id} at ${Date(newNextDueDate)}")

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


