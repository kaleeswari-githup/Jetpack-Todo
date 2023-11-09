@file:OptIn(ExperimentalFoundationApi::class)

package com.example.Pages

import android.annotation.SuppressLint
import android.app.Dialog
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.MediaPlayer
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
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
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.airbnb.lottie.compose.*
import com.example.dothings.*
import com.example.dothings.R
import com.example.dothings.R.DataClass
import com.example.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.*
import java.util.concurrent.TimeUnit

@SuppressLint("UnusedMaterialScaffoldPaddingParameter", "CoroutineCreationDuringComposition",
    "UnrememberedMutableState"
)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun HomeScreen(navController: NavHostController,scale:Float, offset: Dp){
    val scaffoldState = rememberScaffoldState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val database = FirebaseDatabase.getInstance()
    val user = FirebaseAuth.getInstance().currentUser
    val uid = user?.uid
    var context = LocalContext.current
    val sharedPreferences = context.getSharedPreferences("MyAppSettings", Context.MODE_PRIVATE)
    fun getIsChecked(): Boolean {
        return sharedPreferences.getBoolean("isChecked", false)
    }
    val onDeleteClick:(String) -> Unit = {clickedTaskId ->
        val databaseRef = database.reference.child("Task").child(uid.toString())
        val taskRef = database.reference.child("Task").child(uid.toString()).child(clickedTaskId)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(clickedTaskId.hashCode())
        val notificationTag = "Notification_$clickedTaskId"
        Log.d("NotificationTag", notificationTag)
        val workManager = WorkManager.getInstance(context)
        workManager.cancelUniqueWork(notificationTag)
        coroutineScope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            val data = databaseRef.child(clickedTaskId).get().await().getValue(DataClass::class.java)
            if (data != null) {
                databaseRef.child(clickedTaskId).removeValue()
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
                    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    notificationManager.cancel(clickedTaskId.hashCode())
                    val notificationTag = "Notification_$clickedTaskId"
                    Log.d("NotificationTag", notificationTag)
                    val workManager = WorkManager.getInstance(context)
                    workManager.cancelUniqueWork(notificationTag)

                    coroutineScope.launch {
                        snackbarHostState.currentSnackbarData?.dismiss()
                            val result = snackbarHostState.showSnackbar(
                                message = "TASK COMPLETED",
                                actionLabel = "UNDO",
                                duration = SnackbarDuration.Short
                            )
                            when (result) {
                                SnackbarResult.ActionPerformed -> {
                                    //Do Something
                                    taskNewRef.setValue(data)
                                    completedTasksRef.removeValue()

                                }
                                SnackbarResult.Dismissed -> {
                                    //Do Something
                                    completedTasksRef.setValue(data)
                                }

                        }
                      /*  scaffoldState.snackbarHostState.currentSnackbarData?.dismiss()
                        val snackbarResult = scaffoldState.snackbarHostState.showSnackbar(
                            message = "Task Completed",
                            actionLabel = "Undo",
                            duration = SnackbarDuration.Short
                        )
                        when (snackbarResult) {
                            SnackbarResult.Dismissed -> {
                                completedTasksRef.setValue(data)
                            }
                            SnackbarResult.ActionPerformed -> {
                                taskRef.setValue(data)
                                completedTasksRef.removeValue()
                            }
                        }*/
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
    var isAddDaskScreenOpen = remember {
        mutableStateOf(false)
    }
    var isMarkCompletedOpen = remember { mutableStateOf(false) }
    var isUpdatePickerOpen = remember { mutableStateOf(false) }
    var isPickerOpen = remember { mutableStateOf(false) }
    var isAddDaskOpen = remember { mutableStateOf(false) }
    val isChecked = getIsChecked()
    val blurEffectBackground by animateDpAsState(
        targetValue = when {
            selectedItemId.value.isNotEmpty() -> 25.dp
            isAddDaskScreenOpen.value -> 25.dp
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
            Box(modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center){
                Image(painter = painterResource(id = R.drawable.shadowcenter), contentDescription = null,
                    modifier = Modifier
                        .graphicsLayer(alpha = 0.04f)
                        .blur(radius = 90.dp)
                        .align(Alignment.Center))
            }
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center) {

                LazyGridLayout(
                    onMarkCompletedClick = onMarkCompletedClick,
                    onDeleteClick,
                    selectedItemId,
                    isPickerOpen,
                    isAddDaskOpen,
                    scale = scale,
                    offset = offset,
                    isChecked = isCheckedState,
                    isUpdatePickerOpen
                )
            }
            val isDarkTheme = isSystemInDarkTheme()
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
              //  drawRect(brush = gradientBrush)
            }

            Column {
                TopSectionHomeScreen(
                    navController,
                    isMarkCompletedOpen,
                    selectedMarkedItemId,
                    isChecked = isCheckedState,
                    sharedPreferences)
                FloatingActionButton(isAddDaskScreenOpen,isPickerOpen,isCheckedState)
            }
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter),
                snackbar = { CustomSnackbar(it)}
            )
        }
        if (isPickerOpen.value || selectedMarkedItemId.value.isNotEmpty()||isUpdatePickerOpen.value ){
            Box(modifier = Modifier
                .fillMaxSize()
                .background(color = SurfaceGray, shape = RectangleShape)
            )
        }
    }
}
/*@Composable
fun ThemedCanvas() {
    val isDarkTheme = isSystemInDarkTheme()
    val gradientBrush = Brush.verticalGradient(
        colors = if (isDarkTheme) {
            listOf(Color(0xFF222222), Color(0xFF000000))
        } else {
            listOf(Color(0xFFEDEDED), Color(0x00EDEDED))
        },
        startY = 0f,
        endY = 100f
    )
    val opacityBrush = Brush.verticalGradient(
        colors = if (isDarkTheme) {
            listOf(Color(0xFF000000), Color(0xFF222222))
        } else {
            listOf(Color(0x00EDEDED), Color(0xFFEDEDED))
        },
        startY = 0f,
        endY = 100f
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        drawRect(
            brush = gradientBrush,
            topLeft = Offset(0f, 0f),
           // size = Size(size.width, 100.dp.toPx())
        )
        drawRect(
            brush = opacityBrush,
            topLeft = Offset(0f, (size.height - 84.dp.toPx()).coerceAtLeast(0f)),
            size = Size(size.width, size.height)
        )
    }
}*/
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
            ButtonTextWhiteTheme(text = snackbarData.message)
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
fun LazyGridLayout(onMarkCompletedClick: (String) -> Unit,
                   onDeleteClick: (String) -> Unit,
                   selectedItemId: MutableState<String>,
                   isPickerOpen:MutableState<Boolean>,
                   isAddDaskOpen:MutableState<Boolean>,
                   scale:Float,
                   offset: Dp,
                   isChecked: MutableState<Boolean>,
                   isUpdatePickerOpen:MutableState<Boolean>
                   ){
    val database = FirebaseDatabase.getInstance()
    val user = FirebaseAuth.getInstance().currentUser
    val uid = user?.uid
    val databaseRef: DatabaseReference = database.reference.child("Task").child(uid.toString())
    val imageResource = R.drawable.light_square // Resource ID of the image
    var isNotificationSet by remember { mutableStateOf(false) }

    var cardDataList = remember {
        mutableStateListOf<DataClass>()
    }
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
    if (isNotificationSet){
        cardDataList.forEach { data ->
            val selectedDateTime = data.notificationTime
            val itemId = data.id
            val message = data.message
            scheduleNotification(selectedDateTime, itemId, message!!)
        }
    }


   // var animatedItemIndexes by remember { mutableStateOf(emptyList<Int>()) }
    val selectedItemIndex = remember { mutableStateOf(-1) }
    val gridColumns = 2
    if (cardDataList.size > 1) {
        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Fixed(gridColumns),
            verticalItemSpacing = 16.dp,
            contentPadding = PaddingValues(start = 24.dp,end = 24.dp,top = 50.dp),

            ) {
            itemsIndexed(cardDataList.reversed()) { index, cardData ->
                val animationDelay = index
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
             image = imageResource,
             message = cardData.message!!,
             time = cardData.time!!,
             date = formattedDate,
             id = cardData.id,
             animationDelay = animationDelay,
             onMarkCompletedClick = onMarkCompletedClick,
             onDeleteClick = onDeleteClick,
             selectedItemId = selectedItemId,
             isPickerOpen = isPickerOpen,
             isAddDaskOpen = isAddDaskOpen,

             scale = scale,
             offset = offset,
             index = index,
             isChecked = isChecked,
             isUpdatePickerOpen = isUpdatePickerOpen
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
                        image = imageResource,
                        message = cardDataList[0].message!!,
                        time = cardDataList[0].time!!,
                        date = formattedDate,
                        id = cardDataList[0].id,
                        animationDelay = 0,
                        onMarkCompletedClick = onMarkCompletedClick,
                        onDeleteClick = onDeleteClick,
                        selectedItemId = selectedItemId,
                        isPickerOpen = isPickerOpen,
                        isAddDaskOpen = isAddDaskOpen,

                        scale = scale,
                        offset = offset,
                        index = index,
                        isChecked = isChecked,
                        isUpdatePickerOpen = isUpdatePickerOpen
                        )
                }
            }
        }
    }
}



@OptIn(ExperimentalAnimationApi::class)
@SuppressLint("UnrememberedMutableState", "CoroutineCreationDuringComposition",
    "SuspiciousIndentation", "ServiceCast", "WrongConstant"
)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun RoundedCircleCardDesign(
    id:String,
    image:Int,
    message:String,
    time: String,
    date:String,
    animationDelay: Int,
    onMarkCompletedClick: (String) -> Unit,
    onDeleteClick:(String)->Unit,
    selectedItemId: MutableState<String>,
    isPickerOpen:MutableState<Boolean>,
    isAddDaskOpen:MutableState<Boolean>,
    scale:Float,
    offset: Dp,
    index:Int,
    isChecked:MutableState<Boolean>,
    isUpdatePickerOpen:MutableState<Boolean>
    )
{
    val scaffoldState = rememberScaffoldState()
    val infiniteTransition = rememberInfiniteTransition()

    val painter:Painter = painterResource(image)
    val animateX = animationDelay % 2 == 0
    val animateY = animationDelay % 2 != 0
    val coroutineScope = rememberCoroutineScope()
    val mContext = LocalContext.current
    val dx by infiniteTransition.animateFloat(
        initialValue = if (animateX) -0.4f else 0f,
        targetValue = if (animateX) 0.5f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = LinearEasing, delayMillis = animationDelay * 200),
            repeatMode = RepeatMode.Reverse
        )
    )
    val dialogIntent = Intent(mContext, MainActivity::class.java).apply {
        action = "OPEN_UPDATE_TASK_DIALOG"
        putExtra("taskId", id) // You can pass any necessary data
    }

// Create a PendingIntent for the intent
    val pendingIntent = PendingIntent.getActivity(
        mContext,
        0,
        dialogIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    val dy by infiniteTransition.animateFloat(
        initialValue = if (animateY) -0.4f else 0f,
        targetValue = if (animateY) 0.5f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = LinearEasing, delayMillis = animationDelay * 200),
            repeatMode = RepeatMode.Reverse
        )
    )
    val travelDistance = with(LocalDensity.current) { 4.dp.toPx() }
   // val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.exploade_animation))


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
                .offset(y = offset)
                .alpha(scale)
                .aspectRatio(1f)
                .clip(CircleShape)
                .background(MaterialTheme.colors.primary, shape = CircleShape)
                .clickable(indication = null,
                    interactionSource = remember { MutableInteractionSource() }) {
                    selectedItemId.value = id

                    // isAddDaskOpen.value = true
                }
            ,
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
                            }

                            onMarkCompletedClick(id)
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
                    color = MaterialTheme.colors.secondary,
                    style = androidx.compose.ui.text.TextStyle(letterSpacing = 0.sp),
                    modifier = Modifier.padding(top = 4.dp,start = 16.dp,end = 16.dp)
                )
            }
            if(selectedItemId.value == id){
                val updateTaskParams = UpdateTaskScreenParams(
                    selectedDate = mutableStateOf( date) ,
                    selectedTime = mutableStateOf(time) ,
                    textValue = message,
                    id = id,
                    openKeyboard = false,
                    onDismiss = { selectedItemId.value = ""  },
                    onMarkCompletedClick = onMarkCompletedClick,
                    onDeleteClick,
                    isPickerOpen,
                    isAddDaskOpen = isAddDaskOpen,
                    index = index,
                    isChecked = isChecked,
                    isUpdatePickerOpen
                )
                UpdateTaskScreen(updateTaskParams)
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
    val startOfRange = currentDate.plusDays(2) // Day after tomorrow
    val endOfRange = LocalDate.MAX
    return when {
        period.isZero -> "Today"
        period == Period.ofDays(1) -> "Yesterday"
        date == currentDate.plusDays(1) -> "Tomorrow"
        date in startOfRange..endOfRange -> {
            val formatter = if (date.year == currentDate.year) {
                DateTimeFormatter.ofPattern("EEE, d MMM")
            } else {
                DateTimeFormatter.ofPattern("EEE, d MMM yyyy")
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
        else -> date.format(DateTimeFormatter.ofPattern("EEE, d MMM"))
    }
}
@SuppressLint("NewApi")
@RequiresApi(Build.VERSION_CODES.O)
private fun isToday(dateString: String): Boolean {
    val formatter = DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale.ENGLISH)
    val currentDate = LocalDate.now()
    val formattedDate = try {
        LocalDate.parse(dateString, formatter)
    } catch (e: DateTimeParseException) {
        return false
    }
    return formattedDate == currentDate
}


@SuppressLint("UnrememberedMutableState")
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun FloatingActionButton(
    isAddDaskScreenOpen:MutableState<Boolean>,
    isPickerOpen: MutableState<Boolean>,
    isChecked:MutableState<Boolean>){
    val shadowOpacity = 0.4f // Set the desired opacity value (between 0 and 1)
    val shadowColor = Color.Black.copy(alpha = shadowOpacity)
    var visible by remember { mutableStateOf(false) }
     val context = LocalContext.current
    LaunchedEffect(Unit) {
        visible = true
    }
    val offsetYSecond by animateDpAsState(
        targetValue = if (visible) 0.dp else 42.dp,
        animationSpec = tween(
            durationMillis = 400,
            delayMillis = 300,
            easing = EaseOutCirc
        )
    )
    val opacitySecond by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = keyframes {
            durationMillis = 500

            delayMillis = 300// Total duration of the animation
        }
    )
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
                .offset(y = offsetYSecond)
                .alpha(opacitySecond)
                .zIndex(opacitySecond)
                .align(Alignment.BottomCenter)
                .bounceClick()
                    ,
           onClick = {isAddDaskScreenOpen.value = true
                     Vibration(context)
           },
            shape = CircleShape,
           // contentColor = Color.White,
            elevation = FloatingActionButtonDefaults.elevation(0.dp),
            backgroundColor = FABRed,

            contentColor = Color.White.compositeOver(shadowColor)
        ) {
            Image(painterResource(
                id = R.drawable.plus_icon) ,
                contentDescription = "",
            modifier = Modifier)
        }
        val context = LocalContext.current
        val dialog = Dialog(context)
 if(isAddDaskScreenOpen.value){
     AddDaskScreen( selectedDate = mutableStateOf(null),
         selectedTime = mutableStateOf(null),
         textValue = "",
         onDismiss = {isAddDaskScreenOpen.value = false},
         isPickerOpen = isPickerOpen,
         modifier = Modifier,
         isChecked = isChecked
     )
 }
    }

}
enum class ButtonState { Pressed, Idle }
@Composable
fun Modifier.bounceClick() = composed {
    var buttonState by remember { mutableStateOf(ButtonState.Idle) }
    val scale by animateFloatAsState(if (buttonState == ButtonState.Pressed) 0.70f else 1f)
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
@Composable
fun scheduleNotification(selectedDateTime: Long, itemId: String,message: String) {
    val now = Calendar.getInstance().timeInMillis
    var context = LocalContext.current
    val notificationTag = "Notification_$itemId"
    if (selectedDateTime > now) {
        val inputData = workDataOf(
            "itemId" to itemId,
            "messageExtra" to message// Replace with your desired message
        )
        val notificationWorkRequest = OneTimeWorkRequest.Builder(NotificationWorker::class.java)
            .setInputData(inputData)
            .setInitialDelay(selectedDateTime - now, TimeUnit.MILLISECONDS)
            .addTag(notificationTag)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(notificationTag, ExistingWorkPolicy.REPLACE, notificationWorkRequest)
    }
   // Toast.makeText(context, "Alarm set", Toast.LENGTH_SHORT).show()
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
            durationMillis = 700,
            easing = EaseOutCirc
        )
        )
    val offsetYSecond by animateDpAsState(
        targetValue = if (visible) 0.dp else -52.dp,
        animationSpec = tween(
            durationMillis = 700,
            delayMillis = 300,
            easing = EaseOutCirc
        )
    )
    val opacity by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = keyframes {
            durationMillis = 700 // Total duration of the animation
        }
    )
    val opacitySecond by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = keyframes {
            durationMillis = 700
        delayMillis = 300// Total duration of the animation
        }
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
            modifier = Modifier
                .offset(y = offsetY)
                .alpha(opacity),
            fontFamily = interDisplayFamily,
            fontWeight = FontWeight.W100,
            fontSize = 24.sp,
            color = MaterialTheme.colors.secondary,

        )
        Box(modifier = Modifier
            .size(48.dp)
            .offset(y = offsetYSecond)
            .alpha(opacitySecond)
            .clickable(indication = null,
                interactionSource = remember { MutableInteractionSource() }) {
                isMarkCompletedOpen.value = true
                Vibration(context)
            }
            .clip(shape)
            .background(color = MaterialTheme.colors.primary),
            contentAlignment = Alignment.Center

        ) {
           Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
               Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                   Box(modifier = Modifier.size(4.dp)
                       .background(shape = CircleShape,color = FABRed))
                   Box(modifier = Modifier.size(4.dp)
                       .background(color = MaterialTheme.colors.background,shape = CircleShape))
               }
               Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                   Box(modifier = Modifier.size(4.dp)
                       .background(shape = CircleShape,color = MaterialTheme.colors.background))
                   Box(modifier = Modifier.size(4.dp)
                       .background(color = MaterialTheme.colors.background,shape= CircleShape))
               }
           }
        }
        if (isMarkCompletedOpen.value){
            MarkCompletedScreen(
                navController = navController,
                onDismiss = { isMarkCompletedOpen.value = false },
                selectedMarkedItemId,
                isChecked,
                sharedPreferences = sharedPreferences
            )
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