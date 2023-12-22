package com.example.Pages

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.net.toUri
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.dothings.MainActivity
import com.example.dothings.R
import com.example.dothings.R.DataClass

import com.example.dothings.Screen
import com.example.dothings.UPDATE_ID_VALUE
import com.example.dothings.uri


import com.google.firebase.auth.FirebaseAuth

//const val notificationID = 1
const val channelID = "channel1"
const val titleExtra = "titleExtra"
const val messageExtra = "messageExtra"

val id = System.currentTimeMillis().toString() + (0..1000).random()
//const val itemId = "itemId"
  val notificationIdsMap = mutableMapOf<String, Int>()

class NotificationReceiver : BroadcastReceiver() {

    val user = FirebaseAuth.getInstance().currentUser
    val email = user?.email

    override fun onReceive(context: Context, intent: Intent) {
        val itemId = intent.getStringExtra("itemId") ?: ""
        val isCheckedStateValue = intent.getBooleanExtra("isCheckedState", false)


        val notificationId = itemId.hashCode()
        notificationIdsMap[itemId] = notificationId
        val notificationTag = "Notification_$itemId"
       Log.d("ItemId","$itemId")
       // val notificationID = itemId.hashCode()

        val notificationBuilder = NotificationCompat.Builder(context, channelID)

        val deepLinkIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://www.example.com/update_screen/$itemId/$isCheckedStateValue"),
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
            val channel = NotificationChannel(
                channelID,
                "1",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)

            notificationBuilder.setSmallIcon(R.drawable.tick_for_notification_icon)
                .setContentText("$email")
                .setContentTitle(intent.getStringExtra(messageExtra))
                .setContentIntent(pendingDeepLinkIntent)
               // .setColor(ContextCompat.getColor(context, R.color.savebtnbg))
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(Notification.DEFAULT_SOUND or Notification.DEFAULT_VIBRATE)
                .build()
        }else{
            notificationBuilder.setSmallIcon(R.drawable.tick_for_notification_icon)
                .setContentText("$email")
                .setContentTitle(intent.getStringExtra(messageExtra))
                .setContentIntent(pendingDeepLinkIntent)
               // .setColor(ContextCompat.getColor(context, R.color.savebtnbg))
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(Notification.DEFAULT_SOUND or Notification.DEFAULT_VIBRATE)
                .build()
        }
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(notificationId, notificationBuilder.build())

    }

}
/*
val channelID = "channel1"
val notificationID = 1
class NotificationWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    @SuppressLint("MissingPermission")
    override suspend fun doWork(): Result {
        val id = System.currentTimeMillis().toString() + (0..1000).random()
        val email = FirebaseAuth.getInstance().currentUser?.email
        val titleExtra = "titleExtra"
        val messageExtra = "messageExtra"

        val notificationBuilder = NotificationCompat.Builder(context, channelID)

        val mainIntent = Intent(context, MainActivity::class.java)
        val pendingMainIntent = PendingIntent.getActivity(
            context,
            0,
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelID,
                "1",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)

            notificationBuilder.setSmallIcon(R.drawable.tick_for_notification_icon)
                .setContentText("$email")
                .setContentTitle(inputData.getString(messageExtra))
                .setContentIntent(pendingMainIntent)
                //  .setSound(RingtoneManager.getDefaultUri(RingtoneManager.getDefaultType(soundUri)))
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build()
        } else {
            notificationBuilder.setSmallIcon(R.drawable.tick_for_notification_icon)
                .setContentText("$email")
                .setContentTitle(inputData.getString(messageExtra))
                .setContentIntent(pendingMainIntent)
                //  .setSound(RingtoneManager.getDefaultUri(RingtoneManager.getDefaultType(soundUri)))
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build()
        }

        val notificationManager = NotificationManagerCompat.from(context)
        notificationManager.cancel(notificationID)
        notificationManager.notify(id.hashCode(), notificationBuilder.build())

        return Result.success()
    }
}*/