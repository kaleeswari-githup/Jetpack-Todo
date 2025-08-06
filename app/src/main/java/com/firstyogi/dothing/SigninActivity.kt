package com.firstyogi.dothing

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlarmManager
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.media.Image
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.solver.state.State
import androidx.core.app.NotificationManagerCompat
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import com.firstyogi.TodoWidget

import com.firstyogi.ui.theme.AppJetpackComposeTheme
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.FirebaseDatabase
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class SigninActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppJetpackComposeTheme {
                // A surface container using the 'background' color from the theme
                enableEdgeToEdge()
                SignInScreen()

            }
        }

    }


    override fun onStart() {
        super.onStart()
        val auth: FirebaseAuth = FirebaseAuth.getInstance()

        val user = auth.currentUser
        if (user != null) {
            val sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE)
            val notificationChoiceMade = sharedPreferences.getBoolean("notification_permission_choice", false)
            val exactAlarmChoiceMade = sharedPreferences.getBoolean("exact_alarm_permission_choice", false)

            // Check notification permission
            if (!areNotificationsEnabled(this) && !notificationChoiceMade) {
                val intent = Intent(this@SigninActivity, NotificationPermissionActivity::class.java)
                startActivity(intent)
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                finish()
            }
            // Check exact alarm permission (only for Android 12+)
            else if (
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                !exactAlarmChoiceMade &&
                !(getSystemService(Context.ALARM_SERVICE) as AlarmManager).canScheduleExactAlarms()
            ) {
                val intent = Intent(this@SigninActivity, ExactAlarmNotificatiionAllowActivity::class.java)
                startActivity(intent)
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                finish()
            }
            // Both permissions are granted or choices made, go to MainActivity
            else {
                val intent = Intent(this@SigninActivity, MainActivity::class.java)
                startActivity(intent)
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                finish()
            }

            val startTime = System.currentTimeMillis()
            Log.d(TAG, "onStart called")

            // Your sign-in code...

            val endTime = System.currentTimeMillis()
            val elapsedTime = endTime - startTime
            Log.d("singintime", "Sign-in process took $elapsedTime milliseconds")
        }
    }
    @SuppressLint("MissingSuperCall")
    override fun onBackPressed() {
        // Exit the app when the back button is pressed
        finishAffinity()
    }
}


private fun areNotificationsEnabled(context: Context): Boolean {
    val notificationManager = NotificationManagerCompat.from(context)
    return notificationManager.areNotificationsEnabled()
}

@Composable
fun SignInScreen(){

    val auth: FirebaseAuth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser
    lateinit var googleSignInClient: GoogleSignInClient
    lateinit var activityResultLauncher: ActivityResultLauncher<Intent>

    val context = LocalContext.current
    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestIdToken("664988258330-3ic7tbaom8eeruprcj0lktomos8bnrdo.apps.googleusercontent.com")
        .requestEmail()
        .build()
    googleSignInClient = GoogleSignIn.getClient(LocalContext.current, gso)
    activityResultLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val intent = result.data
            val task = GoogleSignIn.getSignedInAccountFromIntent(intent)
            try {
                val account = task.getResult(ApiException::class.java)
                val credential = GoogleAuthProvider.getCredential(account?.idToken, null)
                auth.signInWithCredential(credential)
                    .addOnCompleteListener { signInTask ->
                        if (signInTask.isSuccessful) {
                            val applicationContext = context.applicationContext
                            val user = FirebaseAuth.getInstance().currentUser
                            val userId = user?.uid ?: return@addOnCompleteListener
                            CoroutineScope(Dispatchers.IO).launch {
                                delay(300) // Let Firebase settle

                                // 1. Get user's tasks from Firebase
                                val user = FirebaseAuth.getInstance().currentUser
                                val userId = user?.uid ?: return@launch
                                val snapshot = FirebaseDatabase.getInstance().getReference("Task").child(userId).get().await()

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
                                    )} // your mapping logic

                                // 2. Convert to JSON
                                val todosJson = Gson().toJson(todos)

                                // 3. Update widget state
                                val glanceIds = GlanceAppWidgetManager(applicationContext).getGlanceIds(TodoWidget::class.java)
                                glanceIds.forEach { glanceId ->
                                    updateAppWidgetState(
                                        applicationContext,
                                        PreferencesGlanceStateDefinition,
                                        glanceId
                                    ) { prefs ->
                                        prefs.toMutablePreferences().apply {
                                            this[stringPreferencesKey("todos_json")] = todosJson
                                            this[stringPreferencesKey("user_signed_in")] = "true"
                                        }
                                    }

                                    // 4. Trigger widget refresh
                                    TodoWidget().update(applicationContext, glanceId)
                                }
                            }
                           /* CoroutineScope(Dispatchers.IO).launch {
                                delay(300)
                                val ref = FirebaseDatabase.getInstance().getReference("Task").child(userId)
                                val snapshot = ref.get().await()

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

                                val todosJson = Gson().toJson(todos)

                                val glanceIds = GlanceAppWidgetManager(applicationContext).getGlanceIds(TodoWidget::class.java)

                                glanceIds.forEach { glanceId ->
                                    updateAppWidgetState(
                                        applicationContext,
                                        PreferencesGlanceStateDefinition,
                                        glanceId
                                    ) { prefs ->
                                        prefs.toMutablePreferences().apply {
                                            this[stringPreferencesKey("todos_json")] = todosJson
                                        }
                                    }

                                    TodoWidget().update(applicationContext, glanceId)
                                }
                            }*/
                            val sharedPreferences =
                                context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
                            val notificationChoiceMade = sharedPreferences.getBoolean(
                                "notification_permission_choice",
                                false
                            )
                            val exactAlarmChoiceMade =
                                sharedPreferences.getBoolean("exact_alarm_permission_choice", false)

                            // Check notification permission
                            if (!areNotificationsEnabled(context) && !notificationChoiceMade) {
                                val intent =
                                    Intent(context, NotificationPermissionActivity::class.java)
                                context.startActivity(intent)
                                (context as Activity).overridePendingTransition(
                                    android.R.anim.fade_in,
                                    android.R.anim.fade_out
                                )
                            }
                            // Check exact alarm permission (only for Android 12+)
                            else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !exactAlarmChoiceMade &&
                                !(context.getSystemService(Context.ALARM_SERVICE) as AlarmManager).canScheduleExactAlarms()
                            ) {
                                val intent = Intent(
                                    context,
                                    ExactAlarmNotificatiionAllowActivity::class.java
                                )
                                context.startActivity(intent)
                                (context as Activity).overridePendingTransition(
                                    android.R.anim.fade_in,
                                    android.R.anim.fade_out
                                )
                            } else {
                                // Both permissions are granted or choices made, go to MainActivity
                                val intent = Intent(context, MainActivity::class.java)
                                context.startActivity(intent)
                                (context as Activity).overridePendingTransition(
                                    android.R.anim.fade_in,
                                    android.R.anim.fade_out
                                )
                            }
                        }


            }


            } catch (e: ApiException) {
                Toast.makeText(context,  e.localizedMessage, Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
        }
    }

    Box(modifier = Modifier
        .fillMaxSize()
        .background(color = MaterialTheme.colors.background)
    ){
        var visible by remember {
            mutableStateOf(false)
        }
        val isDarkTheme = isSystemInDarkTheme()
        LaunchedEffect(true) {
            visible = true
        }
        var googleVisible by remember {
            mutableStateOf(false)
        }
        LaunchedEffect(googleVisible) {
            delay(100)
            googleVisible = true
        }
        SignInScreenGridItems()
        Canvas(modifier = Modifier.fillMaxSize()) {
            val gradientBrush = Brush.verticalGradient(
                colors = if (isDarkTheme) {
                    listOf(
                        Color(0xFF000000), // Solid black at the top
                        Color(0x00000000)  // Fades to transparent
                    )
                } else {
                    listOf(
                        Color(0xFFD8DFE7), // Light color at the top
                        Color(0x00EDEDED)  // Fades to transparent
                    )
                },
                startY = 200f,  // Start from the top
                endY = size.height * 0.8f // Gradually fade over the top 30% of the screen
            )

            val opacityBrush = Brush.verticalGradient(
                colors = if (isDarkTheme) {
                    listOf(
                        Color(0x00000000), // Start with transparent
                        Color(0xFF000000)  // Ends with solid black
                    )
                } else {
                    listOf(
                        Color(0x00EDEDED), // Start with transparent
                        Color(0xFFD8DFE7)  // Ends with solid light color
                    )
                },
                startY = size.height * 0.1f,  // Start fading earlier (70% of the screen height)
                endY = size.height + 160.dp.toPx() // Extend below screen for a stronger effect
            )

            drawRect(brush = gradientBrush) // Top overlay
            drawRect(brush = opacityBrush)  // Bottom overlay
        }


       // ThemedGridImage(modifier = Modifier)
Box (modifier = Modifier.fillMaxSize()){
    Column(modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally) {
        val offsetY by animateDpAsState(
            targetValue = if (visible) 0.dp else 32.dp,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessVeryLow
            ),
        )
        val scale by animateFloatAsState(
            targetValue = if (visible) 1f else 0f,
            animationSpec = keyframes {
                durationMillis = 500
            }
        )
        val googleOffsetY by animateDpAsState(
            targetValue = if (googleVisible) 0.dp else 32.dp,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessVeryLow
            )
        )
        val googleScale by animateFloatAsState(
            targetValue = if (googleVisible) 1f else 0f,
            animationSpec = keyframes {
                durationMillis = 500
            }
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(text = "DOTHING",
            fontSize = 40.sp,
            fontFamily = interDisplayFamily,
            fontWeight = FontWeight.W100,
            color = MaterialTheme.colors.primary,

        )
//                ThemedImage(modifier = Modifier
//                    .padding(top = 120.dp)
//                    .offset(y = offsetY)
//                    .alpha(scale))
        Spacer(modifier = Modifier.weight(1f))

        Box(modifier = Modifier
            .wrapContentSize()
            .offset(y = googleOffsetY)
            .alpha(googleScale)

            .padding(bottom = 84.dp)
            .background(
                color = MaterialTheme.colors.secondary,
                shape = RoundedCornerShape(64.dp)
            )
            .clickable(indication = null,
                interactionSource = remember { MutableInteractionSource() }) {
                val signInIntent = googleSignInClient.signInIntent
                activityResultLauncher.launch(signInIntent)
                Vibration(context)

            }
            ,
            contentAlignment = Alignment.Center
        ){
            Row(modifier = Modifier
                .wrapContentWidth()
                .padding(start = 48.dp, end = 48.dp, top = 24.dp, bottom = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ){
                Image(painter = painterResource(id = R.drawable.google_icon), contentDescription = "")
                Spacer(modifier = Modifier.padding(start = 16.dp))
                ButtonTextWhiteTheme(text = ("Continue with Google"),
                    color = MaterialTheme.colors.primary,
                    modifier = Modifier)
            }
        }


    }
}


    }
}
data class TaskItem(
    val id: Int,
    val title: String,
    val dateAndTime:String,
    val icon:String
)
val taskList = listOf(
    TaskItem(1, "Read Book", "TODAY, 9:30 PM","Daily"),
    TaskItem(2, "Book Dentist", "TODAY, 10:15 AM","No repeat"),
    TaskItem(3, "Visual Update", "TOMORROW, 9:30 AM","No repeat"),

    )
@Composable
fun SignInScreenGridItems(){

    val screenWidth = LocalConfiguration.current.screenWidthDp.dp  // Get screen width
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val boxSize = 184.dp
    val totalTravelDistance = screenHeight + boxSize
    val padding = 24.dp  // Padding from both sides

    val infiniteTransition = rememberInfiniteTransition()

// Define equal spacing for 4 boxes
    val spacing = totalTravelDistance.value / 4

// Animate Red Box (Left Side)
    val yOffset1 by infiniteTransition.animateFloat(
        initialValue = totalTravelDistance.value,
        targetValue = -boxSize.value,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 16000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "yOffset1"
    )
    val rotation1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -45f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 50000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    val rotation2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 45f, // Reverse rotation for variety
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 50000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

// Animate Blue Box (Right Side)
    val yOffset2 by infiniteTransition.animateFloat(
        initialValue = totalTravelDistance.value + spacing,
        targetValue = -boxSize.value + spacing,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 16000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "yOffset2"
    )

// Animate Green Box (Left Side)
    val yOffset3 by infiniteTransition.animateFloat(
        initialValue = totalTravelDistance.value + (2 * spacing),
        targetValue = -boxSize.value + (2 * spacing),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 16000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "yOffset3"
    )

// Animate Yellow Box (Right Side)
    val yOffset4 by infiniteTransition.animateFloat(
        initialValue = totalTravelDistance.value + (3 * spacing),
        targetValue = -boxSize.value + (3 * spacing),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 16000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "yOffset4"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        // Red Box (Left Side, 24.dp from left)
        SignInScreenCircleImages(message = "Book Dentist",
            dateAndTime ="TODAY, 9:30 PM" ,
            repeatOption = "No Repeat",
            modifier = Modifier
            .offset(
                    x = padding,
            y = (yOffset1 % (totalTravelDistance.value + boxSize.value)).dp
        )
                .graphicsLayer(rotationZ = rotation1))
        SignInScreenCircleImages(message = "Visual Update",
            dateAndTime ="TODAY, 9:30 PM" ,
            repeatOption = "Daily",
            modifier = Modifier
                .offset(
                    x = screenWidth - boxSize - padding,
                    y = (yOffset2 % (totalTravelDistance.value + boxSize.value)).dp
                )
                .graphicsLayer(rotationZ = rotation2))

        SignInScreenCircleImages(message = "Job Meeting",
            dateAndTime ="TODAY, 9:30 PM" ,
            repeatOption = "No Repeat",
            modifier = Modifier
                .offset(
                    x = padding,
                    y = (yOffset3 % (totalTravelDistance.value + boxSize.value)).dp
                )
                .graphicsLayer(rotationZ = rotation1))
        SignInScreenCircleImages(message = "Read Book",
            dateAndTime ="TODAY, 9:30 PM" ,
            repeatOption = "Daily",
            modifier = Modifier
                .offset(
                    x = screenWidth - boxSize - padding,
                    y = (yOffset4 % (totalTravelDistance.value + boxSize.value)).dp
                )
                .graphicsLayer(rotationZ = rotation2))
        // Yellow Box (Right Side, 24.dp from right)

    }

}
@Composable
fun SignInScreenCircleImages(message:String,dateAndTime:String,repeatOption:String,modifier: Modifier){


                Box(
                    modifier = modifier


                        .size(184.dp)
                        .aspectRatio(1f)
                        .clip(CircleShape)
                        .background(MaterialTheme.colors.secondary, shape = CircleShape),
                    contentAlignment = Alignment.Center,
                ) {

                    Box(
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .size(32.dp)

                            .background(
                                color = MaterialTheme.colors.background,
                                shape = CircleShape
                            )
                            .align(Alignment.TopCenter),

                        )



                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .wrapContentHeight(align = Alignment.CenterVertically),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = ("$message"),
                                textAlign = TextAlign.Center,
                                fontFamily = interDisplayFamily,
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp,
                                lineHeight = 20.sp,
                                color = MaterialTheme.colors.primary,
                                style = androidx.compose.ui.text.TextStyle(letterSpacing = 0.5.sp),
                                modifier = Modifier
                                    .padding(start = 16.dp, end = 16.dp),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis


                            )
                            Text(
                                text = dateAndTime.toUpperCase(),
                                fontFamily = interDisplayFamily,
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Normal,
                                fontSize = 11.sp,
                                lineHeight = 16.sp,
                                color = MaterialTheme.colors.primary.copy(alpha = 0.5f),
                                style = androidx.compose.ui.text.TextStyle(letterSpacing = 1.sp),
                                modifier = Modifier
                                    .padding(top = 4.dp, start = 16.dp, end = 16.dp)
                                    .height(15.dp),
                                overflow = TextOverflow.Ellipsis

                            )
                            if (repeatOption in listOf("Daily", "Weekly", "Monthly", "Yearly")) {
                                ThemedRepeatedIconImage(
                                    modifier = Modifier
                                        .padding(top = 8.dp)
                                        .alpha(0.3f)
                                )
                            }
                        }


                    }




    }
}


@Composable
fun ThemedImage(modifier:Modifier) {
    val isDarkTheme = isSystemInDarkTheme()
    val imageRes = if (isDarkTheme) {
        R.drawable.dark_theme_ball
    } else {
        R.drawable.black_tick_ball
    }
    Image(
        painter = painterResource(id = imageRes),
        contentDescription = null,
        modifier = modifier
    )
}