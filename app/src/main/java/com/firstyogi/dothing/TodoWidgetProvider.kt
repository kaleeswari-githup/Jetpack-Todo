package com.firstyogi.dothing

import android.content.Context
import android.provider.SyncStateContract.Helpers.update
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding

import androidx.glance.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

import androidx.glance.layout.Alignment
import androidx.glance.text.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.glance.layout.Box
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.ImageProvider
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.unit.ColorProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.CountDownLatch


class TodoWidgetProvider : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TodoWidget()
}

class TodoWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // We'll implement this method later

        provideContent {
            WidgetLayout(todos)
        }
    }
    companion object {
        private val todos = mutableListOf<DataClass>()
        private var valueEventListener: ValueEventListener? = null

        fun initializeFirebaseListener(context: Context) {
            if (valueEventListener == null) {
                val database = FirebaseDatabase.getInstance()
                val user = FirebaseAuth.getInstance().currentUser
                val uid = user?.uid
                val databaseRef = database.reference.child("Task").child(uid.toString())

                valueEventListener = object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        todos.clear()
                        for (childSnapshot in snapshot.children) {
                            val id = childSnapshot.key ?: continue
                            val map = childSnapshot.value as? Map<*, *> ?: continue

                            val data = DataClass(
                                id = id,
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
                                //nextDueDateForCompletedTask = map["nextDueDateForCompletedTask"] as? String ?: "",
                                formatedDateForWidget = map["formatedDateForWidget"] as? String ?: ""
                            )

                            todos.add(data)
                        }
                        // Update the widget when data changes
                        GlobalScope.launch(Dispatchers.Main) {
                            updateWidgets(context)
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        // Handle error
                    }
                }

                databaseRef.addValueEventListener(valueEventListener!!)
            }
        }

        suspend fun updateWidgets(context: Context) {
            GlanceAppWidgetManager(context).getGlanceIds(TodoWidget::class.java).forEach { glanceId ->
                TodoWidget().update(context, glanceId)
            }
        }
    }
}







@Composable
fun WidgetLayout(todos: List<DataClass>) {
    Column(
        modifier = GlanceModifier
            .fillMaxWidth()
            .background(Color.White)
            //.padding(8.dp)
    ) {
        Text(
            text = "Todo List",
            modifier = GlanceModifier.padding(bottom = 8.dp)
        )
       LazyColumn(



        ) {
           items(todos.size) { index ->
               TodoItem(todos[index])
           }
        }
    }
}

@Composable
fun TodoItem(todo: DataClass) {

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .size(180.dp)

            //.padding(vertical = 4.dp)
            .background(Color.Black)
            .cornerRadius(120.dp),
        contentAlignment = Alignment.Center
            //.clickable(actionStartActivity<UpdateActivity>(data = mapOf("id" to todo.id)))
    ) {
        Column(
            modifier = GlanceModifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // We can't use ThemedSquareImage in Glance, so we'll use a placeholder
     androidx.glance.Image(
         provider = ImageProvider(R.drawable.light_square),
         contentDescription = null,
         modifier = GlanceModifier.padding(top = 24.dp)

     )

            Text(
                text = todo.message ?: "",
                modifier = GlanceModifier.padding(top = 24.dp,start = 16.dp,end = 16.dp),
                style = androidx.glance.text.TextStyle(
                    color = ColorProvider(Color.White)
                )
            )

            Text(
                text = "${todo.formatedDateForWidget} ${todo.time}",
                modifier = GlanceModifier.padding(top = 4.dp,start = 16.dp,end = 16.dp),
                style = androidx.glance.text.TextStyle(
                    color = ColorProvider(Color.White)
                )

            )
            if (todo.repeatedTaskTime in listOf("DAILY", "WEEKLY", "MONTHLY", "YEARLY")) {
                androidx.glance.Image(
                    provider = ImageProvider(R.drawable.repeat_icon_black),
                    contentDescription = null,
                    modifier = GlanceModifier
                        .padding(top = 8.dp)
                        .size(32.dp)
                )
            }

        }
    }
}
@Composable
fun ThemedSquareImageGlance() {
    val isDarkTheme = isSystemInDarkTheme()

    val imageRes = if (isDarkTheme) {
        R.drawable.dark_square
    } else {
        R.drawable.light_square
    }

    Image(
        painter = painterResource(id = imageRes),
        contentDescription = null,


    )

}