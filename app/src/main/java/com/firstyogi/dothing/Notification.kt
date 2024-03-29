package com.firstyogi.dothing

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat


import com.google.firebase.auth.FirebaseAuth

const val channelID = "channel1"
const val messageExtra = "messageExtra"

val id = System.currentTimeMillis().toString() + (0..1000).random()
  val notificationIdsMap = mutableMapOf<String, Int>()

class NotificationReceiver : BroadcastReceiver() {

    val user = FirebaseAuth.getInstance().currentUser
    val email = user?.email

    override fun onReceive(context: Context, intent: Intent) {
        val itemId = intent.getStringExtra("itemId") ?: ""
        val isCheckedStateValue = intent.getBooleanExtra("isCheckedState", false)

        val notificationId = itemId.hashCode()
        notificationIdsMap[itemId] = notificationId

        val notificationBuilder = NotificationCompat.Builder(context, channelID)

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
                .build()
        }else{
            notificationBuilder.setSmallIcon(R.drawable.notification_tick)
                .setContentText("$email")
                .setContentTitle(intent.getStringExtra(messageExtra))
                .setContentIntent(pendingDeepLinkIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(Notification.DEFAULT_SOUND or Notification.DEFAULT_VIBRATE)
                .build()
        }
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(notificationId, notificationBuilder.build())

    }

}
