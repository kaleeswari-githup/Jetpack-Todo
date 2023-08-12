@file:OptIn(ExperimentalFoundationApi::class)

package com.example.Pages

import android.annotation.SuppressLint
import android.app.Dialog
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.DrawableRes
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
import dev.omkartenkale.explodable.rememberExplosionController
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.*
import java.util.concurrent.TimeUnit

@SuppressLint("UnusedMaterialScaffoldPaddingParameter", "CoroutineCreationDuringComposition")
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun HomeScreen(navController: NavHostController,scale:Float, offset: Dp){
    val scaffoldState = rememberScaffoldState()
    val coroutineScope = rememberCoroutineScope()
    val database = FirebaseDatabase.getInstance()
    val user = FirebaseAuth.getInstance().currentUser
    val uid = user?.uid
    var context = LocalContext.current
    val onDeleteClick:(String) -> Unit = {clickedTaskId ->
        val databaseRef = database.reference.child("Task").child(uid.toString())
        val taskRef = database.reference.child("Task").child(uid.toString()).child(clickedTaskId)
        coroutineScope.launch {
            scaffoldState.snackbarHostState.currentSnackbarData?.dismiss()
            val data = databaseRef.child(clickedTaskId).get().await().getValue(DataClass::class.java)
            if (data != null) {
                databaseRef.child(clickedTaskId).removeValue()
                val snackbarResult = scaffoldState.snackbarHostState.showSnackbar(
                    message = "Task deleted",
                    actionLabel = "Undo",
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
        var completedTasksRef = database.reference.child("Task").child("CompletedTasks").child(uid.toString()).child(clickedTaskId)
        taskRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val data = snapshot.getValue(DataClass::class.java)
                if (data != null) {
                    taskRef.removeValue()
                    completedTasksRef.setValue(data)
                    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    notificationManager.cancel(clickedTaskId.hashCode())
                    val notificationTag = "Notification_$clickedTaskId"
                    val workManager = WorkManager.getInstance(context)
                    workManager.cancelUniqueWork(notificationTag)

                    coroutineScope.launch {
                        scaffoldState.snackbarHostState.currentSnackbarData?.dismiss()
                        val snackbarResult = scaffoldState.snackbarHostState.showSnackbar(
                            message = "Task Mark as Completed",
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
    var isAddDaskScreenOpen = remember {
        mutableStateOf(false)
    }
    var isMarkCompletedOpen = remember { mutableStateOf(false) }
    var isPickerOpen = remember { mutableStateOf(false) }
    var isAddDaskOpen = remember { mutableStateOf(false) }
    val blurEffectBackground by animateDpAsState(
        targetValue = when {
            selectedItemId.value.isNotEmpty() -> 25.dp
            isAddDaskScreenOpen.value -> 25.dp
            isMarkCompletedOpen.value -> 25.dp
            else -> 0.dp
        },
        animationSpec = tween(
            durationMillis = 300, // Adjust the duration to your desired value
            easing = EaseOutCirc // You can try different easing functions for smoother animations
        )
    )
    Scaffold(
        scaffoldState = scaffoldState,
        modifier = Modifier.fillMaxSize(),
        backgroundColor = Color.Transparent,
        ){
        BoxWithConstraints(modifier = Modifier
            .blur(radius = blurEffectBackground)
            .background(color = SurfaceGray)
            .fillMaxSize()
            ){
            Image(painter = painterResource(id = R.drawable.grid_lines), contentDescription = null)
            Box(modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center){
                Image(painter = painterResource(id = R.drawable.shadowcenter), contentDescription = null,
                    modifier = Modifier
                        .graphicsLayer(alpha = 0.04f)
                        .blur(radius = 90.dp)
                        .align(Alignment.Center))
            }
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                LazyGridLayout(
                    onMarkCompletedClick = onMarkCompletedClick,
                    onDeleteClick,
                    selectedItemId,
                    isPickerOpen,
                    isAddDaskOpen,
                    scale = scale,
offset = offset
                )
            }
            Canvas(modifier = Modifier.fillMaxSize()) {
                val gradientBrush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFEDEDED),
                        Color(0x00EDEDED)
                    ),
                    startY = 0f,
                    endY = size.height.coerceAtMost(84.dp.toPx())
                )
                val opacityBrush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0x00EDEDED),
                        Color(0xFFEDEDED)
                    ),
                    startY = (size.height - 84.dp.toPx()).coerceAtLeast(0f),
                    endY = size.height
                )
                drawRect(brush = gradientBrush)
                drawRect(brush = opacityBrush)
                drawRect(brush = gradientBrush)
            }
            Column {
                TopSectionHomeScreen(image = R.drawable.home_icon,navController,isMarkCompletedOpen,selectedMarkedItemId)
                FloatingActionButton(isAddDaskScreenOpen,isPickerOpen)
            }
        }
        if (isPickerOpen.value || selectedMarkedItemId.value.isNotEmpty() ){
            Box(modifier = Modifier
                .fillMaxSize()
                .background(color = SurfaceGray, shape = RectangleShape)
            )
        }
    }
}
@SuppressLint("MissingPermission")
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun LazyGridLayout(onMarkCompletedClick: (String) -> Unit,
                   onDeleteClick: (String) -> Unit,
                   selectedItemId: MutableState<String>,
                   isPickerOpen:MutableState<Boolean>,
                   isAddDaskOpen:MutableState<Boolean>,
                   scale:Float,
                   offset: Dp
                   ){
    val database = FirebaseDatabase.getInstance()
    val user = FirebaseAuth.getInstance().currentUser
    val uid = user?.uid
    val databaseRef: DatabaseReference = database.reference.child("Task").child(uid.toString())
    val imageResource = R.drawable.square // Resource ID of the image
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
    var animatedItemIndexes by remember { mutableStateOf(emptyList<Int>()) }
    val selectedItemIndex = remember { mutableStateOf(-1) }
    val gridColumns = 2
    if (cardDataList.size > 1) {
        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Fixed(gridColumns),
            verticalItemSpacing = 16.dp,
            contentPadding = PaddingValues(24.dp)
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
                val isAnimated = index in animatedItemIndexes

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            top = if (index == 1) 90.dp else 0.dp,
                            bottom = if (index == cardDataList.size - 1) 100.dp else 0.dp
                        )
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
                        modifier = Modifier.animateContentSize(),
                        scale = scale,
                        offset = offset,
                        index = index,
                        isAnimated = isAnimated,
                        selectedItemIndex = selectedItemIndex
                    )
                }
            }
        }
    }else if (cardDataList.size == 1) {
        LazyColumn(){
            itemsIndexed(cardDataList){ index,cardData ->
                val isAnimated = index in animatedItemIndexes
                Box(modifier = Modifier.padding(bottom = 50.dp), contentAlignment = Alignment.Center) {
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
                        modifier = Modifier.animateContentSize(),
                        scale = scale,
                        offset = offset,
                        index = index,
                        isAnimated = isAnimated,
                        selectedItemIndex = selectedItemIndex

                    )
                }
            }
        }
    }
}



@OptIn(ExperimentalAnimationApi::class)
@SuppressLint("UnrememberedMutableState", "CoroutineCreationDuringComposition")
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
    selectedItemIndex: MutableState<Int>,
    isAnimated: Boolean,
    modifier: Modifier)
{
    val scaffoldState = rememberScaffoldState()
    val infiniteTransition = rememberInfiniteTransition()

    val painter:Painter = painterResource(image)
    val animateX = animationDelay % 2 == 0
    val animateY = animationDelay % 2 != 0

    val dx by infiniteTransition.animateFloat(
        initialValue = if (animateX) -0.4f else 0f,
        targetValue = if (animateX) 0.5f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = LinearEasing, delayMillis = animationDelay * 200),
            repeatMode = RepeatMode.Reverse
        )
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
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.exploade_animation))

    val coroutineScope = rememberCoroutineScope()
    val explosionController = rememberExplosionController()
    val deleteClick = remember {
         mutableStateOf(false)
    }
    val offsetY by animateDpAsState(
        targetValue = if (deleteClick.value) 0.dp else 32.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessVeryLow
        ),

        )
    val markedAnimation = remember {
        mutableStateOf(true)
    }

        Box(
            modifier = Modifier
                .size(172.dp)
                //   .scale(scale)
                .offset(y = offset)
                // .offset(y = offsetY)
                .alpha(scale)
                .bounceClick()
                .graphicsLayer {
                    translationX = dx * travelDistance
                    translationY = dy * travelDistance
                }
                .aspectRatio(1f)
                .clip(CircleShape)
                .shadow(
                    elevation = 12.dp
                )
                .background(color = Color.White, shape = CircleShape)
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
                Image(
                    painter = painter,
                    contentDescription = "square image",
                    modifier = Modifier
                        .padding(top = 32.dp)
                        .clickable(indication = null,
                            interactionSource = remember { MutableInteractionSource() }) {

                           onMarkCompletedClick(id)
                            //selectedItemIndex.value = index
                        }
                )
                Text(
                    text = "$message",
                    textAlign = TextAlign.Center,
                    fontFamily = interDisplayFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp,
                    color = Text1,
                    style = androidx.compose.ui.text.TextStyle(letterSpacing = 0.sp),
                    modifier = Modifier.padding(top = 24.dp,start = 16.dp,end = 16.dp)
                )
                Text(
                    text = "$date $time",
                    fontFamily = interDisplayFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 11.sp,
                    color = Text2,
                    style = androidx.compose.ui.text.TextStyle(letterSpacing = 0.sp),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            if (index == selectedItemIndex.value) {
                LottieAnimation(
                    modifier = Modifier.size(200.dp),
                    composition = composition,
                    iterations = 1
                )
            }
            if(selectedItemId.value == id){
                val formatter = DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale.ENGLISH)
                val currentDate = LocalDate.now()
                val formattedDate: String = when {
                    date == "Today" -> currentDate.format(formatter)
                    date == "Yesterday" -> currentDate.minusDays(1).format(formatter)
                    date == "Tomorrow" -> currentDate.plusDays(1).format(formatter)
                    date.endsWith("day ago") || date.endsWith("days ago") -> {
                        val daysAgo = date.split(" ")[0].toInt()
                        currentDate.minusDays(daysAgo.toLong()).format(formatter)
                    }
                    date.endsWith("week ago") || date.endsWith("weeks ago") -> {
                        val weeksAgo = date.split(" ")[0].toInt()
                        currentDate.minusWeeks(weeksAgo.toLong()).format(formatter)
                    }
                    date.endsWith("year ago") || date.endsWith("years ago") -> {
                        val yearsAgo = date.split(" ")[0].toInt()
                        currentDate.minusYears(yearsAgo.toLong()).format(formatter)
                    }
                    else -> {
                        try {
                            LocalDate.parse(date, formatter).format(formatter)
                        } catch (e: DateTimeParseException) {
                            ""
                        }
                    }
                }
                UpdateTaskScreen(
                    selectedDate = mutableStateOf(formattedDate ) ,
                    selectedTime = mutableStateOf(time) ,
                    textValue = message,
                    id = id,
                    openKeyboard = false,
                    onDismiss = { selectedItemId.value = ""  },
                    onMarkCompletedClick = onMarkCompletedClick,
                    onDeleteClick,
                    isPickerOpen,
                    isAddDaskOpen = isAddDaskOpen,
                    index = index
                )
            }
        

    }


}
@SuppressLint("UnrememberedMutableState")
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun FloatingActionButton(isAddDaskScreenOpen:MutableState<Boolean>,isPickerOpen: MutableState<Boolean>){
    val shadowOpacity = 0.4f // Set the desired opacity value (between 0 and 1)
    val shadowColor = Color.Black.copy(alpha = shadowOpacity)
    var visible by remember { mutableStateOf(false) }

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
    val elevation by animateDpAsState(
        targetValue = if (visible) 24.dp else 0.dp, // Set your desired elevation values here
        animationSpec = tween(
            durationMillis = 400,
            delayMillis = 900,
            easing = EaseOutCirc
        )
    )
    val elevationOpacity by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(
            durationMillis = 400,
            delayMillis = 300,
            easing = LinearEasing
        )
    )
    Box(modifier = Modifier
        .fillMaxWidth()
        .fillMaxHeight()
        .padding(bottom = 64.dp)
        .background(Color.Transparent)
    ) {
        androidx.compose.material.FloatingActionButton(
            modifier = Modifier
                .size(84.dp)
                .offset(y = offsetYSecond)
                .alpha(opacitySecond)
                .zIndex(opacitySecond)
                .align(Alignment.BottomCenter)
                .bounceClick()
                    ,
           onClick = {isAddDaskScreenOpen.value = true},
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
         modifier = Modifier
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
fun TopSectionHomeScreen(@DrawableRes image:Int
                         ,navController: NavController,
                         isMarkCompletedOpen:MutableState<Boolean>,
                         selectedMarkedItemId: MutableState<String>){
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        visible = true
    }
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

        .padding(top = 24.dp, start = 24.dp, end = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        val shape = RoundedCornerShape(16.dp)
        Text(
            text = "Do Things",
            fontFamily = interDisplayFamily,
            fontWeight = FontWeight.Black,
            fontSize = 24.sp,
            color = Color.White,
            modifier = Modifier
                .offset(y = offsetY)
                .alpha(opacity)
        )
        Box(modifier = Modifier
            .size(48.dp)
            .offset(y = offsetYSecond)
            .alpha(opacitySecond)
            .clickable(indication = null,
                interactionSource = remember { MutableInteractionSource() }) {
                isMarkCompletedOpen.value = true
            }
            .clip(shape)
            .background(color = SmallBox)

        ) {
            Image(
                painter = painterResource(image),
                contentDescription = "home_icon",
                modifier = Modifier
                    .size(24.dp)
                    .align(Alignment.Center)
            )
        }
        if (isMarkCompletedOpen.value){
            MarkCompletedScreen(
                navController = navController,
                onDismiss = { isMarkCompletedOpen.value = false },
                selectedMarkedItemId
            )
        }
    }
}
