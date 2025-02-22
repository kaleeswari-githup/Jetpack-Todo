package com.firstyogi.dothing

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Build
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.annotation.RequiresApi
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material.*
import androidx.compose.material3.DrawerState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.toUpperCase
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.firstyogi.dothing.*
import com.firstyogi.ui.theme.FABRed
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.*
import kotlin.math.absoluteValue
import kotlin.math.roundToInt


@OptIn(ExperimentalSharedTransitionApi::class)
@SuppressLint("UnrememberedMutableState", "UnusedMaterialScaffoldPaddingParameter")
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun UpdateTaskScreen(

    navController: NavController,
    id: String?,
    openKeyboard: Boolean,
    isChecked:MutableState<Boolean>,
    snackbarHostState:SnackbarHostState,
    coroutineScope:CoroutineScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    sharedTransitionScope: SharedTransitionScope

) {
    val repeatableOption = remember {
        mutableStateOf("")
    }
    var isUpdatePickerOpen = remember { mutableStateOf(false) }
    val maxValue = 32
    val database = FirebaseDatabase.getInstance()
    val user = FirebaseAuth.getInstance().currentUser
    val uid = user?.uid
    val databaseRef: DatabaseReference = database.reference.child("Task").child(uid.toString())
    var data by remember { mutableStateOf<DataClass?>(null) }
    var dataClassMessage = remember { mutableStateOf("") }
    var dataClassDate= remember { mutableStateOf("") }
    var dataClassTime = remember{ mutableStateOf("") }
   // var animationID = remember{ mutableStateOf("") }
    val contextx = LocalContext.current as Activity

    var updateScreenvisible = remember {
        mutableStateOf(false)
    }
    LaunchedEffect(Unit) {
        updateScreenvisible.value = true // Set the visibility to true to trigger the animation
    }

    DisposableEffect(Unit) {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val selectedData = snapshot.child(id.toString()).getValue(DataClass::class.java)
                if (selectedData != null) {
                    data = selectedData

                        val originalDateFormat = DateTimeFormatter.ofPattern("MM/dd/yyyy")
                        val desiredDateFormat = DateTimeFormatter.ofPattern("EEE, d MMM yyyy",Locale.ENGLISH)

                        // Check if the date is not empty before attempting to parse
                    if (!selectedData.date.isNullOrBlank()) {
                        try {
                            // Convert Long to LocalDate
                            val parsedDate = LocalDate.parse(selectedData.date, originalDateFormat)
                            dataClassDate.value = parsedDate.format(desiredDateFormat)
                        } catch (e: DateTimeParseException) {
                            // Handle parsing error if needed
                            Log.e("DateParsingError", "Error parsing date: ${selectedData.date}", e)
                        }
                    } else {
                        // Handle the case where the date is empty
                        dataClassDate.value = ""
                    }
                    dataClassMessage.value = selectedData.message ?: ""
                    dataClassTime.value = selectedData.time?:""
                    repeatableOption.value = selectedData.repeatedTaskTime?:""
                   // animationID.value = selectedData.id
                    Log.d("updatedataclassname","${selectedData.message}")
                }
            }

            override fun onCancelled(error: DatabaseError) {
            }
        }

        databaseRef.addListenerForSingleValueEvent(listener)

        onDispose {
            databaseRef.removeEventListener(listener)
        }
    }

//Log.d("selectedDate","$selectedDate.value")
        var context = LocalContext.current
        val onDoneClick:(String,String) -> Unit = { updatedDate,updatedTime ->
            val originalDateFormat = DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale.ENGLISH)
            val desiredDateFormat = DateTimeFormatter.ofPattern("MM/dd/yyyy")
            val timeFormat = DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH) // Ensure to set the proper Locale

            val dateStringFromDatabase = updatedDate

            val originalDate: LocalDate? = if (dateStringFromDatabase.isNotEmpty()) {
                LocalDate.parse(dateStringFromDatabase, originalDateFormat)
            } else {
                null
            }

            val formattedDate = originalDate?.format(desiredDateFormat) ?: ""

            val timeFormats = listOf(
                DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH),
                DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH).withLocale(Locale.US),
            )

            var formattedTime: LocalTime? = null

            for (format in timeFormats) {
                try {
                    formattedTime = LocalTime.parse(updatedTime.toUpperCase(), format)
                    break
                } catch (e: DateTimeParseException) {
                }
            }

            val notificationTime: Long? = if (!formattedDate.isNullOrBlank() && formattedTime != null) {
                val dateTime = LocalDateTime.of(originalDate, formattedTime)
                dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            } else {
                null
            }
            val longDateValue: Long? = if (!formattedDate.isNullOrBlank()) {
                val date = LocalDate.parse(formattedDate, desiredDateFormat)
                date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            } else {
                null
            }
            val updatedData = DataClass(
                id = id.toString(),
                message = dataClassMessage.value.trim(),
                time = formattedTime?.format(timeFormat) ?: "",
                date = formattedDate,
                notificationTime = notificationTime ?: 0L,
               // dueDate = longDateValue ?:0L
            )

            val dataMap = mapOf(
                "id" to updatedData.id,
                "message" to updatedData.message,
                "time" to updatedData.time,
                "date" to updatedData.date,
                "notificationTime" to updatedData.notificationTime,
               // "longDateValue" to updatedData.dueDate
            )
            databaseRef.child(id.toString()).updateChildren(dataMap)
           // navController.navigate(Screen.Home.route)
            navController.popBackStack()
            updateScreenvisible.value = false
        }

    BackHandler {
        onDoneClick(dataClassDate.value, dataClassTime.value)

    }
    val onDeleteClick:(String) -> Unit = {clickedTaskId ->
            val databaseRef = database.reference.child("Task").child(uid.toString())
            val taskRef = database.reference.child("Task").child(uid.toString()).child(clickedTaskId)

            coroutineScope.launch {
                snackbarHostState.currentSnackbarData?.dismiss()
                val data = databaseRef.child(clickedTaskId).get().await().getValue(DataClass::class.java)

                if (data != null) {
                    databaseRef.child(clickedTaskId).removeValue()
                    cancelNotificationManger(context,clickedTaskId)
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
                        cancelNotificationManger(context,data.id)
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
                        }

                    }
                }
                override fun onCancelled(error: DatabaseError) {
                    Log.e("FirebaseError", "Database operation cancelled: $error")
                }
            })
        }
    var isDeleteTaskScreenOpen = remember {
        mutableStateOf(false)
    }
    val blurEffectBackground by animateDpAsState(targetValue = when{
        isUpdatePickerOpen.value -> 10.dp
        isDeleteTaskScreenOpen.value -> 10.dp
        else -> 0.dp
    }
    )
   /* val homeScreenContent: @Composable () -> Unit = {
        HomeScreen(
            animatedVisibilityScope = animatedVisibilityScope,
            navController = navController,
            snackbarHostState = snackbarHostState,
            coroutineScope = coroutineScope,
            sharedTransitionScope = sharedTransitionScope,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    // Slightly scale down and add transparency to the background screen
                    scaleX = 1f - (offsetY / (2 * threshold)).coerceIn(0f, 0.1f)
                    scaleY = 1f - (offsetY / (2 * threshold)).coerceIn(0f, 0.1f)
                    alpha = 1f - (offsetY / threshold).coerceIn(0f, 0.5f)
                }
        )
    }*/
    BackHandler {
        onDoneClick.invoke(dataClassDate.value, dataClassTime.value)
    }
    Log.d("UpdatePageMessage","$dataClassMessage")
   // var offsetY by remember { mutableStateOf(0f) }
  //  val offsetYAnim = remember { Animatable(0f) }
  //  val scaleAnim = remember { Animatable(1f) }
    val threshold = 5f
    val offsetY by remember { mutableStateOf(0f) }
    val offsetYAnim = remember { Animatable(0f) }
    val scaleAnim = remember { Animatable(1f) }
    val maxDragDistance = 600f // Max distance the screen can be dragged
    val navigationThreshold = 400f
    val maxOpacity = 0.5

// Animate back to the original position if the drag does not reach the threshold
    LaunchedEffect(offsetY) {
        if (offsetY <= threshold) {
            offsetYAnim.snapTo(offsetY)
        }
    }
    LaunchedEffect(offsetYAnim.value) {
        if (offsetYAnim.value <= threshold) {
            // If not past the threshold, animate back to the original position and scale
            scaleAnim.animateTo(1f, animationSpec = tween(durationMillis = 0, easing = EaseInOutElastic)) // Reset scale smoothly
            offsetYAnim.animateTo(0f, animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )) // Reset position smoothly
        }
    }
    val alpha by animateFloatAsState(
        targetValue = (1f - (offsetYAnim.value / threshold).coerceIn(0f, 1f)),
        animationSpec = tween(durationMillis = 500) // Optional: adjust for smoothness
    )
    val isNavigating = remember { mutableStateOf(false) }

        Surface() {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .blur(blurEffectBackground)
                    .background(color = MaterialTheme.colors.background)
                 .clickable(indication = null,
            interactionSource = remember { MutableInteractionSource() }) {
            onDoneClick.invoke(dataClassDate.value, dataClassTime.value)
            updateScreenvisible.value = false
        }
            ) {
                ThemedGridImage(modifier = Modifier)
                // CanvasShadow(modifier = Modifier.fillMaxSize())
                Box(
                    modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {

                        UpdateCircleDesign(
                            onTaskChange = { newTask ->
                                if (newTask.length <= maxValue) {
                                    dataClassMessage.value = newTask
                                }
                            },
                            id = id.toString(),
                            openKeyboard = openKeyboard,
                            isUpdatePickerOpen = isUpdatePickerOpen,
                            dataClassMessage = dataClassMessage,
                            selectedDate = dataClassDate,
                            selectedTime = dataClassTime,
                            isChecked = isChecked,
                            repeatableOption = repeatableOption,
                            animatedVisibilityScope = animatedVisibilityScope,
                            sharedTransitionScope = sharedTransitionScope,


                        )




                        Box(
                            modifier = Modifier
                                .padding(bottom = 40.dp)
                                .alpha(alpha)
                        ) {
                            UpdatedButtons(
                                id = id.toString(),
                                navController = navController,
                                onMarkCompletedClick = onMarkCompletedClick,
                                onDeleteClick = onDeleteClick,
                                isDeleteTaskScreenOpen = isDeleteTaskScreenOpen,
                                repeatOption = repeatableOption.value,
                                visible = updateScreenvisible
                            )

                        }
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 56.dp)
                        .alpha(alpha)
                    ,
                    contentAlignment = Alignment.TopEnd
                ) {
                    CrossFloatingActionButton(
                        onClick = {
                            onDoneClick.invoke(dataClassDate.value, dataClassTime.value)
                        },
                        visible = updateScreenvisible
                    )
                }

            }
        }




}
@OptIn(ExperimentalComposeUiApi::class, ExperimentalSharedTransitionApi::class)
@SuppressLint("SuspiciousIndentation")
@RequiresApi(Build.VERSION_CODES.O)

@Composable
fun UpdateCircleDesign(
    dataClassMessage: MutableState<String>,
    selectedDate: MutableState<String>,
    selectedTime: MutableState<String>,
    id:String,
    onTaskChange:(String) -> Unit,
    isUpdatePickerOpen:MutableState<Boolean>,
    openKeyboard:Boolean,
    isChecked: MutableState<Boolean>,
    repeatableOption: MutableState<String>,
    animatedVisibilityScope:AnimatedVisibilityScope,
    sharedTransitionScope: SharedTransitionScope,


){
    var isClickable = remember { mutableStateOf(true) }
val dataClassMessageMutable by remember{
    mutableStateOf(dataClassMessage)
}
    val isMessageFieldFocused = remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val database = FirebaseDatabase.getInstance()
    val user = FirebaseAuth.getInstance().currentUser
    val uid = user?.uid
    val databaseRef: DatabaseReference = database.reference.child("Task").child(uid.toString())
    val context = LocalContext.current
    val onDoneClick: () -> Unit = {
            val updatedData = HashMap<String, Any>()
            updatedData["id"] = id
            updatedData["message"] = dataClassMessageMutable.value

            databaseRef.child(id).updateChildren(updatedData)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {

                    } else {
                        Toast.makeText(context, task.exception.toString(), Toast.LENGTH_SHORT).show()
                    }
                }
        }
    var visible by remember {
        mutableStateOf(false)
    }
    var isClicked = remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        visible = true // Set the visibility to true to trigger the animation

    }

    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = spring(
            dampingRatio = 0.7f,
            stiffness = Spring.StiffnessVeryLow
        )
    )
    val offsetY by animateDpAsState(
        targetValue = if (visible) 0.dp else 400.dp,
        animationSpec = spring(
            dampingRatio = 0.45f,
            stiffness = Spring.StiffnessMediumLow
        )
    )
    with(sharedTransitionScope){
        Box(
            modifier = Modifier

                .fillMaxWidth()

                .padding(start = 24.dp, end = 24.dp, top = 88.dp)
                // .size(344.dp)
                .sharedBounds(
                    rememberSharedContentState(key = "bounds-$id"),

                    animatedVisibilityScope = animatedVisibilityScope,
                   //   enter = fadeIn(tween(durationMillis = 300, easing = EaseOutBack)),
                    //   exit = fadeOut(tween(durationMillis = 300, easing = EaseOutBack)),
                    boundsTransform = { initialRect, targetRect ->
                        spring(
                            dampingRatio = 0.8f,
                            stiffness = 380f
                        )

                    },

                    /* enter = scaleIn(
                        spring(
                            dampingRatio = 0.8f,
                            stiffness = 380f

                        )
                    ),
                    exit = fadeOut(
                        tween(
                            durationMillis = 50,
                            easing = Ease,
                        )
                    ),*/
                    placeHolderSize = SharedTransitionScope.PlaceHolderSize.animatedSize
                )
                // .offset(y = offsetY)
                //.scale(scale)
                .aspectRatio(1f)
                .clip(CircleShape)
                .background(color = MaterialTheme.colors.primary, shape = CircleShape)

                .clickable(indication = null,
                    interactionSource = remember { MutableInteractionSource() }) { },
            contentAlignment = Alignment.Center
        ){
            val customTextSelectionColors = TextSelectionColors(
                handleColor = Color.Red,
                backgroundColor = Color.Red.copy(alpha = 0.4f)
            )
            Column(modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center) {
                CompositionLocalProvider(LocalTextSelectionColors provides customTextSelectionColors){
                    TextField(
                        value = dataClassMessage.value,
                        onValueChange = onTaskChange ,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(90.dp)
                            .wrapContentHeight()
                            /* .sharedElement(
                                state = rememberSharedContentState(key = "boundsMessage-$id"),
                                animatedVisibilityScope = animatedVisibilityScope,
                                boundsTransform = { _,_, ->
                                    tween(300)
                                }
                            )*/
                            .padding(start = 32.dp, end = 32.dp)
                            .focusRequester(focusRequester)
                            .onFocusChanged { focusState ->
                                if (focusState.isFocused) {
                                    isMessageFieldFocused.value = true
                                } else {
                                    keyboardController?.hide()
                                    isMessageFieldFocused.value = false
                                }
                            },
                        maxLines = 3,

                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done,
                            capitalization = KeyboardCapitalization.Sentences),
                        keyboardActions = KeyboardActions(
                            onDone ={
                                keyboardController?.hide()
                                focusManager.clearFocus(true)
                                onDoneClick.invoke()
                            }
                        ),
                        colors = TextFieldDefaults.textFieldColors(
                            backgroundColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            cursorColor = FABRed
                        ),
                        placeholder = {
                            Text(text = "Task name",
                                modifier = Modifier
                                    .fillMaxWidth()
                                ,
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Medium,
                                fontSize = 24.sp,
                                fontFamily = interDisplayFamily,
                                color = MaterialTheme.colors.secondary.copy(alpha = 0.5f),
                                style = androidx.compose.ui.text.TextStyle(letterSpacing = 0.sp)
                            )
                        },

                        textStyle = LocalTextStyle.current.copy(
                            textAlign = TextAlign.Center,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = interDisplayFamily,
                            color = MaterialTheme.colors.secondary,
                            letterSpacing = -1.sp
                        ),

                        )
                }

                if (isMessageFieldFocused.value){
                    TextStyle(text = "${dataClassMessage.value.length} / 32")
                }

                Box(
                    modifier = Modifier
                        // .wrapContentSize(Alignment.Center)
                        .padding(
                            top = 20.dp, start = 8.dp, end = 8.dp
                        )

                        .bounceClick()
                        .clickable(indication = null,
                            interactionSource = remember { MutableInteractionSource() }) {
                            isUpdatePickerOpen.value = true
                        }
                        /* .sharedElement(
                            state = rememberSharedContentState(key = "boundsDateandTime-$id"),
                            animatedVisibilityScope = animatedVisibilityScope,
                            boundsTransform = { _,_, ->
                                tween(300)
                            }
                        )*/
                        .border(
                            width = 0.4.dp,
                            color = MaterialTheme.colors.secondary, // Change to your desired border color
                            shape = CircleShape
                        )
                        .padding(8.dp)

                ) {
                    val timeFormat = selectedTime.value.format(DateTimeFormatter.ofPattern("hh:mm a",Locale.ENGLISH))?.toUpperCase() ?: ""
                    val timeString:String = timeFormat
                    if (selectedDate.value.isNullOrEmpty() && selectedTime.value.isNullOrEmpty()){
                        ThemedCalendarImage(modifier = Modifier)
                    }else if(selectedDate.value.isNotEmpty() && selectedTime.value.isNullOrEmpty()) {
                        Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            ThemedCalendarImage(modifier = Modifier)
                            Text(
                                text = selectedDate.value.toUpperCase(),
                                fontFamily = interDisplayFamily,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colors.secondary,
                                style = androidx.compose.ui.text.TextStyle(letterSpacing = 0.sp)
                            )
                        }
                    }else{
                        Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            ThemedCalendarImage(modifier = Modifier)
                            Text(
                                text = "${selectedDate.value.toUpperCase()}, ${timeString}",
                                fontFamily = interDisplayFamily,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colors.secondary,
                                style = androidx.compose.ui.text.TextStyle(letterSpacing = 0.sp)
                            )
                        }
                    }
                    Log.d("updatescreendate","${selectedDate}")
                    if (isUpdatePickerOpen.value) {
                        val pattern = "EEE, d MMM yyyy"
                        val locale = Locale.ENGLISH
                        val formatter = DateTimeFormatter.ofPattern(pattern, locale)
                            .withZone(ZoneId.of("America/New_York"))
                        val selectedDateString = selectedDate.value
                        val localDate = if (selectedDateString.isEmpty()) {
                            LocalDate.now()
                        } else {
                            try {
                                LocalDate.parse(selectedDateString, formatter)
                            } catch (e: DateTimeParseException) {
                                LocalDate.now()
                            }
                        }
                        UpdatedCalendarAndTimePickerScreen(
                            userSelectedDate = localDate,
                            userSelectedTime = selectedTime.value,
                            onDismiss = { isUpdatePickerOpen.value = false
                                keyboardController?.show()},
                            onDateTimeSelected = { date, time ->
                                val defaultDateFormat = DateTimeFormatter.ofPattern("MM/dd/yyyy")
                                val desiredDateFormat = DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale.ENGLISH)
                                val defaultDateString = date
                                val parsedDate = LocalDate.parse(defaultDateString, defaultDateFormat)
                                val formattedDate = parsedDate.format(desiredDateFormat)
                                selectedDate.value = formattedDate
                                selectedTime.value = time
                            },
                            id = id,
                            invokeOnDoneClick = true,
                            isChecked = isChecked,
                            message = dataClassMessage,
                            repeatableOption = repeatableOption,
                            isClicked = isClicked,

                            )
                    }
                }

                RepeatedTaskBoxImplement(
                    repeatableOption = repeatableOption,
                    isPickerOpen = isUpdatePickerOpen,
                    isClicked = isClicked,
                    id = id,
                    addtaskCrossClick = false,
                    unMarkCompletedCrossClick = false,
                    updatetaskCrossClick = true,
                    color = MaterialTheme.colors.secondary,
                    modifier = Modifier,
                    isClickable = isClickable
                )
            }
        }
    }

    LaunchedEffect(Unit) {

        isClickable.value = true

    }
    LaunchedEffect(openKeyboard) {
        if (openKeyboard) {
            focusRequester.requestFocus()
            keyboardController?.show()
        } else {
            keyboardController?.hide()
        }
    }
}
@SuppressLint("UnrememberedMutableState")
@Composable
fun UpdatedButtons(
    id: String,
    navController: NavController,
    onMarkCompletedClick: (String) -> Unit,
    onDeleteClick: (String) -> Unit,
    isDeleteTaskScreenOpen:MutableState<Boolean>,
    repeatOption: String?,
    visible:MutableState<Boolean>
){
    val coroutineScope = rememberCoroutineScope()



    val offsetY by animateDpAsState(
        targetValue = if (visible.value) 0.dp else 24.dp,
        animationSpec = tween(durationMillis = 300,easing = EaseOutCirc, delayMillis = 200)
    )
    val opacity by animateFloatAsState(
        targetValue = if (visible.value) 1f else 0f,
        animationSpec = keyframes {
            durationMillis = 300 // Total duration of the animation
            0.0f at 0 // Opacity becomes 0.3f after 200ms
            // Opacity becomes 0.6f after 500ms
            1f at 300

            delayMillis = 200// Opacity becomes 1f after 1000ms (end of the animation)
        }

    )
    AnimatedVisibility(
        visible = visible.value,

        enter = slideInVertically(
            initialOffsetY = { 96 }, // Starts off-screen at the top
            animationSpec = spring(
                dampingRatio = 0.6f,
                stiffness = Spring.StiffnessVeryLow

            )
        ),

        exit = slideOutVertically(
            targetOffsetY = { 96 }, // Exits off-screen at the bottom
            animationSpec = tween(
                durationMillis = 500,
                easing = EaseOutCirc,
            )
        ) + fadeOut( // Combine slide and opacity for exit
            animationSpec = tween(
                durationMillis = 300,
                easing = EaseOutCirc,

            )
        )
        ) {
        Box(modifier = Modifier
            .fillMaxWidth()
            // .wrapContentWidth()
            .padding(start = 24.dp, end = 38.dp)
            .height(48.dp)
            //.offset(y = offsetY)
            //.alpha(opacity)
            .background(color = MaterialTheme.colors.primary, shape = RoundedCornerShape(30.dp)),
            contentAlignment = Alignment.Center
        ) {
            Row(modifier = Modifier
                .fillMaxWidth()
                // .wrapContentWidth()
                .padding(start = 24.dp, end = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            )
            {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.clickable(indication = null,
                        interactionSource = remember { MutableInteractionSource() }) {
                        if (repeatOption in listOf("DAILY","WEEKLY","MONTHLY","YEARLY") ) {
                            isDeleteTaskScreenOpen.value = true
                        }else{
                            onDeleteClick(id)
                            visible.value = false
                            navController.popBackStack()
                        }

                    },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ThemedTrashImage()
                    ButtonTextWhiteTheme(text = "DELETE",color = MaterialTheme.colors.secondary, modifier = Modifier)
                }
                Box(
                    modifier = Modifier
                        .padding(start = 12.dp)
                        .width(1.dp)
                        .fillMaxHeight()
                        .background(color = MaterialTheme.colors.background)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .padding(start = 12.dp)
                        .clickable(indication = null,
                            interactionSource = remember { MutableInteractionSource() }) {
                            coroutineScope.launch {
                                onMarkCompletedClick(id)
                                visible.value = false
                                navController.popBackStack()
                            }
                        },
                    verticalAlignment = Alignment.CenterVertically) {
                    ThemedSquareImage(modifier = Modifier)
                    ButtonTextWhiteTheme(text = "MARK COMPLETED",color = MaterialTheme.colors.secondary,modifier = Modifier)
                }
            }

        }
    }


    if (isDeleteTaskScreenOpen.value){

            DeleteTaskScreenPage (
                onDismiss = {isDeleteTaskScreenOpen.value = false},
                onDeleteClick,
                navController,
                id = id,
                textHeading =  stringResource(id = R.string.delete_one_task_heading),
                textDiscription = stringResource(id = R.string.delete_one_task_subtitle)
            )
        }


}
@Composable
fun ThemedTrashImage() {
    val isDarkTheme = isSystemInDarkTheme()
    val imageRes = if (isDarkTheme) {
        R.drawable.dark_trash_delete
    } else {
        R.drawable.light_trash_delete
    }

    Image(
        painter = painterResource(id = imageRes),
        contentDescription = null,
    )
}
@Composable
fun ButtonTextWhiteTheme(text:String,color: Color,modifier: Modifier){
    Text(
        text = text,
        fontFamily = interDisplayFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        color = color,
       // style = androidx.compose.ui.text.TextStyle(letterSpacing = 1.sp),
        modifier = modifier,
        overflow = TextOverflow.Ellipsis

    )
}
@Composable
fun ButtonTextDarkTheme(text:String,modifier: Modifier){
    Text(
        text = text,
        fontFamily = interDisplayFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        color = MaterialTheme.colors.primary,
       // style = androidx.compose.ui.text.TextStyle(letterSpacing = 1.sp),
        modifier = modifier
    )
}

@Composable
fun CrossFloatingActionButton(
    onClick:() -> Unit,
    visible:MutableState<Boolean>
    ){
    AnimatedVisibility(
        visible = visible.value,

        enter = scaleIn(
            initialScale = 0.1f, // Scale starts from half the size
            animationSpec = tween(durationMillis = 200) // Animation duration
        ),

        exit = scaleOut(
            targetScale = 0.5f, // Scale shrinks to half the size
            animationSpec = tween(durationMillis = 200) // Animation duration
        )
    ){
        Box(modifier = Modifier
            .size(72.dp)
            .padding( end = 24.dp)
            .fillMaxWidth()
            .fillMaxHeight()
            .background(Color.Transparent)
        ) {
            androidx.compose.material.FloatingActionButton(
                modifier = Modifier
                    .size(48.dp)
                    .align(Alignment.TopEnd)
                    .bounceClick()
                ,
                elevation = FloatingActionButtonDefaults.elevation(0.dp),
                onClick = {onClick.invoke()},
                shape = CircleShape,
                backgroundColor = MaterialTheme.colors.primary

            ) {
                ThemedCrossImage(modifier = Modifier)
            }
        }
    }

}
@Composable
fun ThemedCrossImage(modifier: Modifier) {
    val isDarkTheme = isSystemInDarkTheme()
    val imageRes = if (isDarkTheme) {
        R.drawable.dark_cross_icon
    } else {
        R.drawable.light_cross_icon
    }

    Image(
        painter = painterResource(id = imageRes),
        contentDescription = null,
        modifier = modifier
    )
}

@Composable
fun PinchToDismissBox(
    modifier: Modifier = Modifier,
    navController: NavController,
    content: @Composable () -> Unit
) {
    var scale by remember { mutableStateOf(1f) }
    var size by remember { mutableStateOf(IntSize.Zero) }
    val dismissThreshold = 0.7f

    val animatedScale by animateFloatAsState(
        targetValue = scale,
        animationSpec = spring(stiffness = Spring.StiffnessLow)
    )

    val coroutineScope = rememberCoroutineScope()

    Box(
        modifier = modifier
            .onGloballyPositioned { size = it.size }
            .graphicsLayer(
                scaleX = animatedScale,
                scaleY = animatedScale
            )
            .pointerInput(Unit) {
                detectTransformGestures { _, _, zoom, _ ->
                    scale *= zoom
                    if (scale < dismissThreshold) {
                        coroutineScope.launch {
                            animate(scale, 0f) { value, _ ->
                                scale = value
                                if (value <= 0.1f) {
                                    navController.popBackStack()
                                }
                            }
                        }
                    }
                }
            }
    ) {
        content()
    }
}
