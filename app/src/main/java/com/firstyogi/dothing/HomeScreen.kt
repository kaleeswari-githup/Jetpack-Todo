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
import android.graphics.drawable.GradientDrawable
import android.health.connect.datatypes.units.Velocity
import android.media.MediaPlayer
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.annotation.RequiresApi
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.toUpperCase
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.app.ComponentActivity
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.firstyogi.dothing.*
import com.firstyogi.ui.theme.*
import com.google.api.Distribution.BucketOptions.Linear
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.Period
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@OptIn(ExperimentalSharedTransitionApi::class)
@SuppressLint("UnusedMaterialScaffoldPaddingParameter", "CoroutineCreationDuringComposition",
    "UnrememberedMutableState", "RestrictedApi", "SuspiciousIndentation",
    "UnusedBoxWithConstraintsScope"
)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun HomeScreen(
    animatedVisibilityScope: AnimatedVisibilityScope,
    navController: NavController,
    snackbarHostState:SnackbarHostState,
    coroutineScope:CoroutineScope,
    sharedTransitionScope: SharedTransitionScope,
    modifier: Modifier){
    val scaffoldState = rememberScaffoldState()
    val database = FirebaseDatabase.getInstance()
    val user = FirebaseAuth.getInstance().currentUser
    val uid = user?.uid
    var context = LocalContext.current



    val sharedPreferences = context.getSharedPreferences("MyAppSettings", Context.MODE_PRIVATE)

    fun getIsChecked(): Boolean {
        return sharedPreferences.getBoolean("isChecked", true)
    }
    val isChecked = getIsChecked()
    val isCheckedState = mutableStateOf(isChecked)
    val selectedItemId = remember { mutableStateOf("") }
    val selectedMarkedItemId = remember { mutableStateOf("") }
    var isMarkCompletedOpen = remember { mutableStateOf(false) }


    BackHandler {
        (context as? ComponentActivity)?.finish()
    }


    val onMarkCompletedClick: (String) -> Unit = { clickedTaskId ->
        val taskRef = database.reference.child("Task").child(uid.toString()).child(clickedTaskId)
        val taskNewRef = database.reference.child("Task").child(uid.toString()).child(clickedTaskId)
        var completedTasksRef = database.reference.child("Task").child("CompletedTasks").child(uid.toString()).child(clickedTaskId)

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
                            message = "Task completed",
                            actionLabel = "Undo",
                            duration = SnackbarDuration.Short
                        )
                        when (result) {
                            SnackbarResult.ActionPerformed -> {
                               // data.date = data.userSelectedFirstDate
                                taskNewRef.setValue(data)
                                completedTasksRef.removeValue()

                            }
                            SnackbarResult.Dismissed -> {

                                completedTasksRef.setValue(data)
                            }
                        }

                    }
                    GlobalScope.launch(Dispatchers.Main) {
                        TodoWidget.updateWidgets(context)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseError", "Database operation cancelled: $error")
            }
        })
    }
//
    DisposableEffect(Unit) {
        onDispose {
            snackbarHostState.currentSnackbarData?.dismiss()
        }
    }


    val completedTasksCountState = remember { mutableStateOf(0) }
   val blurEffectBackground by animateDpAsState(
        targetValue = when {
            isMarkCompletedOpen.value -> 25.dp
            else -> 0.dp
        }
   )
    val currentDateNow = LocalDate.now()
    val currentDate = LocalDate.now().format(DateTimeFormatter.ofPattern("MM/dd/yyyy"))
    val formattedDate = currentDateNow.format(DateTimeFormatter.ofPattern("EEEE, MMM d", Locale.ENGLISH)).toUpperCase()
    var isNotificationSet = remember { mutableStateOf(false) }
    val databaseRef: DatabaseReference = database.reference.child("Task").child(uid.toString())
    var cardDataList = remember {
        mutableStateListOf<DataClass>()
    }

    val isDarkTheme = isSystemInDarkTheme()
    val todayTasks = cardDataList.filter { it.date == currentDate ||it.date.isNullOrEmpty() || it.date!! <= currentDate }
    val upcomingTasks = cardDataList.filter { it.date!! > currentDate }
    val todayTaskCount = todayTasks.size
    val upcomingTaskCount = upcomingTasks.size
    LaunchedEffect(Unit){
        val valueEventListener = object :ValueEventListener{
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                cardDataList.clear()
                for(childSnapshot in dataSnapshot.children){
                    val id = childSnapshot.key.toString()
                    val data = childSnapshot.getValue(DataClass::class.java)
                    data?.let {
                        cardDataList.add(it.copy(id = id))
                        isNotificationSet.value = true
                        updateTaskInFirebase(data = data, repeatOption = data.repeatedTaskTime!!,context = context)
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseError", "Database operation cancelled: $error")
            }
        }
        databaseRef.addValueEventListener(valueEventListener)
    }

    val overscrollOffset = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    // Custom NestedScrollConnection to handle overscroll
    Scaffold(
        scaffoldState = scaffoldState,
        modifier = modifier.fillMaxSize()
          ,
        backgroundColor = Color.Transparent,
        ){
        BoxWithConstraints(modifier = Modifier
            .fillMaxSize()

            .background(color = MaterialTheme.colors.background)
            .blur(radius = blurEffectBackground)
        ){
           // val listState = rememberLazyListState()
            val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)
            if (cardDataList.isNotEmpty()){
            LazyColumn(modifier = Modifier
                .fillMaxSize(),
                state = listState,
                horizontalAlignment = Alignment.CenterHorizontally){
                if (todayTasks.isNotEmpty()){
                    item{

                        Box(
                            modifier = Modifier
                               // .fillMaxWidth()
                                .wrapContentHeight()
                               // .background(color = Color.Green)
                            ,
                            contentAlignment = Alignment.Center) {
                            val completedTasksCount = completedTasksCountState.value

                            Column(modifier = Modifier
                                //  .fillMaxHeight()
                                .padding(top = 144.dp)
                                ,
                                horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Today",
                                    modifier = Modifier,
                                    color = MaterialTheme.colors.primary,
                                    fontFamily = interDisplayFamily,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp,
                                    letterSpacing = 0.5.sp
                                )
                                Text(text = formattedDate,
                                    color = MaterialTheme.colors.primary.copy(alpha = 0.5f),
                                    fontFamily = interDisplayFamily,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    letterSpacing = 1.sp,
                                    modifier = Modifier.padding(top = 2.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 1000.dp) // Restrict height to avoid conflicts
                                ) {
                                    LazyGridLayout(
                                        navController = navController,
                                        onMarkCompletedClick = onMarkCompletedClick,
                                        selectedItemId,
                                        isChecked = isCheckedState,
                                        animatedVisibilityScope = animatedVisibilityScope,
                                        sharedTransitionScope = sharedTransitionScope,
                                        cardDataList = todayTasks,
                                        isNotificationSet = isNotificationSet
                                    )
                                }

                            }

                        }
                    }
                }
               if (upcomingTasks.isNotEmpty()){
                   item{

                       Box(
                           modifier = Modifier
                               .wrapContentHeight()

                              // .background(color = Color.Blue)
                           ,
                           contentAlignment = Alignment.Center) {
                           val completedTasksCount = completedTasksCountState.value

                           Column(modifier = Modifier
                               //  .fillMaxHeight()
                               .padding(top = if (todayTasks.isEmpty()) 144.dp else 0.dp )
                               ,
                               horizontalAlignment = Alignment.CenterHorizontally) {
                               Text("UPCOMING",
                                   modifier = Modifier,
                                   color = MaterialTheme.colors.primary.copy(alpha = 0.5f),
                                   fontFamily = interDisplayFamily,
                                   fontWeight = FontWeight.Medium,
                                   fontSize = 11.sp,
                                   letterSpacing = 1.sp)

                               Box(
                                   modifier = Modifier
                                       .fillMaxWidth()
                                       .heightIn(max = 1000.dp) // Restrict height to avoid conflicts
                               ) {
                                   LazyGridLayout(
                                       navController = navController,
                                       onMarkCompletedClick = onMarkCompletedClick,
                                       selectedItemId,
                                       isChecked = isCheckedState,
                                       animatedVisibilityScope = animatedVisibilityScope,
                                       sharedTransitionScope = sharedTransitionScope,

                                       cardDataList = upcomingTasks,
                                       isNotificationSet = isNotificationSet
                                   )
                               }

                           }

                       }
                   }
               }

                }

            }else {
                // Show the empty state text only when cardDataList is empty
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 200.dp, start = 50.dp, end = 50.dp)
                    ,
                    contentAlignment = Alignment.BottomCenter
                ) {
                    val infiniteTransition = rememberInfiniteTransition()

                    val animationOffset by infiniteTransition.animateFloat(
                        initialValue = -450f,
                        targetValue = 450f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(durationMillis = 5000, easing = LinearEasing),
                            repeatMode = RepeatMode.Restart
                        )
                    )

                    // Function to rotate a point (x, y) by angle (in radians)
                    fun rotate(x: Float, y: Float, angle: Float): Pair<Float, Float> {
                        val rad = Math.toRadians(angle.toDouble())
                        val cos = cos(rad).toFloat()
                        val sin = sin(rad).toFloat()
                        return Pair(
                            x * cos - y * sin,
                            x * sin + y * cos
                        )
                    }

// Create a rotated horizontal gradient
                    val angle = -30f // degrees
                    val start = rotate(animationOffset, 0f, angle)
                    val end = rotate(animationOffset + 472f, 0f, angle)

                    val fadeBrush = Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colors.primary.copy(alpha = 0.3f),
                            MaterialTheme.colors.primary.copy(alpha = 0.5f),
                            MaterialTheme.colors.primary.copy(alpha = 0.3f)
                        ),
                        start = Offset(start.first, start.second),
                        end = Offset(end.first, end.second)
                    )

                    Text(
                        text = buildAnnotatedString {
                            withStyle(style = SpanStyle(brush = fadeBrush)) {
                                append("Focus on what's essential. \nLimit your priorities to three \nto achieve more.")
                            }
                        },
                        style = MaterialTheme.typography.body1,
                        modifier = Modifier.padding(horizontal = 24.dp),


                    textAlign = TextAlign.Center,
                        fontFamily = interDisplayFamily,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.Unspecified,
                        lineHeight = 24.sp,

                    )
                }
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
                            Color(0xFFD8DFE7),
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
                            Color(0x00D8DFE7),
                            Color(0xFFD8DFE7)
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
                    sharedPreferences,
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = animatedVisibilityScope,
                    todayTaskCount = todayTaskCount,
                    upcomingTaskCount = upcomingTaskCount
                    )
                FloatingActionButton(
                    navController = navController,
                    isChecked = isCheckedState,
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = animatedVisibilityScope,

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




@RequiresApi(Build.VERSION_CODES.O)

@Composable
fun CustomSnackbar(snackbarData: SnackbarData) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colors.secondary,
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
            ButtonTextWhiteTheme(
                text = snackbarData.message,
                color = MaterialTheme.colors.primary,
                modifier = Modifier.weight(1f))
           // Spacer(Modifier.weight(1f))
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

@OptIn(ExperimentalSharedTransitionApi::class)
@SuppressLint("MissingPermission")
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun LazyGridLayout(navController: NavController,
    onMarkCompletedClick: (String) -> Unit,
                   selectedItemId: MutableState<String>,
                   isChecked: MutableState<Boolean>,
                   animatedVisibilityScope: AnimatedVisibilityScope,
                   sharedTransitionScope: SharedTransitionScope,
                   cardDataList:List<DataClass>,
                   isNotificationSet:MutableState<Boolean>
                   ){
    val context = LocalContext.current
    val MY_PERMISSIONS_REQUEST_SCHEDULE_ALARM = 123
    if (isNotificationSet.value){
        cardDataList.forEach { data ->
            var selectedDateTime = data.notificationTime
            val itemId = data.id
            val message = data.message
            val repeatOption = data.repeatedTaskTime
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
                    scheduleNotification(currentContext, selectedDateTime!!, itemId, message!!, isCheckedState = isChecked.value,repeatOption = repeatOption!!)
                }
            } else {
                // For versions lower than Android 6.0, no runtime permission is needed
                scheduleNotification(context, selectedDateTime!!, itemId, message!!, isCheckedState = isChecked.value,repeatOption = repeatOption!!)
            }

            scheduleNotification(context = context,selectedDateTime!!,itemId,message!!, isCheckedState = isChecked.value,repeatOption = repeatOption!!)
        }
    }
    val newTaskId = navController.currentBackStackEntry?.savedStateHandle?.get<String>("newTaskId")
    val sharedKey = "bounds-$newTaskId"
    val gridColumns = 2

    with(sharedTransitionScope){
        if (cardDataList.isNotEmpty()){
            if (cardDataList.size > 1) {
                LazyVerticalStaggeredGrid(
                    columns = StaggeredGridCells.Fixed(gridColumns),
                    verticalItemSpacing = 16.dp,
                    contentPadding = PaddingValues(start = 24.dp,end = 24.dp,top = 24.dp),


                    ) {
                    itemsIndexed(cardDataList.reversed()) { index, cardData ->
                        val isNewTask = cardData.id == newTaskId
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
                                    bottom = if (index == cardDataList.size - 1) 24.dp else 0.dp
                                )

                                .animateContentSize(
                                    animationSpec = tween(
                                        durationMillis = 200,
                                        easing = EaseInElastic
                                    )
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
                                repeatOption = cardData.repeatedTaskTime,
                                animatedVisibilityScope = animatedVisibilityScope,
                                sharedTransitionScope = sharedTransitionScope,
                                isNewTask = isNewTask,
                                modifier = Modifier
                                    /*.sharedBounds(
                                        rememberSharedContentState(key = if (!isNewTask) "bounds-$id" else sharedKey),
                                        animatedVisibilityScope = animatedVisibilityScope,
                                        boundsTransform = {initialRect,targetRect ->
                                            spring(dampingRatio = 0.8f,
                                                stiffness = 380f)

                                        },
                                        placeHolderSize = SharedTransitionScope.PlaceHolderSize.animatedSize
                                    )*/

                            )




                        }
                    }
                }
            }else if (cardDataList.size == 1) {
                LazyColumn(){
                    itemsIndexed(cardDataList){ index,cardData ->
                        val isNewTask = cardData.id == newTaskId
                        Box(modifier = Modifier
                            .fillMaxSize()
                            //.size(300.dp)
                            .padding(top = 24.dp, bottom = 24.dp)

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

                            RoundedCircleCardDesign (
                                message = cardDataList[0].message!!,
                                time = cardDataList[0].time!!,
                                date = formattedDate,
                                id = cardDataList[0].id,
                                onMarkCompletedClick = onMarkCompletedClick,
                                selectedItemId = selectedItemId,
                                isChecked = isChecked,
                                navController = navController,
                                repeatOption = cardDataList[0].repeatedTaskTime,
                                animatedVisibilityScope = animatedVisibilityScope,
                                sharedTransitionScope = sharedTransitionScope,
                                isNewTask = isNewTask,
                                modifier = Modifier.animateItemPlacement()



                            )




                        }
                    }
                }
            }
        }
    }


}



@OptIn(ExperimentalSharedTransitionApi::class)
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
    repeatOption: String?,
    animatedVisibilityScope: AnimatedVisibilityScope,
    sharedTransitionScope: SharedTransitionScope,

    isNewTask: Boolean,
    modifier: Modifier

    )
{

    val transitionKey = remember(id, isNewTask) {
        if (isNewTask) "bounds-$id" else "card-$id"
    }
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
    val updateScreenOpen = remember {
        mutableStateOf(false)
    }
    val isClicked = remember{
        mutableStateOf(false)
    }

    val newTaskId = navController.currentBackStackEntry?.savedStateHandle?.get<String>("newTaskId")
    val sharedKey = "bounds-$newTaskId"
    val isNewlyAdded = id == newTaskId

Log.d("homescreenid","$id")
with(sharedTransitionScope){
    Box(
        modifier = modifier
            .size(184.dp)
            .aspectRatio(1f)
            .sharedBounds(
                rememberSharedContentState(key = "bounds-$id"),
                animatedVisibilityScope = animatedVisibilityScope,

                boundsTransform = { initialRect, targetRect ->
                    spring(
                        dampingRatio = 0.8f,
                        stiffness = 380f
                    )

                },
                placeHolderSize = SharedTransitionScope.PlaceHolderSize.animatedSize
            )

            .clip(CircleShape)
            .background(MaterialTheme.colors.secondary, shape = CircleShape)

            .clickable(indication = null,
                interactionSource = remember { MutableInteractionSource() }) {
                updateScreenOpen.value = true
                isClicked.value = true
               // visible.value = false

            }

        ,
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .heightIn(max = 200.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .size(32.dp)

                    .background(color = MaterialTheme.colors.background, shape = CircleShape)
                    .clickable(indication = null,
                        interactionSource = remember { MutableInteractionSource() }) {
                        if (isChecked.value) {
                            coroutineScope.launch(Dispatchers.IO) {
                                val mMediaPlayer =
                                    MediaPlayer.create(mContext, R.raw.tab_button)
                                mMediaPlayer.start()
                                delay(mMediaPlayer.duration.toLong())
                                mMediaPlayer.release()
                                onMarkCompletedClick(id.toString())
                            }
                            Vibration(context = mContext)
                        }

                    })
            Text(
                text = ("$message"),
                textAlign = TextAlign.Center,
                fontFamily = interDisplayFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                color = MaterialTheme.colors.primary,
                style = androidx.compose.ui.text.TextStyle(letterSpacing = 0.sp),
                modifier = Modifier
                    .padding(top = 26.dp, start = 16.dp, end = 16.dp)
                    ,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                letterSpacing = 0.5.sp
                   /* .sharedElement(
                        state = rememberSharedContentState(key = "boundsMessage-$id"),
                        animatedVisibilityScope = animatedVisibilityScope,
                        boundsTransform = { _,_, ->
                            tween(300)
                        },
                        placeHolderSize = SharedTransitionScope.PlaceHolderSize.animatedSize
                    )*/

            )
            Text(
                text =dateString.toUpperCase(),
                fontFamily = interDisplayFamily,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Normal,
                fontSize = 11.sp,
                color = MaterialTheme.colors.primary.copy(alpha = 0.50f),
                style = androidx.compose.ui.text.TextStyle(letterSpacing = 0.sp),
                modifier = Modifier
                    .padding(top = 4.dp, start = 16.dp, end = 16.dp)
                    .height(15.dp),
                overflow = TextOverflow.Ellipsis,
                letterSpacing = 1.sp
                   /* .sharedElement(
                        state = rememberSharedContentState(key = "boundsDateandTime-$id"),
                        animatedVisibilityScope = animatedVisibilityScope,
                        boundsTransform = { _,_, ->
                            tween(300)
                        }
                    )*/
            )
            if (repeatOption in listOf("Daily","Weekly","Monthly","Yearly") ){
                ThemedRepeatedIconImage(
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .alpha(0.3f))
            }

        }
        if (updateScreenOpen.value){
            LaunchedEffect(Unit){
                navController.navigate(
                    route = Screen.Update.passUpdateValues(
                        id = id.toString(),
                        isChecked = isChecked.value,
                    )
                )
            }
        }
    }
}


}
@Composable
fun ThemedRepeatedIconImage(modifier: Modifier) {
    val isDarkTheme = isSystemInDarkTheme()
    val imageRes = if (isDarkTheme) {
        R.drawable.repeat_icon_black
    } else {
        R.drawable.repeat_icon_white
    }

    Image(
        painter = painterResource(id = imageRes),
        contentDescription = null,
        modifier = modifier
            .alpha(0.5f)

            
    )
}
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun CanvasShadow(
    modifier:Modifier
){
    val isDarkTheme = isSystemInDarkTheme()


    Canvas(

                modifier = modifier) {

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
                    endY = size.height.coerceAtMost(140.dp.toPx())
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
                    startY = (size.height - 96.dp.toPx()).coerceAtLeast(0f),
                    endY = size.height
                )
                drawRect(brush = gradientBrush)
                drawRect(brush = opacityBrush)
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
@OptIn(ExperimentalSharedTransitionApi::class)
@SuppressLint("UnrememberedMutableState")
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun FloatingActionButton(
    navController: NavController,
    isChecked:MutableState<Boolean>,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,

){
    var isButtonProcessing by remember { mutableStateOf(false) }
    val shadowOpacity = 0.4f // Set the desired opacity value (between 0 and 1)
    val shadowColor = Color.Black.copy(alpha = shadowOpacity)
   // var visible by remember { mutableStateOf(false) }
     val context = LocalContext.current

    val coroutineScope = rememberCoroutineScope()
    val mContext = LocalContext.current
    with(animatedVisibilityScope){
        with(sharedTransitionScope){

            Box(modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(bottom = 78.dp)
                .background(Color.Transparent)
            ) {
                androidx.compose.material.FloatingActionButton(
                    modifier = Modifier
                        .size(84.dp)
                        .renderInSharedTransitionScopeOverlay(zIndexInOverlay = 1f)

                        .animateEnterExit(
                            enter = fadeIn(
                                animationSpec = tween(
                                    durationMillis = 300,
                                    delayMillis = 0
                                )
                            ) +
                                    slideInVertically(
                                        initialOffsetY = { 172 }, // Keep it as it is
                                        animationSpec = tween(durationMillis = 300) // Add custom duration
                                    ),
                            exit = fadeOut(
                                animationSpec = tween(
                                    durationMillis = 200,
                                    delayMillis = 0
                                )
                            ) +
                                    slideOutVertically(
                                        targetOffsetY = { 172 }, // Keep it as it is
                                        animationSpec = tween(durationMillis = 200) // Add custom duration
                                    )
                        )


                        .sharedBounds(
                            rememberSharedContentState("addtask"),
                            animatedVisibilityScope = animatedVisibilityScope,
                            /*  enter = fadeIn(tween(durationMillis = 300, easing = EaseOutBack)),
                            exit = fadeOut(tween(durationMillis = 300, easing = EaseOutBack)),*/
                            boundsTransform = { initialRect, targetRect ->
                                spring(
                                    dampingRatio = 0.8f,
                                    stiffness = 380f
                                )

                            },
                            placeHolderSize = SharedTransitionScope.PlaceHolderSize.animatedSize


                        )
                        .align(Alignment.BottomCenter)
                        .bounceClick()
                    ,
                    onClick = {
                        if (!isButtonProcessing) {
                            isButtonProcessing = true
                            coroutineScope.launch {
                                navController.navigate(route = "Screen.AddDask.route/${isChecked.value}")
                                //visible.value = false
                                Vibration(context)
                            }

                        }
                    },
                    shape = CircleShape,
                    elevation = FloatingActionButtonDefaults.elevation(0.dp),
                    backgroundColor = FABRed,
                    contentColor = Color.White.compositeOver(shadowColor)
                ) {
                   ThemedPlusIconImage()
                }
            }


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


fun scheduleNotification(
    context: Context,
    selectedDateTime: Long,
    itemId: String,
    message: String,
    isCheckedState: Boolean,
    repeatOption: String,

) {
    val now = Calendar.getInstance().timeInMillis
    val notificationTag = "Notification_$itemId"

    val intent = Intent(context, NotificationReceiver::class.java)
    intent.putExtra("itemId", itemId)
    intent.putExtra("messageExtra", message)
    intent.putExtra("isCheckedState", isCheckedState)
    intent.putExtra("repeatOption", repeatOption)

    Log.d("messageExtra", "$message")
    val requestCode = notificationTag.hashCode()
    if (selectedDateTime > now) {
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+ requires permission to schedule exact alarms
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    selectedDateTime,
                    pendingIntent
                )
            } else {
                // Permission not granted, fallback to inexact alarm
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    selectedDateTime,
                    pendingIntent
                )
            }
        } else {
            // For Android 11 and below, no permission needed
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                selectedDateTime,
                pendingIntent
            )
        }

        // Always set the initial alarm


      /*  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    selectedDateTime,
                    pendingIntent
                )
            } else {
                // Ask user to grant permission
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                context.startActivity(intent)
                // You may want to inform the user why this is needed before this
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                selectedDateTime,
                pendingIntent
            )
        }*/

        }

}

fun schedulePastTimeNotification(
    context: Context,
    selectedDateTime: Long,
    itemId: String,
    message: String,
    isCheckedState: Boolean,
    repeatOption: String
) {
    val now = System.currentTimeMillis()
    var futureNotificationTime = selectedDateTime

    // Adjust the selectedDateTime to the next valid time if it’s in the past
    while (futureNotificationTime < now) {
        futureNotificationTime = calculateNextDueDate(futureNotificationTime, repeatOption)
    }

    val notificationTag = "Notification_$itemId"
    val intent = Intent(context, NotificationReceiver::class.java).apply {
        putExtra("itemId", itemId)
        putExtra("messageExtra", message)
        putExtra("isCheckedState", isCheckedState)
        putExtra("repeatOption", repeatOption)
    }

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
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        // Android 12+ requires permission to schedule exact alarms
        if (alarmManager.canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
               futureNotificationTime,
                pendingIntent
            )
        } else {
            // Permission not granted, fallback to inexact alarm
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
               futureNotificationTime,
                pendingIntent
            )
        }
    } else {
        // For Android 11 and below, no permission needed
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
           futureNotificationTime,
            pendingIntent
        )
    }



    Log.d("Notification", "Scheduled for itemId $itemId at ${Date(futureNotificationTime)}")
}
/*fun scheduleNotification(context: Context, selectedDateTime: Long, itemId: String, message: String, isCheckedState: Boolean, repeatOption: String) {
    val data = Data.Builder()
        .putString("itemId", itemId)
        .putString("messageExtra", message)
        .putBoolean("isCheckedState", isCheckedState)
        .putString("repeatOption", repeatOption)
        .build()

    val delay = selectedDateTime - System.currentTimeMillis()
    val workRequest = OneTimeWorkRequestBuilder<TaskUpdateWorker>()
        .setInputData(data)
        .setInitialDelay(delay, TimeUnit.MILLISECONDS)
        .build()

    WorkManager.getInstance(context).enqueue(workRequest)
}*/
fun updateNextNotificationDate(itemId: String, nextDate: Long) {
    val database = FirebaseDatabase.getInstance()
    val user = FirebaseAuth.getInstance().currentUser
    val uid = user?.uid
    val taskRef = database.reference.child("Task").child(uid.toString()).child(itemId)
    taskRef.child("nextNotificationDate").setValue(nextDate)
}
private fun setAlarm(alarmManager: AlarmManager, timeInMillis: Long, pendingIntent: PendingIntent) {
    alarmManager.setExact(
        AlarmManager.RTC_WAKEUP,
        timeInMillis,
        pendingIntent
    )
}
private fun getRepeatIntervalMillis(repeatOption: String): Long {
    return when (repeatOption) {
        "DAILY" -> AlarmManager.INTERVAL_DAY
        "WEEKLY" -> AlarmManager.INTERVAL_DAY * 7
        "MONTHLY" -> AlarmManager.INTERVAL_DAY*30// Approximate 30 days in a month
        "YEARLY" -> AlarmManager.INTERVAL_DAY * 365
        else -> -1 // Unknown or no repeat option
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
@OptIn(ExperimentalSharedTransitionApi::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun TopSectionHomeScreen(navController: NavController,
                         isMarkCompletedOpen:MutableState<Boolean>,
                         selectedMarkedItemId: MutableState<String>,
                         isChecked:MutableState<Boolean>,
                         sharedPreferences: SharedPreferences,
                         sharedTransitionScope: SharedTransitionScope,
                         animatedVisibilityScope:AnimatedVisibilityScope,
                         todayTaskCount:Int,
                         upcomingTaskCount:Int
                         ){

    val context = LocalContext.current

    with(animatedVisibilityScope)
   {
        with(sharedTransitionScope){
            Row(modifier = Modifier
                .fillMaxWidth()

                .padding(top = 56.dp, start = 24.dp, end = 24.dp)
                .renderInSharedTransitionScopeOverlay(zIndexInOverlay = 1f)
                .animateEnterExit(
                    enter = fadeIn(
                        tween(
                            durationMillis = 150,
                            delayMillis = 0
                        )
                    ) + slideInVertically() { -72 },
                    exit = fadeOut(
                        tween(
                            durationMillis = 150,
                            delayMillis = 0
                        )
                    ) + slideOutVertically() { -48 }
                ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val shape = RoundedCornerShape(16.dp)
                val backgroundColor = MaterialTheme.colors.background
                Text(
                    text = buildAnnotatedString {
                        withStyle(style = SpanStyle(color = MaterialTheme.colors.primary.copy(alpha = 0.5f))) {
                            append("You have ")
                        }
                        withStyle(style = SpanStyle(color = MaterialTheme.colors.primary)) {
                            append(
                                if (todayTaskCount == 0) "no tasks today"
                                else "$todayTaskCount ${if (todayTaskCount == 1) "task" else "tasks"} today"
                            )
                        }
                        withStyle(style = SpanStyle(color = MaterialTheme.colors.primary.copy(alpha = 0.5f))) {
                            append(",\nand ")
                        }
                        withStyle(style = SpanStyle(color = MaterialTheme.colors.primary.copy(alpha = 0.5f))) {
                            append(
                                if (upcomingTaskCount == 0) "no upcoming tasks"
                                else "$upcomingTaskCount upcoming ${if (upcomingTaskCount == 1) "task" else "tasks"}"
                            )
                        }
                        append(".")
                    }
                    ,
                    modifier = Modifier,
                    fontFamily = interDisplayFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    color = MaterialTheme.colors.primary.copy(alpha = 0.5f),
                    lineHeight = 20.sp,
                    letterSpacing = 0.5.sp

                    )
                with(sharedTransitionScope){
                    Box(modifier = androidx.compose.ui.Modifier
                        .size(48.dp)
                        /*  .sharedBounds(
                            sharedContentState = rememberSharedContentState(key = "topHomeButton"),
                            animatedVisibilityScope = animatedVisibilityScope,
                            enter = fadeIn(),
                            exit = fadeOut()
                        )*/
                        .clickable(indication = null,
                            interactionSource = remember { MutableInteractionSource() }) {
                            navController.navigate(route = "Screen.MarkComplete.route/${isChecked.value}")
                            Vibration(context)
                        }
                        .clip(shape)
                        .background(color = androidx.compose.material.MaterialTheme.colors.secondary),
                        contentAlignment = androidx.compose.ui.Alignment.Center
                    ) {

                            Row(modifier = Modifier,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                Box(modifier = androidx.compose.ui.Modifier
                                    .size(width = 12.dp, height = 32.dp)
                                    .drawBehind {
                                        val strokeWidth = 2.dp.toPx()
                                        drawRoundRect(

                                            color = backgroundColor,
                                            style = Stroke(width = strokeWidth),
                                            cornerRadius = CornerRadius(12.dp.toPx()),
                                            size = Size(size.width - strokeWidth, size.height - strokeWidth), // Shrink for inside border
                                            topLeft = Offset(strokeWidth / 2, strokeWidth / 2) // Offset to center stroke inside
                                        )

                                    }
                                    .background(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colors.background
                                    )

                                )
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp
                                )) {
                                    Box(modifier = androidx.compose.ui.Modifier
                                        .size(12.dp)

                                        .background(
                                            color = androidx.compose.material.MaterialTheme.colors.background,
                                            shape = androidx.compose.foundation.shape.CircleShape
                                        )

                                      )
                                    Box(modifier = androidx.compose.ui.Modifier
                                        .size(12.dp)

                                        .background(
                                            color = androidx.compose.material.MaterialTheme.colors.primaryVariant,
                                            shape = androidx.compose.foundation.shape.CircleShape
                                        )
                                        .drawBehind {
                                            drawCircle(
                                                color = backgroundColor,
                                                style = Stroke(width = 2.dp.toPx()),

                                                )

                                        }
                                        )
                                }

                            }


                    }
                }

            }
        }

    }

}

@Composable
fun ThemedSquareImage(modifier: Modifier) {
    var circleColor = MaterialTheme.colors.primary
    Box(
        modifier = modifier.size(24.dp)
            .background(color = MaterialTheme.colors.secondary, shape = CircleShape)
            .drawBehind {
                drawCircle(
                    color = circleColor.copy(alpha = 0.5f),
                    style = Stroke(width = 1.dp.toPx())
                )
            }
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
@Composable
fun ThemedPlusIconImage() {
    val isDarkTheme = isSystemInDarkTheme()
    val imageRes = if (isDarkTheme) {
        R.drawable.new_plus_icon
    } else {
        R.drawable.new_white_plus_icon
    }

    Image(
        painter = painterResource(id = imageRes),
        contentDescription = null,
    )
}
