package com.firstyogi.dothing

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.firstyogi.ui.theme.AppJetpackComposeTheme
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit


class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("SuspiciousIndentation")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AppJetpackComposeTheme{
                enableEdgeToEdge()
                val context = this
               /* val myWorkRequest = OneTimeWorkRequestBuilder<MyWorker>()
                    .setInitialDelay(10, TimeUnit.MINUTES) // Delay execution if needed
                    .build()
                WorkManager.getInstance(context).enqueue(myWorkRequest)*/
         schedulePeriodicWorkManager(context)
               PeriodicTaskUpdater.enqueue(this)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    channelID,
                    "Tasks",
                    NotificationManager.IMPORTANCE_DEFAULT
                )
                val manager: NotificationManager =
                    context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                manager.createNotificationChannel(channel)}

                SetupNavGraph()
                val currentUser = FirebaseAuth.getInstance().currentUser
                val uid = currentUser?.uid
                LaunchedEffect(Unit) {
                    if (uid != null){
                        migrateCompletedTaskIds(uid = currentUser.uid, context = context)
                    }

                }

            }



        }


    }
    override fun onStop() {
        super.onStop()
        runConditionOnAppClose()
    }
    private fun runConditionOnAppClose() {

        // ... your condition logic
    }
    private fun schedulePeriodicWorkManager(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .build()

        val periodicWorkRequest = PeriodicWorkRequestBuilder<MyWorker>(
            15, TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "CheckCompletedTasks",
            ExistingPeriodicWorkPolicy.REPLACE,
            periodicWorkRequest
        )
      /*  val workRequest = PeriodicWorkRequestBuilder<MyWorker>(15, TimeUnit.MINUTES)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.NOT_REQUIRED) // Ensure network is available
                    .setRequiresBatteryNotLow(true) // Prevent execution if battery is low
                    .build()
            )
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "CheckCompletedTasks",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )*/
    }


}
fun migrateCompletedTaskIds(uid: String, context: Context) {
    val prefs = context.getSharedPreferences("prefs", Context.MODE_PRIVATE)
    val alreadyMigrated = prefs.getBoolean("completed_tasks_migrated", false)
    if (alreadyMigrated) return

    val completedRef = FirebaseDatabase.getInstance()
        .getReference("Task/CompletedTasks/$uid")

    completedRef.get().addOnSuccessListener { snapshot ->
        for (child in snapshot.children) {
            val task = child.getValue(DataClass::class.java)
            val pushedKey = child.key

            if (task != null && pushedKey != task.id && !task.id.isNullOrEmpty()) {
                // Move data to correct ID path
                val correctRef = completedRef.child(task.id)

                // 1. Write to correct ID
                correctRef.setValue(task).addOnSuccessListener {
                    // 2. Remove the wrongly pushed node
                    completedRef.child(pushedKey!!).removeValue()
                }
            }
        }

        prefs.edit().putBoolean("completed_tasks_migrated", true).apply()
    }
}

@Composable
fun ThemedBackground() {
    val isDarkTheme = isSystemInDarkTheme()
    if (isDarkTheme) {
        (LocalView.current.parent as DialogWindowProvider)?.window?.setDimAmount(0.1f)
    } else {
        (LocalView.current.parent as DialogWindowProvider)?.window?.setDimAmount(0.1f)
    }
}



