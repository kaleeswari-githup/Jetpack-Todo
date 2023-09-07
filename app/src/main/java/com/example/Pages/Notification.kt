package com.example.Pages

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.dothings.MainActivity
import com.example.dothings.R
import com.google.firebase.auth.FirebaseAuth
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
}
