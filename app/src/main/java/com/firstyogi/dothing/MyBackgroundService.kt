package com.firstyogi.dothing

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.work.CoroutineWorker
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MyWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {
    override fun doWork(): Result {
        // Execute your condition logic here
        val database = FirebaseDatabase.getInstance()
        val user = FirebaseAuth.getInstance().currentUser
        val uid = user?.uid
        var TaskRef = database.reference.child("Task").child(uid.toString())
        val completedTasksRef = database.reference.child("Task").child("CompletedTasks").child(uid.toString())
        val taskList = mutableListOf<DataClass>()
        val currentDate = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault()).format(Date())
        TaskRef.addListenerForSingleValueEvent(object :ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                for (childSnapshot in snapshot.children){
                    val data = childSnapshot.getValue(DataClass::class.java)
                    val currentTime = System.currentTimeMillis()
                    var id = data!!.id
                    var repeatOption = data.repeatedTaskTime
                   // val nextDueDateInMillis = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault()).parse(data.nextDueDateForCompletedTask)?.time ?: 0L
                    val storedDateInMillis = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
                        .parse(data.nextDueDateForCompletedTask!!)?.time ?: 0L

                    if (currentDate >= storedDateInMillis.toString()) {
                        checkAndUpdateTask(id, repeatOption!!, context = applicationContext)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }

        })
        completedTasksRef.addListenerForSingleValueEvent(object : ValueEventListener {
            @RequiresApi(Build.VERSION_CODES.O)
            override fun onDataChange(snapshot: DataSnapshot) {
                for (childSnapshot in snapshot.children) {
                    val data = childSnapshot.getValue(DataClass::class.java)
                    if (data != null) {
                        val currentDateInMillis = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault()).parse(currentDate)?.time ?: 0L
                        val nextDueDateInMillis = if (!data.nextDueDateForCompletedTask.isNullOrEmpty()) {
                            SimpleDateFormat("MM/dd/yyyy", Locale.getDefault()).parse(data.nextDueDateForCompletedTask)?.time ?: 0L
                        } else {
                            0L
                        }

                        if (data.repeatedTaskTime in listOf("DAILY", "WEEKLY", "MONTHLY", "YEARLY") &&
                            currentDateInMillis >= nextDueDateInMillis) {

                            // Remove from completed tasks
                            val completedTaskKey = childSnapshot.key
                            completedTasksRef.child(completedTaskKey.toString()).removeValue()
                            checkAndUpdateTask(data.id, data.repeatedTaskTime!!, context = applicationContext)
                            // Calculate new dates before moving to active tasks
                            val newNextDueDate = calculateNextDueDate(data.nextDueDate!!, data.repeatedTaskTime!!)
                            val originalNotificationOffset = data.notificationTime !!- data.nextDueDate!!
                            val nextNotificationTime = newNextDueDate + originalNotificationOffset
                            val newDateFormatted = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault()).format(Date(data.nextDueDate!!))
                            // Update the task data
                            val updatedData = data.copy(
                                date = newDateFormatted,
                                nextDueDate = newNextDueDate,
                                nextDueDateForCompletedTask = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault()).format(Date(newNextDueDate)),
                                notificationTime = nextNotificationTime,
                               // isCheckedState = false
                            )
                            val NextNotificationTime = calculateNextNotificationTime(data.notificationTime!!, newNextDueDate)
                            val now = Calendar.getInstance().timeInMillis
                            // Move to active tasks with updated data
                            val databaseRef = database.reference.child("Task").child(uid.toString()).child(data.id)
                            databaseRef.setValue(updatedData)

                                // Schedule new notification only after successful database update
                            if(nextNotificationTime > now){
                                scheduleNotification(
                                    applicationContext,
                                    nextNotificationTime,
                                    data.id,
                                    data.message ?: "",
                                    false,
                                    data.repeatedTaskTime
                                )
                            }else{
                                schedulePastTimeNotification(
                                    applicationContext,
                                    nextNotificationTime,
                                    data.id,
                                    data.message ?: "",
                                    false,
                                    data.repeatedTaskTime
                                )
                            }

                                Log.d("Worker", "Rescheduled notification for task ${data.id} at ${Date(nextNotificationTime)}")
                            
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Worker", "Database error: ${error.message}")
            }
        })

        return Result.success()
        processTasks(taskList)
    }
    private fun processTasks(taskList: List<DataClass>) {
        val currentTime = System.currentTimeMillis()

        for (data in taskList) {
            // Check if the current time is greater than or equal to nextDueDate

            // Call the checkAndUpdateTask function
            checkAndUpdateTask(data.id, data.repeatedTaskTime ?: "", applicationContext)

        }
    }
}
class LocalDatabaseOperations(private val context: Context) {
    private val database = AppDatabase.getInstance(context)
    private val taskDao = database.taskDao()

    suspend fun checkAndUpdateTask(itemId: String, repeatOption: String) =
        withContext(Dispatchers.IO) {
            val task = taskDao.getTaskById(itemId) ?: return@withContext
            updateTaskInLocalDatabase(task, repeatOption)
        }

    private suspend fun updateTaskInLocalDatabase(task: TaskEntity, repeatOption: String) =
        withContext(Dispatchers.IO) {
            // Calculate the new next due date
            val newNextDueDate = calculateNextDueDate(task.nextDueDate, repeatOption)

            // Update the task data
            val updatedTask = task.copy(
                date = SimpleDateFormat(
                    "MM/dd/yyyy",
                    Locale.ENGLISH
                ).format(Date(task.nextDueDate)),
                nextDueDate = newNextDueDate,
                nextDueDateForCompletedTask = SimpleDateFormat(
                    "MM/dd/yyyy",
                    Locale.getDefault()
                ).format(Date(newNextDueDate)),
                needsSync = true
            )

            // Save the updated data to the local database
            taskDao.insertTask(updatedTask)

            // Schedule the next notification
            val nextNotificationTime =
                calculateNextNotificationTime(task.notificationTime, newNextDueDate)
            scheduleNotification(
                context,
                task.notificationTime, // Use the original notification time
                task.id,
                task.message!!,
                false, // Assuming isCheckedState should be false for a new cycle
                repeatOption
            )

           // updateWidget( context)

            Log.d(
                "NotificationCheck",
                "Scheduled notification for task ${task.id} at ${Date(newNextDueDate)}"
            )
        }
}
/*completedTasksRef.addListenerForSingleValueEvent(object : ValueEventListener {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onDataChange(snapshot: DataSnapshot) {
        for (childSnapshot in snapshot.children) {
            val data = childSnapshot.getValue(DataClass::class.java)
            var id = data!!.id
            var repeatOption = data.repeatedTaskTime



            val currentDateInMillis = currentDate

            // val nextDueDate = getNextDueDate(data.date!!, data.repeatedTaskTime!!)
            if (data.repeatedTaskTime in listOf(
                    "DAILY",
                    "WEEKLY",
                    "MONTHLY",
                    "YEARLY"
                ) && data.nextDueDateForCompletedTask == currentDateInMillis
            ) {

                val completedTaskKey = childSnapshot.key
                completedTasksRef.child(completedTaskKey.toString()).removeValue()
                // Move the task back to active tasks
                val databaseRef = database.reference.child("Task").child(uid.toString())
                    .child(data.id)
                databaseRef.setValue(data)
                checkAndUpdateTask(id, repeatOption!!, context = applicationContext)
                val newNextDueDate =
                    calculateNextDueDate(data.nextDueDate, repeatOption)

                // Update the task data
                val originalNotificationOffset = data.notificationTime - data.nextDueDate
                val nextNotificationTime = newNextDueDate + originalNotificationOffset

                // Save the updated data back to Firebase

                // Schedule the next notification
                // val nextNotificationTime = calculateNextNotificationTime(data.notificationTime, newNextDueDate)
                scheduleNotification(
                    applicationContext,
                    nextNotificationTime, // Use the original notification time
                    data.id,
                    data.message ?: "",
                    false, // Assuming isCheckedState should be false for a new cycle
                    repeatOption
                )
                // val nextDueDate = calculateNextDueDate(System.currentTimeMillis(), data.repeatedTaskTime!!)
                //data.nextDueDate = nextDueDate
                Log.d(
                    "Worker",
                    "Task unmarked from completed, scheduling notification for task ${data.id}"
                )



            }



        }
    }

    override fun onCancelled(error: DatabaseError) {

    }
})*/








