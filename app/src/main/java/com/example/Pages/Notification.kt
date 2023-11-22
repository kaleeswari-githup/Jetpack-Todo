package com.example.Pages

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
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

 const val notificationID = 1
const val channelID = "channel1"
const val titleExtra = "titleExtra"
const val messageExtra = "messageExtra"

class NotificationReceiver : BroadcastReceiver() {
    var counter = 0
    val user = FirebaseAuth.getInstance().currentUser
    val email = user?.email
    override fun onReceive(context: Context, intent: Intent) {
        val id = System.currentTimeMillis().toString() + (0..1000).random()
        counter++

        val notificationBuilder = NotificationCompat.Builder(context, channelID)


        val mainIntent = Intent(context,MainActivity::class.java)
        val pendingMainIntent = if(Build.VERSION.SDK_INT >=23){
            PendingIntent.getActivity(context,
                0 ,
                mainIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }else{
            PendingIntent.getActivity(context,
                0 ,
                mainIntent,
                PendingIntent.FLAG_UPDATE_CURRENT )
        }
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
                .setContentIntent(pendingMainIntent)
               // .setColor(ContextCompat.getColor(context, R.color.savebtnbg))
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(Notification.DEFAULT_SOUND or Notification.DEFAULT_VIBRATE)
                .build()
        }else{
            notificationBuilder.setSmallIcon(R.drawable.tick_for_notification_icon)
                .setContentText("$email")
                .setContentTitle(intent.getStringExtra(messageExtra))
                .setContentIntent(pendingMainIntent)
               // .setColor(ContextCompat.getColor(context, R.color.savebtnbg))
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(Notification.DEFAULT_SOUND or Notification.DEFAULT_VIBRATE)
                .build()
        }



        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(notificationID)
        manager.notify(id.hashCode(), notificationBuilder.build())
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