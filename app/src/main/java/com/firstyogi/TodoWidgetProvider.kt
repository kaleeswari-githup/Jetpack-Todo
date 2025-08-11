package com.firstyogi

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Paint
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.AppWidgetId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.background
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column

import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle

import androidx.glance.color.ColorProvider
import androidx.glance.layout.wrapContentSize
import androidx.glance.layout.wrapContentWidth
import androidx.glance.text.FontFamily
import androidx.glance.unit.ColorProvider
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.firstyogi.dothing.DataClass
import com.firstyogi.dothing.MainActivity
import com.firstyogi.dothing.R
import com.firstyogi.dothing.SigninActivity
import com.firstyogi.dothing.Vibration
import com.firstyogi.dothing.WidgetUpdateWorker
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter


class TodoWidgetProvider : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TodoWidget()
    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        TodoWidget.initializeFirebaseListener(context)
    }
}

class TodoWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // We'll implement this method later

        provideContent {
            GlanceTheme(colors = MyAppWidgetGlanceColorScheme.colors){

                val isSignedIn = FirebaseAuth.getInstance().currentUser != null
               val targetActivity = if (isSignedIn) MainActivity::class.java else SigninActivity::class.java

                val intent = Intent(context, targetActivity).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra("from_widget", true)
                }


                val prefs = currentState<Preferences>()
                val todosJson = prefs[stringPreferencesKey("todos_json")]
                val todos = if (!todosJson.isNullOrEmpty()) {
                    Gson().fromJson(todosJson, Array<DataClass>::class.java).toList()
                } else emptyList()
                val addTaskIntent = Intent(context, AddTaskActivity::class.java).apply {
                    putExtra("from_widget", true)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                val user = FirebaseAuth.getInstance().currentUser
               // val isSignedIn = user != null

                if (!isSignedIn) {
                   // DoThingWidgetContent()
                    SignInPromptWidget(context)
                } else {
                    val prefs = currentState<Preferences>()
                    val todosJson = prefs[stringPreferencesKey("todos_json")]
                    val todos = if (!todosJson.isNullOrEmpty()) {
                        Gson().fromJson(todosJson, Array<DataClass>::class.java).toList()
                    } else emptyList()

                    val addTaskIntent = Intent(context, AddTaskActivity::class.java).apply {
                        putExtra("from_widget", true)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    }

                    WidgetDesign(todos, addTaskIntent,isSignedIn)
                }
            }

            //WidgetLayout(todos)
        }
    }
    companion object {

        private var firebaseListenerInitialized = false

        fun initializeFirebaseListener(context: Context) {
            if (firebaseListenerInitialized) return  // Prevent multiple listeners
            val userId = FirebaseAuth.getInstance().currentUser?.uid

            val ref = FirebaseDatabase.getInstance().getReference("Task").child(userId.toString())

            ref.keepSynced(true) // Ensure immediate updates

            ref.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val todos = snapshot.children.mapNotNull { childSnapshot ->
                        val id = childSnapshot.key ?: return@mapNotNull null
                        val map = childSnapshot.value as? Map<*, *> ?: return@mapNotNull null
                        DataClass(
                            id = id,
                            message = map["message"] as? String ?: "",
                            time = map["time"] as? String ?: "",
                            date = map["date"] as? String ?: "",
                            notificationTime = (map["notificationTime"] as? Long) ?: 0L,
                            repeatedTaskTime = map["repeatedTaskTime"] as? String ?: "",
                            nextDueDate = (map["nextDueDate"] as? Long),
                            formatedDateForWidget = map["formatedDateForWidget"] as? String ?: ""
                        )
                    }

                    CoroutineScope(Dispatchers.IO).launch {
                        val glanceIds = GlanceAppWidgetManager(context).getGlanceIds(TodoWidget::class.java)
                        glanceIds.forEach { glanceId ->
                            updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { prefs ->
                                prefs.toMutablePreferences().apply {
                                    this[stringPreferencesKey("todos_json")] = Gson().toJson(todos)
                                }
                            }
                            TodoWidget().update(context, glanceId)
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            })
            triggerInitialWidgetUpdate(context)
        }

    }
}

@Composable
fun DoThingWidgetContent() {
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(Color.White) // Background of the widget
            .cornerRadius(24.dp)
    ) {
        // Column for vertical layout
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(12.dp),
            verticalAlignment = Alignment.Vertical.CenterVertically,
            horizontalAlignment = Alignment.Horizontal.CenterHorizontally
        ) {
            // Top-right DOTHING text
            Text(
                text = "DOTHING",
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .padding(end = 4.dp)
                    .defaultWeight()
                    ,
                style = TextStyle(
                    color = GlanceTheme.colors.onSecondary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    // You can set a pixel-style fontFamily if available
                )
            )

            Spacer(GlanceModifier.defaultWeight())

            // Google Sign In Button
            Box(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .background(GlanceTheme.colors.primary)
                    .cornerRadius(50.dp)

                 //   .clickable(actionStartActivity(signInIntent))
            ) {
                Row(
                    verticalAlignment = Alignment.Vertical.CenterVertically,
                    horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
                    modifier = GlanceModifier.padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Image(
                        provider = ImageProvider(R.drawable.google_icon),
                        contentDescription = "Google logo",
                        modifier = GlanceModifier.size(24.dp)
                    )

                    Spacer(GlanceModifier.width(8.dp))

                    Text(
                        text = "Sign In",
                        style = TextStyle(
                            color = GlanceTheme.colors.onSecondary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun SignInPromptWidget(context: Context) {
    val signInIntent = Intent(context, SigninActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }

    Box(
        modifier = GlanceModifier
            .padding(5.dp)
            .fillMaxSize()
            .background(GlanceTheme.colors.onPrimary)
            .cornerRadius(18.dp),
           // .clickable(actionStartActivity(signInIntent)),
       // contentAlignment = Alignment.TopEnd

    ) {
        Box (
            modifier = GlanceModifier.fillMaxSize()
                //  .padding(8.dp)
                .background(GlanceTheme.colors.primary)
                .cornerRadius(16.dp)
        ){

        }

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(end = 16.dp)
            ,
            contentAlignment = Alignment.TopEnd // ✅ This aligns content to top-end
        ) {
            Image(
                provider = ImageProvider(resId = R.drawable.dothing_widget_logo),
                contentDescription = null,
                modifier = GlanceModifier.size(56.dp)
            )
        }
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(start = 16.dp,end = 16.dp,bottom = 16.dp), // Optional horizontal padding
            contentAlignment = Alignment.BottomCenter // ✅ This centers the button
        ) {
            Box(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .background(GlanceTheme.colors.onSecondary)
                    .cornerRadius(50.dp)
                    .clickable(actionStartActivity(signInIntent))
            ) {
                Row(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .padding( top = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        provider = ImageProvider(resId = R.drawable.google_icon),
                        contentDescription = null,
                        modifier = GlanceModifier.size(20.dp)
                    )
                    Spacer(modifier = GlanceModifier.width(8.dp))
                    Text(
                        text = "Sign In",
                        style = TextStyle(
                            color = GlanceTheme.colors.background,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = FontFamily.SansSerif
                        )
                    )
                }
            }
        }
     /*   Column(modifier = GlanceModifier.fillMaxSize()
            .padding(12.dp)
            .background(GlanceTheme.colors.primary)) {
            Image(
                provider = androidx.glance.ImageProvider(resId = R.drawable.dothing_widget_logo),
                contentDescription = null,
                modifier = GlanceModifier
                    .size(68.dp)
                   // .padding(8.dp)
                ,
                //  .clickable(actionRunCallback(NextImageAction::class.java)),

            )
           // Spacer(GlanceModifier.defaultWeight())
            Box(
                modifier = GlanceModifier
                  //  .padding(horizontal = 16.dp)
                    .fillMaxWidth()
                    .height(48.dp)
                    .background(
                         GlanceTheme.colors.onSecondary,
                    )
                    .cornerRadius(50.dp)
                    .clickable(
                        actionStartActivity(signInIntent)
                    )

            ) {
               Row(
                   modifier = GlanceModifier
                       .fillMaxSize()
                       .padding(start = 23.dp,end = 23.dp,top = 8.dp, bottom = 8.dp)
                       ,
                   verticalAlignment = Alignment.CenterVertically,
               ) {
                   Image(
                       provider = androidx.glance.ImageProvider(resId = R.drawable.google_icon),
                       contentDescription = null,
                       )
                   Spacer(modifier = GlanceModifier.width(8.dp))
                   Text(
                       text = "Sign In",
                       style = TextStyle(
                           color = GlanceTheme.colors.background ,
                           fontSize = 16.sp,
                           fontWeight = FontWeight.Bold,
                           fontFamily = FontFamily.SansSerif

                       )
                   )
               }
            }

           
            
        }*/


    }
}


@Composable
fun WidgetDesign(todos: List<DataClass>,addTaskIntent: Intent,isSignedIn: Boolean){
    val currentDate = LocalDate.now().format(DateTimeFormatter.ofPattern("MM/dd/yyyy"))

    val todayTasks = todos.filter { it.date == currentDate ||it.date.isNullOrEmpty() || it.date!! <= currentDate }
    val upcomingTasks = todos.filter { it.date!! > currentDate }
    val todayTaskCount = todayTasks.size
    val upcomingTaskCount = upcomingTasks.size
    val context = LocalContext.current
    Log.d("todayTasks","$todayTaskCount")
    Box(
        modifier = GlanceModifier
           .padding(5.dp)
            .cornerRadius(18.dp)
            .background(GlanceTheme.colors.onPrimary)
            .size(163.dp)


            .let {
                if (isSignedIn) {
                    it.clickable(actionStartActivity<MainActivity>())
                } else {
                    it // No click action
                }
            }

        ,
        contentAlignment = Alignment.Center

    ) {

        Box (
            modifier = GlanceModifier.fillMaxSize()
              //  .padding(8.dp)
                .background(ImageProvider(R.drawable.widget_background))
                .cornerRadius(16.dp)
        ){

        }



        Box (modifier = GlanceModifier.fillMaxSize()
            .padding(12.dp)
            // .border(width = 8.dp, color = ColorProvider(Color.Blue))
        ){
            androidx.glance.layout.Column(

                horizontalAlignment = Alignment.CenterHorizontally,

                modifier = GlanceModifier.fillMaxSize()

                // .padding(12.dp)
            ) {
                androidx.glance.layout.Column(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .defaultWeight()
                ) {



                    Text(
                        text = buildAnnotatedString {
                            append(
                                if (todayTaskCount == 0) "no tasks today,"
                                else "$todayTaskCount ${if (todayTaskCount == 1) "task" else "tasks"} today,"
                            )
                        }.toString(),
                        style = TextStyle(
                            color = GlanceTheme.colors.background ,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = FontFamily.SansSerif

                            ),
                        maxLines =  1
                    )
                    Text(
                        text = buildAnnotatedString {
                            append(
                                if (upcomingTaskCount == 0) "0 upcoming tasks.".uppercase()
                                else "$upcomingTaskCount upcoming ${if (upcomingTaskCount == 1) "task." else "tasks."}".uppercase()
                            )
                        }.toString(),
                        style = TextStyle(
                            color = GlanceTheme.colors.surface,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.SansSerif


                            ),
                        maxLines =  1
                    )
                }

                Row(
                    modifier = GlanceModifier
                        .fillMaxWidth(),
                       // .background(GlanceTheme.colors.surface),
                      //  .height(40.dp),
                    //   .padding(top = 16.dp),

                    verticalAlignment = Alignment.Bottom
                ) {




                        Image(
                            provider = androidx.glance.ImageProvider(resId = R.drawable.dothing_widget_image),
                            contentDescription = null,
                            modifier = GlanceModifier
                                .size(58.dp)
                              //  .clickable(actionRunCallback(NextImageAction::class.java))
                        )


                    Spacer(modifier = GlanceModifier.defaultWeight())

                    Image(
                        provider = androidx.glance.ImageProvider(resId =R.drawable.add_button ),
                        contentDescription = null,
                        modifier = GlanceModifier.size(48.dp)
                            .clickable(

                                actionStartActivity(addTaskIntent)
                            )

                    )

                  /*  Box(
                        modifier = GlanceModifier
                            .size(48.dp)
                            .cornerRadius(24.dp)
                            .background(GlanceTheme.colors.secondary)
                            .clickable(

                                actionStartActivity(addTaskIntent)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            provider = androidx.glance.ImageProvider(resId = R.drawable.new_plus_icon),
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(GlanceTheme.colors.primary)

                        )
                    }*/


                }
            }
        }

    }

}
fun triggerInitialWidgetUpdate(context: Context) {
    CoroutineScope(Dispatchers.IO).launch {
        val glanceIds = GlanceAppWidgetManager(context).getGlanceIds(TodoWidget::class.java)
        glanceIds.forEach { glanceId ->
            TodoWidget().update(context, glanceId)
        }
    }
}
class NextImageAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        // Get current index from SharedPreferences
        val prefs = context.getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)
        val currentIndex = prefs.getInt("image_index", 0)
        val nextIndex = (currentIndex + 1) % 3  // assuming you have 3 images

        // Save new index
        prefs.edit().putInt("image_index", nextIndex).apply()

        // Trigger widget update
        TodoWidget().update(context, glanceId)
    }
}


