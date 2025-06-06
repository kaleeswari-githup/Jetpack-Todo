package com.firstyogi.dothing

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Service.START_STICKY
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.provider.ContactsContract.Data
import android.util.Log
import android.view.animation.OvershootInterpolator
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.toUpperCase

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.firstyogi.ui.theme.*
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.*

@OptIn(ExperimentalSharedTransitionApi::class)
@SuppressLint("UnusedMaterialScaffoldPaddingParameter", "SuspiciousIndentation")
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun MarkCompletedScreen(
    navController:NavController,
    isChecked:MutableState<Boolean>,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope:AnimatedVisibilityScope,
    snackbarHostState: SnackbarHostState
    ){

    val repeatableOption = remember {
        mutableStateOf("No Repeat")
    }
    var isDeleteAllScreenOpen by remember {
        mutableStateOf(false)
    }
   // val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val sharedPreferences = context.getSharedPreferences("MyAppSettings", Context.MODE_PRIVATE)
    val selectedMarkedItemId = remember { mutableStateOf("") }
    val database = FirebaseDatabase.getInstance()
    val user = FirebaseAuth.getInstance().currentUser
    val uid = user?.uid

    val completedTasksCountState = remember { mutableStateOf(0) }
    val scaffoldState = rememberScaffoldState()
    val coroutineScope = rememberCoroutineScope()

    val valueEventListener = object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            completedTasksCountState.value = snapshot.childrenCount.toInt()
        }

        override fun onCancelled(error: DatabaseError) {
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            scaffoldState.snackbarHostState.currentSnackbarData?.dismiss()
        }
    }
    var completedTasksRef = database.reference.child("Task").child("CompletedTasks").child(uid.toString())
    DisposableEffect(Unit) {
        completedTasksRef.addValueEventListener(valueEventListener)

        onDispose {
            completedTasksRef.removeEventListener(valueEventListener)
        }
    }

    var cardDataList = remember {
        mutableStateListOf<DataClass>()
    }
    LaunchedEffect(Unit){
        val valueEventListener = object :ValueEventListener{
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                cardDataList.clear()
                for(childSnapshot in dataSnapshot.children){
                    val id = childSnapshot.key.toString()
                    val map = childSnapshot.value as? Map<*, *>
                    if (map != null) {
                        val safeData = DataClass(
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
                            //nextDueDateForCompletedTask = map["nextDueDateForCompletedTask"] as? String ?: "",
                            formatedDateForWidget = map["formatedDateForWidget"] as? String ?: ""
                        )
                        cardDataList.add(safeData)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseError", "Database operation cancelled: $error")
            }
        }
        completedTasksRef.addValueEventListener(valueEventListener)
    }

    val onUnMarkCompletedClick: (String) -> Unit = { clickedTaskId ->
        val taskRef = database.reference.child("Task").child(uid.toString()).child(clickedTaskId)
        val completedTasksRef = database.reference.child("Task").child("CompletedTasks").child(uid.toString()).child(clickedTaskId)
        val completedNewTaskRef = database.reference.child("Task").child("CompletedTasks").child(uid.toString()).child(clickedTaskId)
        completedTasksRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val data = snapshot.getValue(DataClass::class.java)
                if (data != null) {
Log.d("unmarkcompletid","${completedNewTaskRef.key}")
                    val nextDueDate = calculateNextDueDate(System.currentTimeMillis(), data.repeatedTaskTime!!)
                    if (data.date != null){

                     //   data.nextDueDate = nextDueDate
                    }



                    completedTasksRef.removeValue()
                    taskRef.setValue(data)
                    scheduleNotification(
                        context,
                        nextDueDate,
                        data.id,
                        data.message!!,
                        false,
                        data.repeatedTaskTime!!
                    )
                    coroutineScope.launch {
                        snackbarHostState.currentSnackbarData?.dismiss()
                        val snackbarResult = snackbarHostState.showSnackbar(
                            message = "Task marked uncompleted",
                            actionLabel = "Undo",
                            duration = SnackbarDuration.Short
                        )
                        when (snackbarResult) {
                            SnackbarResult.Dismissed -> {
                                taskRef.setValue(data)
                            }
                            SnackbarResult.ActionPerformed -> {
                                //data.date = data.nextDueDate
                                completedNewTaskRef.setValue(data)
                                taskRef.removeValue()
                                cancelNotification(context, data.id)

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

    fun saveIsChecked(isChecked: Boolean) {
        val editor = sharedPreferences.edit()
        editor.putBoolean("isChecked", isChecked)
        editor.apply()
    }
    val blurEffectBackground by animateDpAsState(targetValue = when{
        selectedMarkedItemId.value.isNotEmpty() -> 25.dp
        isDeleteAllScreenOpen -> 25.dp
        else -> 0.dp
    }
    )
    var markCompletevisible = remember { mutableStateOf(false) }
    val gson = Gson()

    LaunchedEffect(Unit) {
                markCompletevisible.value = true // Set the visibility to true to trigger the animation
            }
    val snackbarDeleteMessage = navController.currentBackStackEntry?.savedStateHandle?.get<String>("snackbarDeleteMessage")
    val taskId = navController.currentBackStackEntry?.savedStateHandle?.get<String>("taskId")
    LaunchedEffect(snackbarDeleteMessage){
        if (snackbarDeleteMessage != null && taskId != null){

            coroutineScope.launch {

                val snapshot = completedTasksRef.child(taskId).get().await()
                val map = snapshot.value as? Map<*, *>
                val data = map?.let { parseDataClassFromSnapshot(it, taskId) }
                if (data != null){
                    completedTasksRef.child(taskId).removeValue()
                    val snackbarResult = snackbarHostState.showSnackbar(
                        message = snackbarDeleteMessage,
                        actionLabel = "Undo",
                        duration = SnackbarDuration.Short
                    )
                    val taskRef = database.reference.child("Task")
                        .child(uid.toString()).child(taskId)

                    // val data = taskRef.get().await().getValue(DataClass::class.java)
                    when(snackbarResult){
                        SnackbarResult.Dismissed -> {
                            completedTasksRef.child(taskId).removeValue()
                        }
                        SnackbarResult.ActionPerformed -> {

                            val database = FirebaseDatabase.getInstance()
                            val user = FirebaseAuth.getInstance().currentUser
                            val uid = user?.uid

                            val taskRef = database.reference.child("Task")
                                .child(uid.toString()).child(taskId)
                            val completedTasksRef = database.reference
                                .child("Task").child("CompletedTasks")
                                .child(uid.toString()).child(taskId)

                            // val data = taskRef.get().await().getValue(DataClass::class.java)
                            completedTasksRef.setValue(data)
                            // taskRef.removeValue()
                        }
                    }
                }

            }
        }
    }
    val snackbarUncompleteMessage =
        navController.currentBackStackEntry?.savedStateHandle?.get<String>("snackbarUncompleteMessage")
    LaunchedEffect(snackbarUncompleteMessage) {
        if (snackbarUncompleteMessage != null && taskId != null) {
            coroutineScope.launch {
                val snackbarResult = snackbarHostState.showSnackbar(
                    message = snackbarUncompleteMessage,
                    actionLabel = "Undo",
                    duration = SnackbarDuration.Short
                )

                when (snackbarResult) {
                    SnackbarResult.Dismissed -> {
                        // Task remains in the main list
                    }
                    SnackbarResult.ActionPerformed -> {
                        // Restore task to CompletedTasks on UNDO
                        val database = FirebaseDatabase.getInstance()
                        val user = FirebaseAuth.getInstance().currentUser
                        val uid = user?.uid

                        val taskRef = database.reference.child("Task")
                            .child(uid.toString()).child(taskId)
                        val completedTasksRef = database.reference
                            .child("Task").child("CompletedTasks")
                            .child(uid.toString()).child(taskId)

                        val data = taskRef.get().await().getValue(DataClass::class.java)
                        completedTasksRef.setValue(data)
                        taskRef.removeValue()
                    }
                }

                // Clear the saved state after showing the Snackbar
                navController.currentBackStackEntry?.savedStateHandle?.remove<String>("snackbarUncompleteMessage")
                navController.currentBackStackEntry?.savedStateHandle?.remove<String>("taskId")
            }
        }


    // Rest of the MarkCompletedScreen UI...
}
    var alreadyNavigated by remember { mutableStateOf(false) }
    with(sharedTransitionScope){
        Scaffold(
            scaffoldState = scaffoldState,
            modifier = Modifier.fillMaxSize(),
            backgroundColor = Color.Transparent
        ) {
            Box(modifier = Modifier
                .blur(radius = blurEffectBackground)
                .fillMaxSize()
                /*  .sharedBounds(
                    sharedContentState = rememberSharedContentState(key = "topHomeButton"),
                    animatedVisibilityScope = animatedVisibilityScope,
                    enter = fadeIn(),
                    exit = fadeOut()
                )*/
                .background(color = MaterialTheme.colors.background)
                .clickable(indication = null,
                    interactionSource = remember { MutableInteractionSource() }) {
                    if (!alreadyNavigated) {
                        alreadyNavigated = true
                        navController.popBackStack()
                        markCompletevisible.value = false
                    }

                },
                ) {
                //ThemedGridImage(modifier = Modifier)
              //  CanvasShadow(modifier = Modifier.fillMaxSize())
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally
                ){
                    LazyColumn(modifier = Modifier.wrapContentHeight(),
                    ) {
                        item{
                            /* val offsetY by animateDpAsState(
                                 targetValue = if (markCompletevisible.value) 0.dp else 32.dp,
                                 animationSpec = tween(
                                     durationMillis = 300,
                                     delayMillis = 0,
                                     easing = EaseOutCirc
                                 ),

                                 )
                             val opacity by animateFloatAsState(
                                 targetValue = if (markCompletevisible.value) 1f else 0f,
                                 animationSpec = keyframes {
                                     durationMillis = 300 // Total duration of the animation
                                     0.3f at 100 // Opacity becomes 0.3f after 200ms
                                     0.6f at 200 // Opacity becomes 0.6f after 500ms
                                     1f at 300 // Opacity becomes 1f after 1000ms (end of the animation)
                                 }
                             )*/
                            AnimatedVisibility(
                                visible = markCompletevisible.value,

                                enter = slideInVertically(
                                    initialOffsetY = { 32 }, // Starts off-screen at the top
                                    animationSpec = tween(
                                        durationMillis = 300,
                                        easing = { OvershootInterpolator().getInterpolation(it) },
                                    )
                                )+ fadeIn( // Combine slide and opacity for exit
                                    animationSpec = tween(
                                        durationMillis = 300,
                                        // delayMillis = 300,
                                        easing = EaseOutCirc,

                                        )
                                ),

                                exit = slideOutVertically(
                                    targetOffsetY = { 32 }, // Exits off-screen at the bottom
                                    animationSpec = tween(
                                        durationMillis = 300,
                                        delayMillis = 150,
                                        easing = EaseOutCirc,
                                    )
                                ) + fadeOut( // Combine slide and opacity for exit
                                    animationSpec = tween(
                                        durationMillis = 300,
                                        delayMillis = 150,
                                        easing = EaseOutCirc,

                                        )
                                )
                            ){
                                Box(modifier = Modifier
                                    .fillMaxWidth()
                                    // .offset(y = offsetY)
                                    // .alpha(opacity)

                                    .padding(start = 24.dp, end = 24.dp, top = 104.dp)

                                   ,

                                    ) {
                                    Column(modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 24.dp, end = 24.dp),

                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Box(modifier = Modifier
                                            .size(48.dp)
                                            .background(
                                                shape = RoundedCornerShape(16.dp),
                                                color = MaterialTheme.colors.secondary,

                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            val firebaseAuth = FirebaseAuth.getInstance()
                                            val user = firebaseAuth.currentUser
                                            val photoUrl = user?.photoUrl
                                            val initials = user?.email?.take(1)?.toUpperCase()
                                            if (photoUrl != null) {
                                                AsyncImage(
                                                    model = ImageRequest.Builder(LocalContext.current)
                                                        .data(photoUrl)
                                                        .build(),
                                                    contentDescription = "Profile picture",
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .clip(RoundedCornerShape(16.dp))
                                                )
                                            }else{
                                                Text(
                                                    text = initials ?: "",
                                                    color = Color.White,
                                                    fontFamily = interDisplayFamily,
                                                    fontSize = 20.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }

                                        }

                                        ButtonTextWhiteTheme(text = ("${user?.displayName}"),
                                            color = MaterialTheme.colors.primary,
                                            modifier = Modifier.padding(top = 12.dp),
                                        )

                                        Text(
                                            text = if (user?.email?.length ?: 0 > 32) user?.email?.substring(0, 32) + "..." else user?.email.orEmpty(),
                                            fontFamily = interDisplayFamily,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Normal,
                                            color = MaterialTheme.colors.primary.copy(alpha = 0.5f),
                                            style = androidx.compose.ui.text.TextStyle(letterSpacing = 1.sp),
                                            modifier = Modifier.padding(top = 4.dp)
                                        )

                                    }

                                }
                            }

                        }
                        item {
                            /*   val offsetY by animateDpAsState(
                                   targetValue = if (markCompletevisible.value) 0.dp else 32.dp,
                                   animationSpec = tween(durationMillis = 300, delayMillis = 100,easing = EaseOutCirc)
                               )
                               val opacity by animateFloatAsState(
                                   targetValue = if (markCompletevisible.value) 1f else 0f,
                                   animationSpec = keyframes {
                                       durationMillis = 300 // Total duration of the animation
                                       0.3f at 100 // Opacity becomes 0.3f after 200ms
                                       0.6f at 200 // Opacity becomes 0.6f after 500ms
                                       1f at 300

                                       delayMillis = 100
                                   }
                               )*/
                            AnimatedVisibility(
                                visible = markCompletevisible.value,

                                enter = slideInVertically(
                                    initialOffsetY = { 32 }, // Starts off-screen at the top
                                    animationSpec = tween(
                                        durationMillis = 300,
                                        delayMillis = 50,
                                        easing = { OvershootInterpolator().getInterpolation(it) },
                                    )
                                )+ fadeIn( // Combine slide and opacity for exit
                                    animationSpec = tween(
                                        durationMillis = 300,
                                        delayMillis = 50,
                                        easing = EaseOutCirc,

                                        )
                                ),

                                exit = slideOutVertically(
                                    targetOffsetY = { 32 }, // Exits off-screen at the bottom
                                    animationSpec = tween(
                                        durationMillis = 300,
                                        delayMillis = 100,
                                        easing = EaseOutCirc,
                                    )
                                ) + fadeOut( // Combine slide and opacity for exit
                                    animationSpec = tween(
                                        durationMillis = 300,
                                        delayMillis = 100,
                                        easing = EaseOutCirc,

                                        )
                                )
                            ){
                                Box(modifier = Modifier
                                    .fillMaxWidth()

                                    .padding(start = 24.dp, end = 24.dp, top = 24.dp)
                                    //  .offset(y = offsetY)
                                    //  .alpha(opacity)
                                    .background(
                                        color = MaterialTheme.colors.secondary,
                                        shape = RoundedCornerShape(32.dp)
                                    )

                                    .clickable(indication = null,
                                        interactionSource = remember { MutableInteractionSource() }) { },
                                    contentAlignment = Alignment.Center) {
                                    Column(modifier = Modifier
                                        ,
                                        verticalArrangement = Arrangement.Center,
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        val completedTasksCount = completedTasksCountState.value

                                        Spacer(modifier = Modifier.padding(top = 24.dp))
                                        Row(modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = 24.dp, end = 24.dp)
                                            ,
                                            horizontalArrangement = Arrangement.SpaceBetween

                                        ) {
                                            ButtonTextWhiteTheme(
                                                text = ("Completed ($completedTasksCount)"),color = MaterialTheme.colors.primary,modifier = Modifier)
                                            if (completedTasksCount > 0 ){
                                                Text(text = stringResource(id = R.string.delete_all).toUpperCase(),
                                                    fontSize = 11.sp,
                                                    fontFamily = interDisplayFamily,
                                                    fontWeight = FontWeight.Medium,
                                                    color = FABRed,
                                                    letterSpacing = 1.sp,
                                                    modifier = Modifier.clickable {
                                                        isDeleteAllScreenOpen = true
                                                    }
                                                )
                                            }

                                        }
                                        Spacer(modifier = Modifier.padding(top = 12.dp))
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(218.dp)
                                            //  .padding(start = 24.dp, end = 24.dp)

                                            ,
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (completedTasksCount > 0) {
                                                LazyRowCompletedTask(
                                                    onUnMarkCompletedClick,
                                                    isChecked,
                                                    repeatableOption = repeatableOption,
                                                    animatedVisibilityScope = animatedVisibilityScope,
                                                    sharedTransitionScope = sharedTransitionScope,
                                                    navController = navController,
                                                    cardDataList = cardDataList
                                                )
                                            }
                                            else {
                                                Text(
                                                    text = ("No completed tasks").uppercase(),
                                                    fontFamily = interDisplayFamily,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    letterSpacing = 1.sp,
                                                    color = MaterialTheme.colors.primary.copy(alpha = 0.50f),
                                                    //  modifier = Modifier.padding(top = 24.dp)
                                                )
                                            }


                                        }

                                        Spacer(modifier = Modifier.padding(top = 24.dp))
                                    }
                                }
                            }

                        }
                        item {
                            /* val offsetY by animateDpAsState(
                                 targetValue = if (markCompletevisible.value) 0.dp else 32.dp,
                                 animationSpec = tween(durationMillis = 300, delayMillis = 200,easing =  EaseOutCirc)
                             )
                             val opacity by animateFloatAsState(
                                 targetValue = if (markCompletevisible.value) 1f else 0f,
                                 animationSpec = keyframes {
                                     durationMillis = 300 // Total duration of the animation
                                     0.3f at 100 // Opacity becomes 0.3f after 200ms
                                     0.6f at 200 // Opacity becomes 0.6f after 500ms
                                     1f at 300

                                     delayMillis = 200
                                 }
                             )*/
                            AnimatedVisibility(
                                visible = markCompletevisible.value,

                                enter = slideInVertically(
                                    initialOffsetY = { 32 }, // Starts off-screen at the top
                                    animationSpec = tween(
                                        durationMillis = 300,
                                        delayMillis = 100,
                                        easing = { OvershootInterpolator().getInterpolation(it) },
                                    )
                                )+ fadeIn( // Combine slide and opacity for exit
                                    animationSpec = tween(
                                        durationMillis = 300,
                                        delayMillis = 100,
                                        easing = EaseOutCirc,

                                        )
                                ),

                                exit = slideOutVertically(
                                    targetOffsetY = { 32 }, // Exits off-screen at the bottom
                                    animationSpec = tween(
                                        durationMillis = 300,
                                        delayMillis = 50,
                                        easing = EaseOutCirc,
                                    )
                                ) + fadeOut( // Combine slide and opacity for exit
                                    animationSpec = tween(
                                        durationMillis = 300,
                                        delayMillis = 50,
                                        easing = EaseOutCirc,

                                        )
                                )
                            ){
                                Box(modifier = Modifier
                                    .fillMaxWidth()

                                    // .offset(y = offsetY)
                                    // .alpha(opacity)
                                    .height(72.dp)
                                    .padding(start = 24.dp, end = 24.dp, top = 8.dp)
                                    .background(
                                        color = MaterialTheme.colors.secondary,
                                        shape = RoundedCornerShape(24.dp)
                                    )
                                    .clickable(indication = null,
                                        interactionSource = remember { MutableInteractionSource() }) {
                                        isChecked.value = !isChecked.value
                                        saveIsChecked(isChecked.value)
                                        if (isChecked.value) {
                                            coroutineScope.launch(Dispatchers.IO) {
                                                val mMediaPlayer =
                                                    MediaPlayer.create(context, R.raw.toggle_sound)
                                                mMediaPlayer.start()
                                                delay(mMediaPlayer.duration.toLong())
                                                mMediaPlayer.release()
                                            }
                                        }
                                        Vibration(context)
                                    },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(modifier = Modifier
                                        .fillMaxSize()
                                        .padding(start = 24.dp, end = 24.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween) {
                                        Box() {

                                            ButtonTextWhiteTheme(text = ("Sound"),color = MaterialTheme.colors.primary,modifier = Modifier)



                                        }

                                        Box(modifier = Modifier) {
                                            Box(
                                                modifier = Modifier
                                                    .clickable(indication = null,
                                                        interactionSource = remember { MutableInteractionSource() }) {
                                                        isChecked.value = !isChecked.value
                                                        saveIsChecked(isChecked.value)
                                                        if(isChecked.value){
                                                            coroutineScope.launch(Dispatchers.IO) {
                                                                val mMediaPlayer = MediaPlayer.create(context, R.raw.toggle_sound)
                                                                mMediaPlayer.start()
                                                                delay(mMediaPlayer.duration.toLong())
                                                                mMediaPlayer.release()
                                                            }
                                                        }


                                                        Vibration(context)
                                                    }
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(width = 24.dp, height = 32.dp)
                                                        .background(
                                                            if (isChecked.value) MaterialTheme.colors.primary else MaterialTheme.colors.background,
                                                            shape = RoundedCornerShape(8.dp)
                                                        )
                                                    , contentAlignment = Alignment.Center
                                                ) {

                                                    Spacer(
                                                        modifier = Modifier
                                                            .padding(start = 2.dp, end = 2.dp)
                                                            .align(if (isChecked.value) Alignment.CenterEnd else Alignment.CenterStart)
                                                            .size(width = 12.dp, height = 28.dp)
                                                            .background(
                                                                MaterialTheme.colors.secondary,
                                                                CircleShape
                                                            )
                                                    ) }
                                            }
                                        }

                                    }

                                }
                            }

                        }
                        item {
                            /* val offsetY by animateDpAsState(
                                 targetValue = if (markCompletevisible.value) 0.dp else 32.dp,
                                 animationSpec = tween(durationMillis = 300, delayMillis = 300,easing = EaseOutCirc)
                             )
                             val opacity by animateFloatAsState(
                                 targetValue = if (markCompletevisible.value) 1f else 0f,
                                 animationSpec = keyframes {
                                     durationMillis = 300 // Total duration of the animation
                                     0.3f at 100 // Opacity becomes 0.3f after 200ms
                                     0.6f at 200 // Opacity becomes 0.6f after 500ms
                                     1f at 300

                                     delayMillis = 300
                                 }
                             )*/
                            AnimatedVisibility(
                                visible = markCompletevisible.value,

                                enter = slideInVertically(
                                    initialOffsetY = { 32 }, // Starts off-screen at the top
                                    animationSpec = tween(
                                        durationMillis = 300,
                                        delayMillis = 150,
                                        easing =  { OvershootInterpolator().getInterpolation(it) },
                                    )
                                )+ fadeIn( // Combine slide and opacity for exit
                                    animationSpec = tween(
                                        durationMillis = 300,
                                        delayMillis = 150,
                                        easing = EaseOutCirc,

                                        )
                                ),

                                exit = slideOutVertically(
                                    targetOffsetY = { 32 }, // Exits off-screen at the bottom
                                    animationSpec = tween(
                                        durationMillis = 300,
                                        // delayMillis = 300,
                                        easing = EaseOutCirc,
                                    )
                                ) + fadeOut( // Combine slide and opacity for exit
                                    animationSpec = tween(
                                        durationMillis = 300,
                                        //delayMillis = 300,
                                        easing = EaseOutCirc,

                                        )
                                )
                            ){
                                Box(modifier = Modifier
                                    .fillMaxWidth()

                                    // .offset(y = offsetY)
                                    // .alpha(opacity)
                                    .height(72.dp)
                                    .padding(start = 24.dp, end = 24.dp, top = 8.dp)
                                    .background(
                                        color = MaterialTheme.colors.secondary,
                                        shape = RoundedCornerShape(24.dp)
                                    )
                                    .clickable(indication = null,

                                        interactionSource = remember { MutableInteractionSource() }) {
                                        cancelNotification(context, id)
                                        cancelNotificationManger(context, id)
                                        val user = FirebaseAuth.getInstance().currentUser
                                        val currentuserId = user?.uid
                                        if (currentuserId != null) {
                                            cancelAllNotifications(context, currentuserId)
                                        }
                                        val auth = FirebaseAuth.getInstance()
                                        // Sign out from Firebase
                                        auth.signOut()
                                        val googleSignInClient = GoogleSignIn.getClient(
                                            context,
                                            GoogleSignInOptions.DEFAULT_SIGN_IN
                                        )
                                        // Sign out from Google
                                        googleSignInClient
                                            .signOut()
                                            .addOnCompleteListener {
                                                // Optional: Perform any additional actions after sign out
                                                val intent =
                                                    Intent(context, SigninActivity::class.java)
                                                context.startActivity(intent)
                                                (context as Activity).overridePendingTransition(
                                                    android.R.anim.fade_in,
                                                    android.R.anim.fade_out
                                                )
                                                // onDismiss.invoke()
                                            }
                                    },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(modifier = Modifier
                                        .fillMaxSize()
                                        .padding(start = 24.dp, end = 24.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween) {
                                        ButtonTextWhiteTheme(text = ("Log Out"),color = MaterialTheme.colors.primary,modifier = Modifier)
                                        Box(modifier = Modifier

                                            .align(Alignment.CenterVertically)
                                        ) {
                                            ThemedRightIcon()
                                        }

                                    }

                                }
                            }

                        }


                    }
var openXPage by remember{
    mutableStateOf(false)
}
                    Box(modifier = Modifier
                        .padding(bottom = 40.dp)
                        .wrapContentWidth()
                        .height(48.dp)


                        .clickable(indication = null,

                            interactionSource = remember { MutableInteractionSource() }) {
                            openXPage = true
                        }
                        ,
                        contentAlignment = Alignment.Center
                    ){
                        Row(modifier = Modifier

                            .padding(start = 24.dp,end = 24.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "Connect With Us".toUpperCase(),
                                color = MaterialTheme.colors.primary.copy(alpha = 0.5f),
                                fontFamily = interDisplayFamily,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                letterSpacing = 1.sp)

                            ThemedTwitterIcon()

                            Text(text = "@to_dothing",
                                color = MaterialTheme.colors.primary.copy(alpha = 0.5f),
                                fontFamily = interDisplayFamily,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                letterSpacing = 0.5.sp)
                        }
                    }
                    if (openXPage){
                        OpenXButton{openXPage = false}
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 56.dp),
                    contentAlignment = Alignment.TopEnd
                ){
                    CrossFloatingActionButton(
                        onClick = {
                            if (!alreadyNavigated) {
                                alreadyNavigated = true
                                navController.popBackStack()
                                markCompletevisible.value = false
                            }

                        },
                        visible = markCompletevisible
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

    if (isDeleteAllScreenOpen){
        DeleteAllScreenPage(onDismiss = {
            isDeleteAllScreenOpen = false
        } ,
            cardDataList = cardDataList)
    }
}
@Composable
fun ThemedTwitterIcon() {
    val isDarkTheme = isSystemInDarkTheme()
    val imageRes = if (isDarkTheme) {
        R.drawable.twitter_x_icon_dark_theme
    } else {
        R.drawable.twitter_x_icon_light_theme
    }

    Image(
        painter = painterResource(id = imageRes),
        contentDescription = null,
    )
}

@Composable
fun OpenXButton(onPageOpened: () -> Unit) {
    val context = LocalContext.current
    val twitterUsername = "to_dothing" // REMOVE '@' from the username


        val twitterAppUri = Uri.parse("twitter://user?screen_name=$twitterUsername")
        val twitterWebUri = Uri.parse("https://twitter.com/$twitterUsername")

        val intent = Intent(Intent.ACTION_VIEW, twitterAppUri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            context.startActivity(intent) // Open in the X app
        } catch (e: Exception) {
            context.startActivity(Intent(Intent.ACTION_VIEW, twitterWebUri)) // Open in browser if X app is not installed
        }
    onPageOpened()
}

@Composable
fun ThemedRightIcon() {
    val isDarkTheme = isSystemInDarkTheme()
    val imageRes = if (isDarkTheme) {
        R.drawable.exit_logout_icon_dark_theme
    } else {
        R.drawable.exit_logout_icon_light_theme
    }

    Image(
        painter = painterResource(id = imageRes),
        contentDescription = null,

        )
}

@Composable
fun ThemedSoundIcon() {
    val isDarkTheme = isSystemInDarkTheme()
    val imageRes = if (isDarkTheme) {
        R.drawable.dark_sound_icon
    } else {
        R.drawable.light_sound_icon
    }

    Image(
        painter = painterResource(id = imageRes),
        contentDescription = null,

        )
}
@Composable
fun ThemedLogoutIcon() {
    val isDarkTheme = isSystemInDarkTheme()
    val imageRes = if (isDarkTheme) {
        R.drawable.dark_logout_icon
    } else {
        R.drawable.light_logout_icon
    }

    Image(
        painter = painterResource(id = imageRes),
        contentDescription = null,

        )
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalSharedTransitionApi::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun LazyRowCompletedTask(
    onUnMarkcompletedClick: (String) -> Unit,
    isChecked: MutableState<Boolean>,
                         repeatableOption: MutableState<String>,
                         animatedVisibilityScope: AnimatedVisibilityScope,
                         sharedTransitionScope: SharedTransitionScope,
                         navController: NavController,
    cardDataList:List<DataClass>
                         ){
    val database = FirebaseDatabase.getInstance()
    val user = FirebaseAuth.getInstance().currentUser
    val uid = user?.uid
    var completedTasksRef = database.reference.child("Task").child("CompletedTasks").child(uid.toString())



    LazyRow(contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)){
        items(cardDataList.reversed(),key = {it.id}){cardData ->
            val originalDateFormat = DateTimeFormatter.ofPattern("MM/dd/yyyy")
            val desiredDateFormat = DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale.ENGLISH)
            val dateStringFromDatabase = cardData.nextDueDate
            val formattedDate = cardData.nextDueDate?.takeIf { it != 0L }?.let { nextDueDate ->
                val originalDate = Date(nextDueDate)
                val calendar = Calendar.getInstance().apply { time = originalDate }
                val currentYear = LocalDate.now().year
                val desiredDateFormat = if (calendar[Calendar.YEAR] == currentYear) {
                    DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale.ENGLISH) // Current year format
                } else {
                    DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale.ENGLISH) // Different year format
                }
                val localDate = originalDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
                desiredDateFormat.format(localDate)
            } ?: run {
                // Check if cardData.date is not empty or null before parsing
                if (!cardData.date.isNullOrEmpty()) {
                    val originalDate = LocalDate.parse(cardData.date, DateTimeFormatter.ofPattern("MM/dd/yyyy"))
                    val calendar = Calendar.getInstance().apply { time = Date(originalDate.toEpochDay()) }
                    val currentYear = LocalDate.now().year
                    val desiredDateFormat = if (calendar[Calendar.YEAR] == currentYear) {
                        DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale.ENGLISH) // Current year format
                    } else {
                        DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale.ENGLISH) // Different year format
                    }
                    desiredDateFormat.format(originalDate)
                } else {
                    // Return a default value if date is empty or null
                    "No Date Available"
                }
            }


            Log.d("formattedDateValue" , "$formattedDate")
            MarkCompletedCircleDesign(
                id = cardData.id,
                message = cardData.message!!,
                time = cardData.time!!,
                date = formattedDate ,

            onUnMarkcompletedClick = onUnMarkcompletedClick,

                modifier = Modifier.animateItemPlacement(),
                isChecked = isChecked,
                repeatableOption = cardData.repeatedTaskTime!!,
                animatedVisibilityScope = animatedVisibilityScope,
                sharedTransitionScope = sharedTransitionScope,
                navController = navController

            )

        }
    }
}
@OptIn(ExperimentalSharedTransitionApi::class)
@SuppressLint("UnrememberedMutableState")
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun MarkCompletedCircleDesign(
    id:String?,
    message:String?,
    time: String,
    date:String,

    onUnMarkcompletedClick:(String) -> Unit,

    modifier: Modifier=Modifier,
    isChecked: MutableState<Boolean>,
    repeatableOption: String,
    animatedVisibilityScope:AnimatedVisibilityScope,
    sharedTransitionScope: SharedTransitionScope,
    navController: NavController
                              ){
    var shadowColors = MaterialTheme.colors.secondary
    var borderShadowColor = MaterialTheme.colors.primary
    with(sharedTransitionScope){
        Box(
            modifier = modifier
                .size(184.dp)


                .sharedBounds(
                    rememberSharedContentState(key = "boundsUnMark-$id"),
                    animatedVisibilityScope = animatedVisibilityScope,
                    // enter = fadeIn(tween(durationMillis = 300, easing = EaseOutBack)),
                    // exit = fadeOut(tween(durationMillis = 300, easing = EaseOutBack)),
                    boundsTransform = { initialRect, targetRect ->
                        spring(
                            dampingRatio = 0.8f,
                            stiffness = 380f
                        )

                    },
                    placeHolderSize = SharedTransitionScope.PlaceHolderSize.animatedSize
                )

                .bounceClick()
                .background(color = MaterialTheme.colors.secondary, shape = CircleShape)
                .clip(CircleShape)
                .drawBehind {
                    val shadowColor = borderShadowColor.copy(alpha = 0.05f) // Soft inner shadow
                    val strokeWidth = 2.dp.toPx() // Shadow thickness
                    val topOffset = 1.5.dp.toPx() // Adjust to keep top shadow visible
                    val bottomOffset = 4.dp.toPx() // Slightly hide bottom shadow

                    drawCircle(
                        color = shadowColor,
                        radius = size.minDimension / 2 - strokeWidth, // Make shadow inner
                        center = Offset(
                            size.width / 2,
                            size.height / 2 - topOffset
                        ), // Shift shadow upward
                        style = Stroke(width = strokeWidth)
                    )

                    drawCircle(
                        color = shadowColors, // Hide bottom part of the shadow
                        radius = size.minDimension / 2 - strokeWidth - bottomOffset,
                        center = Offset(size.width / 2, size.height / 2 + bottomOffset)
                    )
                }
                .clickable(indication = null,
                    interactionSource = remember { MutableInteractionSource() }) {
                    // selectedMarkedItemId.value = id
                    navController.navigate(
                        route = Screen.UnMarkCompleted.passUnMarkCompletedValue(
                            id = id.toString(),
                            isChecked = isChecked.value
                        )
                    )
                },
            contentAlignment = Alignment.Center
        ){
            val originalDate: LocalDate? = if (date.isNotEmpty()) {
                try {
                    LocalDate.parse(date, DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale.ENGLISH))
                } catch (e: DateTimeParseException) {
                    // Handle the parsing error, log it, or set originalDate to a default value
                    null
                }
            } else {
                null
            }
            val currentYear = LocalDate.now().year
            val desiredDateFormat = originalDate?.let {
                if (it.year == currentYear) {
                    DateTimeFormatter.ofPattern("EEE, d MMM", Locale.ENGLISH)
                } else {
                    DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale.ENGLISH)
                }
            }
            val dateString = if (originalDate != null) {
                if (time.isNotEmpty()) {
                    "${formatDate(originalDate)}, $time"
                    // "${originalDate.format(desiredDateFormat)}, $time"
                } else {
                    "${formatDate(originalDate)}"
                    //originalDate.format(desiredDateFormat)
                }
            } else {
                ""
            }
            Log.d("dateStringValue","$date")
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ){
                ThemedFilledSquareImage(modifier = Modifier
                    .padding(top = 8.dp)
                    .clickable {
                        onUnMarkcompletedClick(id.toString())
                        Log.d("ClickedId","$id")

                    })
                Text(
                    text = buildAnnotatedString {
                        append("$message")
                        addStyle(
                            style = SpanStyle(
                                textDecoration = TextDecoration.LineThrough
                            ),
                            start = 0,
                            end = message!!.length
                        )
                    },
                    textAlign = TextAlign.Center,
                    fontFamily = interDisplayFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    color = MaterialTheme.colors.primary,
                    modifier = Modifier.padding(top = 26.dp,start = 16.dp,end = 16.dp)
                        ,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = androidx.compose.ui.text.TextStyle(letterSpacing = 0.5.sp)
                )
                Text(

                    text = dateString.toUpperCase(),
                    fontFamily = interDisplayFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colors.primary.copy(alpha = 0.75f),
                    modifier = Modifier.padding(top = 4.dp,start = 16.dp,end = 16.dp),
                    style = androidx.compose.ui.text.TextStyle(letterSpacing = 1.sp)
                )
                if (repeatableOption in listOf("Daily","Weekly","Monthly","Yearly") ){
                    ThemedRepeatedIconImage(
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .alpha(0.3f))
                }
            }
           /* if (selectedMarkedItemId.value == id){
                UnMarkCompletedTaskScreen(
                    selectedDate = mutableStateOf(date),
                    selectedTime = mutableStateOf(time),
                    textValue = message!!,
                    id = id,
                    openKeyboard = false,
                    onDismiss ={ selectedMarkedItemId.value = "" },
                    onDeletedClick,
                    onUnMarkCompletedClick = onUnMarkcompletedClick,
                    isChecked,
                    repeatableOption = repeatableOption,
                    animatedVisibilityScope = animatedVisibilityScope,
                    sharedTransitionScope = sharedTransitionScope
                )
            }*/
        }
    }


}
enum class ContainerState{
    MarkCompletedCircleDesign,
    UnMarkCompletedScreen
}

@Composable
fun ThemedFilledSquareImage(modifier: Modifier) {
    val isDarkTheme = isSystemInDarkTheme()

    val imageRes = if (isDarkTheme) {
        R.drawable.dark_black_square
    } else {
        R.drawable.light_black_square
    }

    Image(
        painter = painterResource(id = imageRes),
        contentDescription = null,
        modifier = modifier

    )

}