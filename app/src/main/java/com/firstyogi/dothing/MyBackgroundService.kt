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
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.ParseException
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
                for (childSnapshot in snapshot.children) {
                    val data = childSnapshot.getValue(DataClass::class.java)
                    val currentTime = System.currentTimeMillis()
                    var id = data!!.id
                    var repeatOption = data.repeatedTaskTime
                    // val nextDueDateInMillis = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault()).parse(data.nextDueDateForCompletedTask)?.time ?: 0L


                    val dateFormat = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
                    val nextDueDateString = data.nextDueDateForCompletedTask

                    if (!nextDueDateString.isNullOrBlank()) {
                        val storedDateInMillis = try {
                            dateFormat.parse(nextDueDateString)?.time ?: 0L
                        } catch (e: ParseException) {
                            e.printStackTrace()
                            0L
                        }
updateTaskInFirebase(data,repeatOption!!,applicationContext)
                        if (currentDate >= storedDateInMillis.toString()) {
                            val repeatOption = data.repeatedTaskTime
                            if (!repeatOption.isNullOrBlank()) {

                            }
                        }
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
                    val map = childSnapshot.value as? Map<*, *> ?: continue
                    val id = childSnapshot.key ?: continue
                    // val data = childSnapshot.getValue(DataClass::class.java)
                    val data = DataClass(
                        id = map["id"] as? String ?: "",
                        message = map["message"] as? String ?: "",
                        time = map["time"] as? String ?: "",
                        date = map["date"] as? String ?: "",
                        notificationTime = when (val nt = map["notificationTime"]) {
                            is Long -> nt
                            is String -> nt.toLongOrNull() ?: 0L
                            else -> 0L
                        },
                        repeatedTaskTime = map["repeatedTaskTime"] as? String ?: "",
                        nextDueDate = when (val nd = map["nextDueDate"]) {
                            is Long -> nd
                            is String -> nd.toLongOrNull()
                            else -> null
                        },
                        nextDueDateForCompletedTask = map["nextDueDateForCompletedTask"] as? String ?: "",
                        formatedDateForWidget = map["formatedDateForWidget"] as? String ?: ""
                    )
                    if (data != null) {
                        val currentDateInMillis = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault()).parse(currentDate)?.time ?: 0L
                        val nextDueDateInMillis = if (!data.nextDueDateForCompletedTask.isNullOrEmpty()) {
                            SimpleDateFormat("MM/dd/yyyy", Locale.getDefault()).parse(data.nextDueDateForCompletedTask)?.time ?: 0L
                        } else {
                            0L
                        }

                        if (data.repeatedTaskTime in listOf("Daily", "Weekly", "Monthly", "Yearly") &&
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
                            val startDate = map["startDate"] as? String ?: ""
                            // Update the task data
                            val updatedData = data.copy(
                                date = newDateFormatted,
                                nextDueDate = newNextDueDate,
                                nextDueDateForCompletedTask = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault()).format(Date(newNextDueDate)),
                                notificationTime = nextNotificationTime,
                                startDate = startDate
                                // isCheckedState = false
                            )
                            Log.d("StartDateMissing","${data.startDate}")
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


/*@Suppress("UNREACHABLE_CODE")
class MyWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {
    override fun doWork(): Result {
        // Execute your condition logic here
        val database = FirebaseDatabase.getInstance()
        val user = FirebaseAuth.getInstance().currentUser
        val uid = user?.uid ?: return Result.failure()
        val taskRef = database.reference.child("Task").child(uid)
        val dateFormat = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
        val completedTasksRef = database.reference.child("Task").child("CompletedTasks").child(uid.toString())
        val currentDate = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault()).format(Date())
        val taskList = mutableListOf<DataClass>()

        // Use a latch to wait for async completion
        val latch = java.util.concurrent.CountDownLatch(1)

        taskRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val taskList = snapshot.children.mapNotNull { it.getValue(DataClass::class.java) }

               // checkAndUpdateTaskAll(taskList, applicationContext)
                for (task in taskList) {
                    val repeatOption = task.repeatedTaskTime
                    updateTaskInFirebase(task,repeatOption = repeatOption!!, context = applicationContext)
                  //  val nextDueDateStr = task.nextDueDateForCompletedTask

                  /*  if (!nextDueDateStr.isNullOrBlank() && !repeatOption.isNullOrBlank()) {
                        try {
                          //  val storedDateMillis = dateFormat.parse(nextDueDateStr)?.time ?: 0L
                            val currentTime = System.currentTimeMillis()
                            if (storedDateMillis <= currentTime) {
                              //  checkAndUpdateTask(task, applicationContext)

                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }*/
                }
                latch.countDown() // Signal completion
            }

            override fun onCancelled(error: DatabaseError) {
                latch.countDown() // Signal completion even on error
            }
        })

        // Wait for Firebase to finish (timeout to prevent hanging)
        latch.await(10, java.util.concurrent.TimeUnit.SECONDS)
        return Result.success()

        completedTasksRef.addListenerForSingleValueEvent(object : ValueEventListener {
            @RequiresApi(Build.VERSION_CODES.O)
            override fun onDataChange(snapshot: DataSnapshot) {
                for (childSnapshot in snapshot.children) {
                    val data = childSnapshot.getValue(DataClass::class.java)
                    if (data != null) {
                        val currentDateInMillis = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault()).parse(currentDate)?.time ?: 0L
                        val nextDueDateInMillis = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault()).parse(data.nextDueDateForCompletedTask)?.time ?: 0L

                        if (data.repeatedTaskTime in listOf("Daily", "Weekly", "Monthly", "Yearly") &&
                            currentDateInMillis >= nextDueDateInMillis) {

                            // Remove from completed tasks
                            val completedTaskKey = childSnapshot.key
                            completedTasksRef.child(completedTaskKey.toString()).removeValue()
                            checkAndUpdateTask(data.id, data.repeatedTaskTime!!, context = applicationContext)
                            // Calculate new dates before moving to active tasks
                            val newNextDueDate = calculateNextDueDate(data.nextDueDate!!, data.repeatedTaskTime!!)
                            val originalNotificationOffset = data.notificationTime - data.nextDueDate!!
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
                            val NextNotificationTime = calculateNextNotificationTime(data.notificationTime, newNextDueDate)
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
           // checkAndUpdateTaskAll(data, applicationContext)

        }
    }
}
@RequiresApi(Build.VERSION_CODES.O)
fun checkAndRescheduleCompletedTask(
    data: DataClass,
    completedTasksRef: DatabaseReference,
    activeTasksRef: DatabaseReference,
    context: Context
) {
    val currentDateInMillis = System.currentTimeMillis()

    // Parse stored date
    val formatter = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
    val nextDueDateInMillis = try {
        formatter.parse(data.nextDueDateForCompletedTask ?: "")?.time ?: 0L
    } catch (e: Exception) {
        0L
    }

    // If current date is today or past due
    if (data.repeatedTaskTime in listOf("Daily", "Weekly", "Monthly", "Yearly") &&
        currentDateInMillis >= nextDueDateInMillis
    ) {
        val newNextDueDate = calculateNextDueDate(nextDueDateInMillis, data.repeatedTaskTime!!)
        val originalOffset = data.notificationTime - (data.nextDueDate ?: 0L)
        val newNotificationTime = newNextDueDate + originalOffset

        // Update fields
        val updatedData = data.copy(
            date = formatter.format(Date(nextDueDateInMillis)), // new visible date
            nextDueDate = newNextDueDate,
            nextDueDateForCompletedTask = formatter.format(Date(newNextDueDate)),
            notificationTime = newNotificationTime
        )

        // 1. Remove from completed
        completedTasksRef.child(data.id).removeValue()

        // 2. Add to active
        activeTasksRef.child(data.id).setValue(updatedData).addOnSuccessListener {
            // 3. Schedule notification
            if (newNotificationTime > System.currentTimeMillis()) {
                scheduleNotification(
                    context,
                    newNotificationTime,
                    data.id,
                    data.message ?: "",
                    false,
                    data.repeatedTaskTime
                )
            } else {
                schedulePastTimeNotification(
                    context,
                    newNotificationTime,
                    data.id,
                    data.message ?: "",
                    false,
                    data.repeatedTaskTime
                )
            }

            Log.d("TaskMove", "Task ${data.id} moved back to active and notification rescheduled.")
        }
    }
}*/
fun checkAndRescheduleCompletedTask(
    data: DataClass,
    currentDate: String,
    completedTasksRef: DatabaseReference,
    activeTasksRef: DatabaseReference,
    context: Context
) {
    val formatter = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
    val currentMillis = try {
        formatter.parse(currentDate)?.time ?: 0L
    } catch (e: Exception) {
        0L
    }

    val nextDueDateMillis = try {
        formatter.parse(data.nextDueDateForCompletedTask ?: "")?.time ?: 0L
    } catch (e: Exception) {
        0L
    }

    if (data.repeatedTaskTime in listOf("Daily", "Weekly", "Monthly", "Yearly") &&
        currentMillis >= nextDueDateMillis) {

        // ✅ Step 1: Calculate new due date
        val newNextDueDate = calculateNextDueDate(data.nextDueDate!!, data.repeatedTaskTime!!)

        // ✅ Step 2: Build next notification time based on original time of day
        val notifCal = Calendar.getInstance().apply { timeInMillis = data.notificationTime ?: 0L }
        val newDueCal = Calendar.getInstance().apply {
            timeInMillis = newNextDueDate
            set(Calendar.HOUR_OF_DAY, notifCal.get(Calendar.HOUR_OF_DAY))
            set(Calendar.MINUTE, notifCal.get(Calendar.MINUTE))
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val nextNotificationTime = newDueCal.timeInMillis

        // ✅ Step 3: Update DataClass with new dates
        val updatedData = data.copy(
            date = formatter.format(Date(newNextDueDate)),
            nextDueDate = newNextDueDate,
            nextDueDateForCompletedTask = formatter.format(Date(newNextDueDate)),
            notificationTime = nextNotificationTime
        )

        // ✅ Step 4: Remove from completed & add to active
        completedTasksRef.child(data.id).removeValue()
        activeTasksRef.child(data.id).setValue(updatedData)

        // ✅ Step 5: Schedule notification
        val now = System.currentTimeMillis()
        if (nextNotificationTime > now) {
            scheduleNotification(
                context,
                nextNotificationTime,
                data.id,
                data.message ?: "",
                false,
                data.repeatedTaskTime
            )
        } else {
            schedulePastTimeNotification(
                context,
                nextNotificationTime,
                data.id,
                data.message ?: "",
                false,
                data.repeatedTaskTime
            )
        }

        Log.d("Rescheduler", "Moved ${data.id} back to Home & scheduled notification at ${Date(nextNotificationTime)}")
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








