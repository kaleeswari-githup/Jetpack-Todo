package com.firstyogi.dothing

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.WorkManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class NotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "TaskReminderChannel"
        const val CHANNEL_NAME = "Task Reminders"
        const val CHANNEL_DESCRIPTION = "Notifications for task reminders"
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESCRIPTION
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    @SuppressLint("MissingPermission")
    fun showNotification(itemId: String, message: String, isCheckedState: Boolean) {
        val notificationId = itemId.hashCode()

        // Create an Intent for deep linking
        val deepLinkIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://firstyogi.page.link/update_screen/$itemId/$isCheckedState"),
            context,
            MainActivity::class.java
        )
        val pendingDeepLinkIntent = PendingIntent.getActivity(
            context,
            0,
            deepLinkIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Create an Intent for "Mark as Completed" action
        val markCompletedIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = "MARK_COMPLETED"
            putExtra("itemId", itemId)
        }
        val pendingMarkCompletedIntent = PendingIntent.getBroadcast(
            context,
            notificationId,
            markCompletedIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Build the notification
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.notification_tick)
            .setContentTitle(message)
            .setContentText(FirebaseAuth.getInstance().currentUser?.email ?: "Task Reminder")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingDeepLinkIntent)
            .setAutoCancel(true)
            .addAction(0, "Mark as Completed", pendingMarkCompletedIntent)

        // Show the notification
        with(NotificationManagerCompat.from(context)) {
            notify(notificationId, builder.build())
        }
    }
}

// This receiver handles the "Mark as Completed" action
class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "MARK_COMPLETED") {
            val itemId = intent.getStringExtra("itemId") ?: return
            // Handle marking the task as completed
            markTaskAsCompleted(itemId,context)
        }
    }

    private fun markTaskAsCompleted(itemId: String,context: Context) {
        val user = FirebaseAuth.getInstance().currentUser
        val uid = user?.uid ?: return
        val database = FirebaseDatabase.getInstance()

        val taskRef = database.reference.child("Task").child(uid).child(itemId)
        val completedTasksRef = database.reference.child("Task").child("CompletedTasks").child(uid).push()

        taskRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val data = snapshot.getValue(DataClass::class.java)
                if (data != null) {
                    // Move the task to completed tasks
                    completedTasksRef.setValue(data)
                    // Remove the task from active tasks
                    taskRef.removeValue()
                    // Cancel any pending notifications for this task
                    cancelNotificationAndAlarms(itemId,context)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseError", "Database operation cancelled: $error")
            }
        })
    }

    private fun cancelNotificationAndAlarms(itemId: String,context: Context) {

        // Cancel the notification
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(itemId.hashCode())

        // Cancel any pending WorkManager tasks
        WorkManager.getInstance(context).cancelAllWorkByTag(itemId)
    }
}