package com.firstyogi.dothing

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey
    var needsSync: Boolean = false,
    var id: String = "",
    val message: String? = "",
    val time: String? = "",
    var date: String? = "",
    var notificationTime: Long = 0,
    val repeatedTaskTime: String? = "",
    var nextDueDate: Long = 0,
    var nextDueDateForCompletedTask: String? = "",
    var formatedDateForWidget: String? = "",
      // New field to track sync status
)
@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks")
    suspend fun getAllTasks(): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE id = :taskId")
    suspend fun getTaskById(taskId: String): TaskEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity)

    @Query("UPDATE tasks SET needsSync = false WHERE id = :taskId")
    suspend fun markTaskSynced(taskId: String)
}
@Database(entities = [TaskEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao

    companion object {
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "task_database"
                ).build().also { instance = it }
            }
        }
    }
}