@file:OptIn(ExperimentalFoundationApi::class)

package com.firstyogi.dothing

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.annotation.RequiresApi
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.app.ComponentActivity
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.firstyogi.dothing.*
import com.firstyogi.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter
import java.util.*

@SuppressLint("UnusedMaterialScaffoldPaddingParameter", "CoroutineCreationDuringComposition",
    "UnrememberedMutableState", "RestrictedApi"
)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun HomeScreen(navController: NavController,snackbarHostState:SnackbarHostState,coroutineScope:CoroutineScope){
    val scaffoldState = rememberScaffoldState()
    val database = FirebaseDatabase.getInstance()
    val user = FirebaseAuth.getInstance().currentUser
    val uid = user?.uid
    var context = LocalContext.current
    val isDarkTheme = isSystemInDarkTheme()
    val sharedPreferences = context.getSharedPreferences("MyAppSettings", Context.MODE_PRIVATE)
    fun getIsChecked(): Boolean {
        return sharedPreferences.getBoolean("isChecked", true)
    }
    BackHandler {
        (context as? ComponentActivity)?.finish()
    }


    val onDeleteClick:(String) -> Unit = {clickedTaskId ->
        val databaseRef = database.reference.child("Task").child(uid.toString())
        val taskRef = database.reference.child("Task").child(uid.toString()).child(clickedTaskId)

        coroutineScope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            val data = databaseRef.child(clickedTaskId).get().await().getValue(DataClass::class.java)

            if (data != null) {
                databaseRef.child(clickedTaskId).removeValue()
                cancelNotification(context, clickedTaskId)
                val snackbarResult = snackbarHostState.showSnackbar(
                    message = "TASK DELETED",
                    actionLabel = "UNDO",
                    duration = SnackbarDuration.Short
                )
                when (snackbarResult) {
                    SnackbarResult.Dismissed -> {
                        databaseRef.child(clickedTaskId).removeValue()
                    }
                    SnackbarResult.ActionPerformed -> {
                        taskRef.setValue(data)
                    }
                }
            }
        }

    }
    DisposableEffect(Unit) {
        onDispose {
            coroutineScope.coroutineContext.cancelChildren()
        }
    }
    val onMarkCompletedClick: (String) -> Unit = { clickedTaskId ->
        val taskRef = database.reference.child("Task").child(uid.toString()).child(clickedTaskId)
        val taskNewRef = database.reference.child("Task").child(uid.toString()).push()
        var completedTasksRef = database.reference.child("Task").child("CompletedTasks").child(uid.toString()).push()

        taskRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val data = snapshot.getValue(DataClass::class.java)
                if (data != null) {
                    taskRef.removeValue()
                    completedTasksRef.setValue(data)
                    cancelNotification(context, data.id)
                    cancelNotificationManger(context, data.id)
                    coroutineScope.launch {
                        snackbarHostState.currentSnackbarData?.dismiss()
                        val result = snackbarHostState.showSnackbar(
                            message = "TASK COMPLETED",
                            actionLabel = "UNDO",
                            duration = SnackbarDuration.Short
                        )
                        when (result) {
                            SnackbarResult.ActionPerformed -> {
                                taskNewRef.setValue(data)
                                completedTasksRef.removeValue()
                            }
                            SnackbarResult.Dismissed -> {
                                completedTasksRef.setValue(data)
                            }
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseError", "Database operation cancelled: $error")
            }
        })
    }

    val selectedItemId = remember { mutableStateOf("") }
    val selectedMarkedItemId = remember { mutableStateOf("") }
    var isMarkCompletedOpen = remember { mutableStateOf(false) }
    var isUpdatePickerOpen = remember { mutableStateOf(false) }
   var isPickerOpen = remember { mutableStateOf(false) }
    var isAddDaskOpen = remember { mutableStateOf(false) }
    val isChecked = getIsChecked()

    val completedTasksCountState = remember { mutableStateOf(0) }
   val blurEffectBackground by animateDpAsState(
        targetValue = when {
            isMarkCompletedOpen.value -> 25.dp
            else -> 0.dp
        }
   )

    val isCheckedState = mutableStateOf(isChecked)
    Scaffold(
        scaffoldState = scaffoldState,
        modifier = Modifier.fillMaxSize(),
        backgroundColor = Color.Transparent,
        ){
        BoxWithConstraints(modifier = Modifier
            .fillMaxSize()
            .background(color = MaterialTheme.colors.background)
            .blur(radius = blurEffectBackground)
        ){
            ThemedGridImage()
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center) {
                val completedTasksCount = completedTasksCountState.value
                LazyGridLayout(
                    navController = navController,
                    onMarkCompletedClick = onMarkCompletedClick,
                    selectedItemId,
                    isChecked = isCheckedState,
                    )
            }

            Canvas(modifier = Modifier.fillMaxSize()) {

                val gradientBrush = Brush.verticalGradient(
                    colors = if(isDarkTheme){
                        listOf(
                            Color(0xFF000000),
                            Color(0x00000000)
                        )
                    }else{
                        listOf(
                            Color(0xFFEDEDED),
                            Color(0x00EDEDED)
                        )
                    }
                    ,
                    startY = 0f,
                    endY = size.height.coerceAtMost(100.dp.toPx())
                )
                val opacityBrush = Brush.verticalGradient(
                    colors = if (isDarkTheme){
                        listOf(
                            Color(0x00000000),
                            Color(0xFF000000)
                        )
                    }else{
                        listOf(
                            Color(0x00EDEDED),
                            Color(0xFFEDEDED)
                        )
                    }
                    ,
                    startY = (size.height - 84.dp.toPx()).coerceAtLeast(0f),
                    endY = size.height
                )
               drawRect(brush = gradientBrush)
                drawRect(brush = opacityBrush)
            }

            Column {
                TopSectionHomeScreen(
                    navController,
                    isMarkCompletedOpen,
                    selectedMarkedItemId,
                    isChecked = isCheckedState,
                    sharedPreferences)
                FloatingActionButton(
                    navController = navController,
                    isChecked = isCheckedState
                )
            }
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter),
                snackbar = { CustomSnackbar(it)}
            )
        }
    }
}

@Composable
fun CustomSnackbar(snackbarData: SnackbarData) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colors.primary,
        modifier = Modifier
            .fillMaxWidth()
            .height(82.dp)
            .padding(16.dp)
            .shadow(
                elevation = 48.dp,
                spotColor = Color(0x40000000),
                ambientColor = Color(0x40000000)
            )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start= 24.dp,end = 24.dp)
        ) {
            ButtonTextWhiteTheme(text = snackbarData.message,color = MaterialTheme.colors.secondary)
            Spacer(Modifier.weight(1f))
            snackbarData.actionLabel?.let { actionLabel ->
                TextButton(onClick = { snackbarData.performAction() }) {
                    Text(text = actionLabel,
                        color = FABRed,
                        fontFamily = interDisplayFamily,
                        fontSize = 15.sp,
                        style = androidx.compose.ui.text.TextStyle(letterSpacing = 1.sp),
                        fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}
@OptIn(ExperimentalFoundationApi::class)
@SuppressLint("MissingPermission")
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun LazyGridLayout(navController: NavController,
    onMarkCompletedClick: (String) -> Unit,
                   selectedItemId: MutableState<String>,
                   isChecked: MutableState<Boolean>,
                   ){
    val database = FirebaseDatabase.getInstance()
    val user = FirebaseAuth.getInstance().currentUser
    val uid = user?.uid
    val databaseRef: DatabaseReference = database.reference.child("Task").child(uid.toString())
    var isNotificationSet by remember { mutableStateOf(false) }
    val context = LocalContext.current

    var cardDataList = remember {
        mutableStateListOf<DataClass>()
    }
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(100)
        visible = true
    }
    val offsetYSecond by animateDpAsState(
        targetValue = if (visible) 0.dp else 0.dp,

    )
    val opacitySecond by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = keyframes {
            delayMillis = 30// Total duration of the animation
        }
    )
    LaunchedEffect(Unit){
        val valueEventListener = object :ValueEventListener{
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                cardDataList.clear()
                for(childSnapshot in dataSnapshot.children){
                    val id = childSnapshot.key.toString()
                    val data = childSnapshot.getValue(DataClass::class.java)
                    data?.let {
                        cardDataList.add(it.copy(id = id))
                        isNotificationSet = true
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseError", "Database operation cancelled: $error")
            }
        }
        databaseRef.addValueEventListener(valueEventListener)
    }
    val MY_PERMISSIONS_REQUEST_SCHEDULE_ALARM = 123
    if (isNotificationSet){
        cardDataList.forEach { data ->
            var selectedDateTime = data.notificationTime
            val itemId = data.id
            val message = data.message
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // Use LocalContext.current to get the current context in Compose
                val currentContext = LocalContext.current

                if (ContextCompat.checkSelfPermission(
                        currentContext,
                        android.Manifest.permission.SCHEDULE_EXACT_ALARM
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    // Permission is not granted, request it
                    ActivityCompat.requestPermissions(
                        LocalContext.current as Activity,
                        arrayOf(android.Manifest.permission.SCHEDULE_EXACT_ALARM),
                        MY_PERMISSIONS_REQUEST_SCHEDULE_ALARM
                    )
                } else {
                    // Permission is already granted, proceed with the operation
                    scheduleNotification(currentContext, selectedDateTime, itemId, message!!, isCheckedState = isChecked.value)
                }
            } else {
                // For versions lower than Android 6.0, no runtime permission is needed
                scheduleNotification(context, selectedDateTime, itemId, message!!, isCheckedState = isChecked.value)
            }

            scheduleNotification(context = context,selectedDateTime,itemId,message!!, isCheckedState = isChecked.value)
        }
    }
    val gridColumns = 2
    if (cardDataList.isNotEmpty()){
        if (cardDataList.size > 1) {
            LazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Fixed(gridColumns),
                verticalItemSpacing = 16.dp,
                contentPadding = PaddingValues(start = 24.dp,end = 24.dp,top = 50.dp),
                ) {
                itemsIndexed(cardDataList.reversed()) { index, cardData ->
                    val originalDateFormat = DateTimeFormatter.ofPattern("MM/dd/yyyy")
                    val desiredDateFormat = DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale.ENGLISH)
                    val dateStringFromDatabase = cardData.date
                    val formattedDate = if (dateStringFromDatabase!!.isNotEmpty()) {
                        val originalDate = LocalDate.parse(dateStringFromDatabase, originalDateFormat)
                        originalDate.format(desiredDateFormat)
                    } else {
                        ""
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(
                                top = if (index == 1) 100.dp else 0.dp,
                                bottom = if (index == cardDataList.size - 1) 90.dp else 0.dp
                            ),
                        contentAlignment = Alignment.Center
                    ) {

                        RoundedCircleCardDesign(
                            message = cardData.message!!,
                            time = cardData.time!!,
                            date = formattedDate,
                            id = cardData.id,
                            onMarkCompletedClick = onMarkCompletedClick,
                            selectedItemId = selectedItemId,
                            isChecked = isChecked,
                            navController = navController,
                            )
                    }
                }
            }
        }else if (cardDataList.size == 1) {
            LazyColumn(){
                itemsIndexed(cardDataList){ index,cardData ->
                    Box(modifier = Modifier
                        .fillMaxSize()
                        .size(300.dp)
                        .padding(bottom = 50.dp)
                        , contentAlignment = Alignment.Center) {
                        val originalDateFormat = DateTimeFormatter.ofPattern("MM/dd/yyyy")
                        val desiredDateFormat = DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale.ENGLISH)
                        val dateStringFromDatabase = cardData.date
                        val formattedDate = if (dateStringFromDatabase!!.isNotEmpty()) {
                            val originalDate = LocalDate.parse(dateStringFromDatabase, originalDateFormat)
                            originalDate.format(desiredDateFormat)
                        } else {
                            ""
                        }
                        RoundedCircleCardDesign(
                            message = cardDataList[0].message!!,
                            time = cardDataList[0].time!!,
                            date = formattedDate,
                            id = cardDataList[0].id,
                            onMarkCompletedClick = onMarkCompletedClick,
                            selectedItemId = selectedItemId,
                            isChecked = isChecked,
                            navController = navController,
                            )
                    }
                }
            }
        }
    }else{
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(
                bottom = 216.dp,
                start = 50.dp, end = 50.dp
            )
            .offset(y = offsetYSecond)
            .alpha(opacitySecond),
            contentAlignment = Alignment.BottomCenter
        ){
            Text(text =  "Focus on what's essential. \n" +
                    "Limit your priorities to three to achieve more.",
                textAlign = TextAlign.Center,
                fontFamily = interDisplayFamily,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colors.secondary.copy(alpha = 0.75f),
                lineHeight = 20.sp
            )
        }
    }

}
@SuppressLint("UnrememberedMutableState", "CoroutineCreationDuringComposition",
    "SuspiciousIndentation", "ServiceCast", "WrongConstant"
)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun RoundedCircleCardDesign(
    navController: NavController,
    id:String?,
    message:String,
    time: String,
    date:String,
    onMarkCompletedClick: (String) -> Unit,
    selectedItemId: MutableState<String>,
    isChecked:MutableState<Boolean>,
    )
{
    val coroutineScope = rememberCoroutineScope()
    val mContext = LocalContext.current
    val formatter = DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale.ENGLISH)
    val dateString = if (date.isNotEmpty()) {
        val parsedDate = LocalDate.parse(date,formatter)
        if (time.isNotEmpty()){
            "${formatDate(parsedDate)}, $time"
        }else{
            "${formatDate(parsedDate)}"
        }

    } else {
        ""
    }
    Box(
        modifier = Modifier
            .size(184.dp)
            .aspectRatio(1f)
            .bounceClick()
            .clip(CircleShape)
            .background(MaterialTheme.colors.primary, shape = CircleShape)
            .clickable(indication = null,
                interactionSource = remember { MutableInteractionSource() }) {
                navController.navigate(
                    route = Screen.Update.passUpdateValues(
                        id = id.toString(),
                        isChecked = isChecked.value,
                        )
                ) },
        contentAlignment = Alignment.Center,
    ) {
        Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ThemedSquareImage(
                    modifier = Modifier
                        .padding(top = 32.dp)
                        .clickable(indication = null,
                            interactionSource = remember { MutableInteractionSource() }) {
                            if (isChecked.value) {
                                coroutineScope.launch(Dispatchers.IO) {
                                    val mMediaPlayer =
                                        MediaPlayer.create(mContext, R.raw.tab_button)
                                    mMediaPlayer.start()
                                    delay(mMediaPlayer.duration.toLong())
                                    mMediaPlayer.release()

                                }
                                Vibration(context = mContext)
                            }
                            onMarkCompletedClick(id.toString())
                        })
                Text(
                    text = ("$message"),
                    textAlign = TextAlign.Center,
                    fontFamily = interDisplayFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp,
                    color = MaterialTheme.colors.secondary,
                    style = androidx.compose.ui.text.TextStyle(letterSpacing = 0.sp),
                    modifier = Modifier.padding(top = 24.dp,start = 16.dp,end = 16.dp)
                )
                Text(
                    text =dateString,
                    fontFamily = interDisplayFamily,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Normal,
                    fontSize = 11.sp,
                    color = MaterialTheme.colors.secondary.copy(alpha = 0.75f),
                    style = androidx.compose.ui.text.TextStyle(letterSpacing = 0.sp),
                    modifier = Modifier.padding(top = 4.dp,start = 16.dp,end = 16.dp)
                )
            }
    }

}
@RequiresApi(Build.VERSION_CODES.O)
fun formatDate(date: LocalDate): String {
    val currentDate = LocalDate.now()
    val period = Period.between(date, currentDate)

    val yearsAgo = period.years
    val monthsAgo = period.months
    val daysAgo = period.days
    val startOfRange = currentDate.plusDays(2)
    val endOfRange = LocalDate.MAX
    return when {
        period.isZero -> "Today"
        period == Period.ofDays(1) -> "Yesterday"
        date == currentDate.plusDays(1) -> "Tomorrow"
        date in startOfRange..endOfRange -> {
            val formatter = if (date.year == currentDate.year) {
                DateTimeFormatter.ofPattern("EEE, d MMM",Locale.ENGLISH)
            } else {
                DateTimeFormatter.ofPattern("EEE, d MMM yyyy",Locale.ENGLISH)
            }
            date.format(formatter)
        }// Check if the date is the next day
        yearsAgo == 0 && monthsAgo == 0 && daysAgo < 7 -> "$daysAgo days ago"
        yearsAgo == 0 && monthsAgo == 0 && daysAgo < 14 -> "1 week ago"
        yearsAgo == 0 && monthsAgo == 0 -> "${daysAgo / 7} weeks ago"
        yearsAgo == 0 && monthsAgo == 1 -> "1 month ago"
        yearsAgo == 0 && monthsAgo > 1 -> "$monthsAgo months ago"
        yearsAgo == 1 -> "1 year ago"
        yearsAgo > 1 -> "$yearsAgo years ago"
        else -> date.format(DateTimeFormatter.ofPattern("EEE, d MMM",Locale.ENGLISH))
    }
}
@SuppressLint("UnrememberedMutableState")
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun FloatingActionButton(
    navController: NavController,
    isChecked:MutableState<Boolean>
){
    var isButtonProcessing by remember { mutableStateOf(false) }
    val shadowOpacity = 0.4f // Set the desired opacity value (between 0 and 1)
    val shadowColor = Color.Black.copy(alpha = shadowOpacity)
    var visible by remember { mutableStateOf(false) }
     val context = LocalContext.current

    val coroutineScope = rememberCoroutineScope()
    val mContext = LocalContext.current
    Box(modifier = Modifier
        .fillMaxWidth()
        .fillMaxHeight()
        .padding(bottom = 64.dp)
        .background(Color.Transparent)
    ) {
        androidx.compose.material.FloatingActionButton(
            modifier = Modifier
                .size(96.dp)
                .align(Alignment.BottomCenter)
                .bounceClick()
                    ,
           onClick = {
               if (!isButtonProcessing) {
                   isButtonProcessing = true
                   coroutineScope.launch {
                       navController.navigate(route = "Screen.AddDask.route/${isChecked.value}")
                       Vibration(context)
                   }

               }
           },
            shape = CircleShape,
            elevation = FloatingActionButtonDefaults.elevation(0.dp),
            backgroundColor = FABRed,
            contentColor = Color.White.compositeOver(shadowColor)
        ) {
            Image(painterResource(
                id = R.drawable.plus_icon) ,
                contentDescription = "",
            modifier = Modifier)
        }
    }
}
enum class ButtonState { Pressed, Idle }
@Composable
fun Modifier.bounceClick() = composed {
    var buttonState by remember { mutableStateOf(ButtonState.Idle) }
    val scale by animateFloatAsState(if (buttonState == ButtonState.Pressed) 0.90f else 1f)
    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = { }
        )
        .pointerInput(buttonState) {
            awaitPointerEventScope {
                buttonState = if (buttonState == ButtonState.Pressed) {
                    waitForUpOrCancellation()
                    ButtonState.Idle
                } else {
                    awaitFirstDown(false)
                    ButtonState.Pressed
                }
            }
        }
}

@SuppressLint("ScheduleExactAlarm")
@Composable
fun scheduleNotification(context: Context, selectedDateTime: Long, itemId: String, message: String,isCheckedState: Boolean) {
    val now = Calendar.getInstance().timeInMillis
    val notificationTag = "Notification_$itemId"

    if (selectedDateTime > now) {
        val intent = Intent(context, NotificationReceiver::class.java)
        intent.putExtra("itemId", itemId)
        intent.putExtra("messageExtra", message)
        intent.putExtra("isCheckedState",isCheckedState)

        Log.d("messageExtra","$message")
        val requestCode = notificationTag.hashCode()

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
        )
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.setExact(
            AlarmManager.RTC_WAKEUP,
            selectedDateTime,
            pendingIntent
        )
    }
}

fun cancelNotificationManger(context: Context, itemId: String ){
    val notificationId = notificationIdsMap[itemId]

    if (notificationId != null) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(notificationId)
    }
}
fun cancelNotification(context: Context, itemId: String) {
    val notificationTag = "Notification_$itemId"
    val intent = Intent(context, NotificationReceiver::class.java)
    val requestCode = notificationTag.hashCode()
    val pendingIntent = PendingIntent.getBroadcast(
        context,
        requestCode,
        intent,
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
    )

    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    alarmManager.cancel(pendingIntent)
}
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun TopSectionHomeScreen(navController: NavController,
                         isMarkCompletedOpen:MutableState<Boolean>,
                         selectedMarkedItemId: MutableState<String>,
                         isChecked:MutableState<Boolean>,
                         sharedPreferences: SharedPreferences){
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        visible = true
    }
    val context = LocalContext.current
    val offsetY by animateDpAsState(
        targetValue = if (visible) 0.dp else -52.dp,
        animationSpec = tween(
            durationMillis = 500,
            delayMillis = 300,
            easing = EaseOutCirc
        )
        )

    Row(modifier = Modifier
        .fillMaxWidth()

        .padding(top = 12.dp, start = 24.dp, end = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        val shape = RoundedCornerShape(16.dp)
        Text(
            text = "DOTHING",
            modifier = Modifier,
            fontFamily = interDisplayFamily,
            fontWeight = FontWeight.W100,
            fontSize = 24.sp,
            color = MaterialTheme.colors.secondary,

            )
        Box(modifier = Modifier
            .size(48.dp)
            .clickable(indication = null,
                interactionSource = remember { MutableInteractionSource() }) {
                navController.navigate(route = "Screen.MarkComplete.route/${isChecked.value}")
                Vibration(context)
            }
            .clip(shape)
            .background(color = MaterialTheme.colors.primary),
            contentAlignment = Alignment.Center
        ) {
           Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
               Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                   Box(modifier = Modifier
                       .size(4.dp)
                       .background(shape = CircleShape, color = FABRed))
                   Box(modifier = Modifier
                       .size(4.dp)
                       .background(color = MaterialTheme.colors.background, shape = CircleShape))
               }
               Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                   Box(modifier = Modifier
                       .size(4.dp)
                       .background(shape = CircleShape, color = MaterialTheme.colors.background))
                   Box(modifier = Modifier
                       .size(4.dp)
                       .background(color = MaterialTheme.colors.background, shape = CircleShape))
               }
           }
        }
    }
}

@Composable
fun ThemedSquareImage(modifier: Modifier) {
    val isDarkTheme = isSystemInDarkTheme()

    val imageRes = if (isDarkTheme) {
        R.drawable.dark_square
    } else {
        R.drawable.light_square
    }

    Image(
        painter = painterResource(id = imageRes),
        contentDescription = null,
        modifier = modifier

    )

}
fun Vibration(context:Context){
    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    val vibrationEffect2: VibrationEffect
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        vibrationEffect2 =
            VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
        vibrator.cancel()
        vibrator.vibrate(vibrationEffect2)
    }
}
fun cancelAllNotifications(context: Context, uid: String) {
    val database = FirebaseDatabase.getInstance()
    val taskRef = database.reference.child("Task").child(uid)

    // Read the data from the Firebase Realtime Database
    taskRef.addListenerForSingleValueEvent(object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            // Iterate through each child item
            for (taskSnapshot in snapshot.children) {
                val itemId = taskSnapshot.key

                // Cancel notification for the current child item
                cancelNotification(context, itemId.toString())
                cancelNotificationManger(context,itemId.toString())
            }
        }

        override fun onCancelled(error: DatabaseError) {
            // Handle the error, if any
            Log.e("Firebase", "Error reading data from Firebase", error.toException())
        }
    })
}
