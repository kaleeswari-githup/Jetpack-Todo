package com.firstyogi.dothing

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class PeriodicTaskUpdater(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        Log.d("PeriodicTaskUpdater", "Starting doWork")
        val database = FirebaseDatabase.getInstance()
        val user = FirebaseAuth.getInstance().currentUser
        val uid = user?.uid ?: return Result.failure()
        val tasksRef = database.reference.child("Task").child(uid)

        try {
            val snapshot = tasksRef.get().await()
            val currentTime = System.currentTimeMillis()
            Log.d("PeriodicTaskUpdater", "Current time: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(currentTime))}")

            for (childSnapshot in snapshot.children) {
                val task = childSnapshot.getValue(DataClass::class.java) ?: continue
                Log.d("PeriodicTaskUpdater", "Checking task: ${task.id}, Next due date: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(task.nextDueDate))}")

                if (currentTime >= task.nextDueDate) {
                    Log.d("PeriodicTaskUpdater", "Updating task: ${task.id}")
                    updateTask(task)
                }
            }
            return Result.success()
        } catch (e: Exception) {
            Log.e("PeriodicTaskUpdater", "Error in doWork: ${e.message}")
            return Result.retry()
        }
    }

    private suspend fun updateTask(data: DataClass) {
        val user = FirebaseAuth.getInstance().currentUser
        val uid = user?.uid ?: return
        val database = FirebaseDatabase.getInstance()
        val taskRef = database.reference.child("Task").child(uid).child(data.id)

        val newNextDueDate = calculateNextDueDate(data.nextDueDate, data.repeatedTaskTime!!)

        data.apply {
            date = SimpleDateFormat("MM/dd/yyyy", Locale.ENGLISH).format(Date(nextDueDate))
            nextDueDate = newNextDueDate
            nextDueDateForCompletedTask = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault()).format(Date(newNextDueDate))
            updateFormattedDateForWidget()
        }

        try {
            taskRef.setValue(data).await()
            Log.d("PeriodicTaskUpdater", "Task updated successfully: ${data.id}")
            scheduleNotification(
                applicationContext,
                data.notificationTime,
                data.id,
                data.message ?: "",
                false,
                data.repeatedTaskTime
            )
            updateWidget( applicationContext)
        } catch (e: Exception) {
            Log.e("PeriodicTaskUpdater", "Error updating task: ${e.message}")
        }
    }

    companion object {
        fun enqueue(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<PeriodicTaskUpdater>(15, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "PeriodicTaskUpdater",
                ExistingPeriodicWorkPolicy.REPLACE,
                request
            )
            Log.d("PeriodicTaskUpdater", "Enqueued PeriodicTaskUpdater")
        }
    }
}