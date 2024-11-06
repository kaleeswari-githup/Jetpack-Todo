package com.firstyogi.dothing


import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.glance.appwidget.Tracing.enabled
import androidx.glance.text.TextStyle
import androidx.navigation.NavController

import com.firstyogi.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.HashMap


@OptIn(ExperimentalSharedTransitionApi::class)
@SuppressLint("RememberReturnType", "SuspiciousIndentation")
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun AddDaskScreen(
    navController: NavController,
    selectedDate: MutableState<LocalDate?>,
    selectedTime: MutableState<LocalTime?>,
    textValue:String,
    isChecked: MutableState<Boolean>,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope
    ) {
    var task = rememberSaveable {
        mutableStateOf(textValue)
    }
    val repeatableOption = remember {
        mutableStateOf("NO REPEAT")
    }
    val softwareKeyboardController = LocalSoftwareKeyboardController.current
    var isPickerOpen = remember { mutableStateOf(false) }
      val context = LocalContext.current
    val mutableSelectedTime = remember { mutableStateOf(selectedTime) }
    val mutableSelectedDate = remember { mutableStateOf(selectedDate) }
    val database = FirebaseDatabase.getInstance()
    val user = FirebaseAuth.getInstance().currentUser
    val uid = user?.uid

    val databaseRef:DatabaseReference = database.reference.child("Task").child(uid.toString())
    val id:String = databaseRef.push().key.toString()
    val maxValue = 32

    val onDoneClick:() -> Unit = {
        val trimmedText = task.value.trim()
        val areNotificationsEnabled = areNotificationsEnabled(context)
        val timeFormat = if (selectedTime != null && selectedTime.value != null) {
            selectedTime.value!!.format(DateTimeFormatter.ofPattern("hh:mm a",Locale.ENGLISH)).toUpperCase()
        } else {
            null
        }
        val formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy")
        val formattedDate = if (selectedDate != null && selectedDate.value != null) {
            selectedDate.value!!.format(formatter)
        } else {
            null
        }


        val messageText = if (trimmedText.isNullOrBlank()) null else trimmedText
        val today = LocalDate.now()

        val userSelectedDate = if (formattedDate.isNullOrBlank()) {
            null
        } else {
            val selectedLocalDate = LocalDate.parse(formattedDate, formatter)
            if (selectedLocalDate.isBefore(today)) {
                // If selected date is in the past, use today's date instead
                today.format(formatter)
            } else {
                formattedDate
            }
        }
        val userSelectedTime = if (timeFormat.isNullOrBlank()) null else timeFormat

        val notificationTime: Long? = if (userSelectedDate != null && userSelectedTime != null) {
            val combinedDateTime = "$userSelectedDate $userSelectedTime"
            val dateTimeFormat = SimpleDateFormat("MM/dd/yyyy hh:mm a", Locale.ENGLISH)
            val date: Date = dateTimeFormat.parse(combinedDateTime)
            date?.time
        } else {
            null
        }

       /* val formattedDateForWidget = if (!userSelectedDate.isNullOrBlank()) {
            val parsedLocalDate = LocalDate.parse(userSelectedDate, DateTimeFormatter.ofPattern("MM/dd/yyyy"))
            parsedLocalDate.format(DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale.ENGLISH))
        } else {
            null
        }*/
        if (userSelectedDate != null) {
            val parsedDate = LocalDate.parse(userSelectedDate, formatter)
        } else {
            // Handle the case where userSelectedDate is null
            Log.e("AddDaskScreen", "userSelectedDate is null")
        }

       // val formattedDateWidget = formatDate(formattedDateForWidget)

        val nextDueDate = calculateNextDueDate(notificationTime ?: 0L, repeatableOption.value)
        val nextDueDateForCompletedTask = if (nextDueDate != 0L) {
            val dateFormat = SimpleDateFormat("MM/dd/yyyy", Locale.ENGLISH)
            dateFormat.format(Date(nextDueDate))
        } else {
            null
        }
        val data = DataClass(id,
            messageText ?: "",
            userSelectedTime ?: "",
            date = userSelectedDate ?: "",
            notificationTime =notificationTime ?: 0L ,
            repeatedTaskTime = repeatableOption.value,
            nextDueDate = nextDueDate,
            nextDueDateForCompletedTask = nextDueDateForCompletedTask,
            

            )
        databaseRef.child(id).setValue(data)
        softwareKeyboardController!!.hide()
        navController.popBackStack()


    }
    val onRepeatedDelete:()->Unit ={
        val data = DataClass(repeatedTaskTime = "NO REPEATE")
        databaseRef.child(id).setValue(data)
    }

    val blurEffectBackground by animateDpAsState(targetValue = when{
        isPickerOpen.value -> 10.dp
        else -> 0.dp
    }
    )
    var visible by remember {
        mutableStateOf(false)
    }
    LaunchedEffect(Unit) {
        visible = true
    }
    val offsetY by animateDpAsState(
        targetValue = if (visible) 0.dp else 42.dp,
        animationSpec = tween(durationMillis = 300, delayMillis = 100,easing = EaseOutCirc)
    )
    val opacity by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = keyframes {
            durationMillis = 300
            0.3f at 100
            0.6f at 200
            1f at 300
        }
    )

        Box(modifier = Modifier
            .fillMaxSize()

            .blur(blurEffectBackground)
            .background(color = MaterialTheme.colors.background)
            .clickable(indication = null,
                interactionSource = remember { MutableInteractionSource() }) {
                softwareKeyboardController!!.hide()
                navController.popBackStack()
            }){
            ThemedGridImage()
            CanvasShadow()
            Column(modifier = Modifier,
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ){
                AddDaskCircleDesign( mutableSelectedDate.value,mutableSelectedTime.value,task = task, onTaskChange = {newTask ->
                    if (newTask.length <= maxValue){
                        task.value = newTask
                    }
                },onDoneClick = onDoneClick,
                    isPickerOpen = isPickerOpen,
                    isChecked = isChecked,
                    repeatableOption = repeatableOption,
                    id = id,
                    animatedVisibilityScope = animatedVisibilityScope,
                    sharedTransitionScope = sharedTransitionScope,
                    softwareKeyboardController = softwareKeyboardController!!
                )
                Box(modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = offsetY)
                    .alpha(opacity)
                    .padding(top = 32.dp)
                ){
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(width = 105.dp, height = 48.dp)
                                .bounceClick()
                                .background(
                                    shape = RoundedCornerShape(53.dp),
                                    color = MaterialTheme.colors.primary
                                )
                                .clickable(indication = null,
                                    interactionSource = remember { MutableInteractionSource() }) {
                                    softwareKeyboardController.hide()
                                    navController.popBackStack()

                                },
                            contentAlignment = Alignment.Center
                        ) {
                            ButtonTextWhiteTheme(text = "CANCEL",color = MaterialTheme.colors.secondary)
                        }

                        Spacer(modifier = Modifier.padding(40.dp))
                        Button(onClick = {
                            onDoneClick.invoke()
                        },
                            shape = RoundedCornerShape(53.dp),
                            modifier = Modifier
                                .size(width = 105.dp, height = 48.dp)
                                .bounceClick(),
                            colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.secondary),
                            elevation = ButtonDefaults.elevation(0.dp)
                        ) {

                            ButtonTextDarkTheme(text = "SAVE")

                        }
                    }


                }
            }
            CrossFloatingActionButton {
                softwareKeyboardController!!.hide()
                navController.popBackStack()

            }
        }


}
private fun areNotificationsEnabled(context:Context): Boolean {
    val notificationManager = NotificationManagerCompat.from(context)
    return notificationManager.areNotificationsEnabled()
}

/*private fun areNotificationsEnabled(context: Context): Boolean {
    val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    // For Android 8.0 (Oreo) and above, check notification channels
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = notificationManager.getNotificationChannel(channelID)
        return channel != null && channel.importance != NotificationManager.IMPORTANCE_NONE
    }

    // For versions prior to Oreo, check if notifications are enabled
    return NotificationManagerCompat.from(context).areNotificationsEnabled()
}*/

@SuppressLint("SuspiciousIndentation")
@OptIn(ExperimentalComposeUiApi::class, ExperimentalAnimationApi::class,
    ExperimentalSharedTransitionApi::class
)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun AddDaskCircleDesign(
    selectedDate: MutableState<LocalDate?>,
    selectedTime: MutableState<LocalTime?>,
    task:MutableState<String>,
    onTaskChange: (String) -> Unit,
    onDoneClick: () -> Unit,
    isPickerOpen: MutableState<Boolean>,
    isChecked: MutableState<Boolean>,
    repeatableOption: MutableState<String>,
    id:String,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope:AnimatedVisibilityScope,
    softwareKeyboardController: SoftwareKeyboardController

){
    var isClickable = remember { mutableStateOf(true) }
    val focusRequester = remember { FocusRequester() }
    var visible by remember {
        mutableStateOf(false)
    }
    LaunchedEffect(Unit) {
        visible = true

    }
    Log.d("RepeatText","$repeatableOption.value")
    var isClicked = remember { mutableStateOf(false) }
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
                .padding(start = 24.dp, end = 24.dp, top = 52.dp)
                .size(344.dp)
                .sharedBounds(
                    rememberSharedContentState("addtask"),
                    animatedVisibilityScope = animatedVisibilityScope,
                    enter = fadeIn(tween(durationMillis = 300, easing = EaseOutBack)),
                    exit = fadeOut(tween(durationMillis = 300, easing = EaseOutBack)),


                    )

                //.offset(y = offsetY)
                // .scale(scale)
                .aspectRatio(1f)
                .clip(CircleShape)
                .background(MaterialTheme.colors.primary, shape = CircleShape)
                .clickable(indication = null,
                    interactionSource = remember { MutableInteractionSource() }) { },

            contentAlignment = Alignment.Center
        ) {
            Column(modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                val customTextSelectionColors = TextSelectionColors(
                    handleColor = Color.Red,
                    backgroundColor = Color.Red.copy(alpha = 0.4f),

                    )
                CompositionLocalProvider(LocalTextSelectionColors provides customTextSelectionColors){
                    TextField(
                        value = task.value,
                        onValueChange = onTaskChange ,
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight()
                            .padding(start = 32.dp, end = 32.dp)
                            .focusRequester(focusRequester)
                            .onFocusChanged { focusState ->
                                if (focusState.isFocused) {
                                    focusRequester.requestFocus()
                                    softwareKeyboardController?.show()
                                }
                            },

                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Done,
                            capitalization = KeyboardCapitalization.Sentences),
                        maxLines = Int.MAX_VALUE ,
                        keyboardActions = KeyboardActions(
                            onDone ={
                                onDoneClick()
                            }
                        ),
                        colors = TextFieldDefaults.textFieldColors(
                            backgroundColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            cursorColor = FABRed,
                        ),
                        placeholder = {
                            Text(text = "Task name",
                                modifier = Modifier
                                    .fillMaxWidth(),
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Medium,
                                fontSize = 24.sp,
                                color = MaterialTheme.colors.secondary.copy(alpha = 0.5f),
                                fontFamily = interDisplayFamily,
                                style = androidx.compose.ui.text.TextStyle(letterSpacing = 0.sp)
                            )
                        },

                        textStyle = LocalTextStyle.current.copy(
                            textAlign = TextAlign.Center,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = interDisplayFamily,
                            color = MaterialTheme.colors.secondary,
                            letterSpacing = 1.sp

                        ),
                        singleLine = false
                    )
                }


                TextStyle(text = "${task.value.length} / 32")
                Box(
                    modifier = Modifier
                        .wrapContentSize(Alignment.Center)
                        .padding(top = 20.dp)
                        .bounceClick()
                        .clickable(indication = null,
                            interactionSource = remember { MutableInteractionSource() }) {
                            isPickerOpen.value = true
                        }
                        .border(
                            width = 0.4.dp,
                            color = MaterialTheme.colors.secondary, // Change to your desired border color
                            shape = CircleShape
                        )
                        .padding(8.dp)
                ) {
                    if (selectedDate.value == null && selectedTime.value == null){
                        ThemedCalendarImage(modifier = Modifier)
                    }else if(selectedDate.value != null && selectedTime.value == null) {
                        val formatter = DateTimeFormatter.ofPattern("EEE, d MMM yyyy")
                        val formattedDate = selectedDate.value?.format(formatter) ?: ""
                        Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            ThemedCalendarImage(modifier = Modifier)
                            Text(
                                text = formattedDate,
                                fontFamily = interDisplayFamily,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colors.secondary,
                                style = androidx.compose.ui.text.TextStyle(letterSpacing = 0.sp)
                            )
                        }
                    }else{
                        val formatter = DateTimeFormatter.ofPattern("EEE, d MMM yyyy",Locale.ENGLISH)
                        val formattedDate = selectedDate.value?.format(formatter) ?: ""
                        val dateString:String = formattedDate
                        val timeFormat = selectedTime.value?.format(DateTimeFormatter.ofPattern("hh:mm a",Locale.ENGLISH))?.toUpperCase() ?: ""
                        val timeString:String = timeFormat
                        Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            ThemedCalendarImage(modifier =Modifier)
                            Text(
                                text = dateString,
                                fontFamily = interDisplayFamily,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colors.secondary,
                                style = androidx.compose.ui.text.TextStyle(letterSpacing = 0.sp)
                            )
                            Text(
                                text = ", $timeString",
                                fontFamily = interDisplayFamily,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colors.secondary,
                                style = androidx.compose.ui.text.TextStyle(letterSpacing = 0.sp))

                        }
                    }
                    if (isPickerOpen.value) {
                        UpdatedCalendarAndTimePickerScreen(
                            onDismiss = { isPickerOpen.value = false
                                softwareKeyboardController?.show()},
                            onDateTimeSelected = { date, time ->
                                val dateFormatter = DateTimeFormatter.ofPattern("MM/dd/yyyy")
                                val parsedDate = LocalDate.parse(date, dateFormatter)

                                val timeFormatter = DateTimeFormatter.ofPattern("hh:mm a",Locale.ENGLISH)
                                val parsedTime = if (time.isNotEmpty()) {
                                    LocalTime.parse(time, timeFormatter)
                                } else {
                                    null
                                }
                                selectedDate.value = parsedDate
                                selectedTime.value = parsedTime

                            },
                            id = "",
                            userSelectedDate = if (selectedDate.value == null) null else selectedDate.value,
                            userSelectedTime = if (selectedDate.value == null) null else selectedTime.value?.format(DateTimeFormatter.ofPattern("hh:mm a",Locale.ENGLISH)),
                            invokeOnDoneClick = false,

                            isChecked = isChecked,
                            message = task,
                            isClicked = isClicked,
                            repeatableOption = repeatableOption,




                            )
                    }
                }

                RepeatedTaskBoxImplement(repeatableOption = repeatableOption,
                    isPickerOpen =  isPickerOpen,
                    isClicked = isClicked,
                    id = id,
                    addtaskCrossClick = true,
                    unMarkCompletedCrossClick = false,
                    color = MaterialTheme.colors.secondary,
                    modifier = Modifier,
                    isClickable = isClickable)

            }

        }

    }

    LaunchedEffect(Unit) {

            isClickable.value = true

    }
    LaunchedEffect(Unit) {
        delay(100)
        focusRequester.requestFocus()
        softwareKeyboardController?.show()
    }

}

@Composable
fun RepeatedTaskBoxImplement(repeatableOption:MutableState<String>,
                             isPickerOpen: MutableState<Boolean>,
                             isClicked:MutableState<Boolean>,
                             id:String,
                             addtaskCrossClick:Boolean = true,
                             updatetaskCrossClick:Boolean = true,
                             unMarkCompletedCrossClick : Boolean = true,
                             color: Color,
                             modifier:Modifier,
                             isClickable:MutableState<Boolean>
                             ){

    var isBoxVisible by remember { mutableStateOf(true) }
    val database = FirebaseDatabase.getInstance()
    val user = FirebaseAuth.getInstance().currentUser
    val uid = user?.uid
    val databaseRef: DatabaseReference = database.reference.child("Task").child(uid.toString())
    val completedTasksRef = database.reference.child("Task").child("CompletedTasks").child(uid.toString())
Log.d("CheckWrongTime","$repeatableOption.value")
    val coroutineScope = rememberCoroutineScope()
    if (isBoxVisible && repeatableOption.value in listOf("DAILY","WEEKLY","MONTHLY","YEARLY") ){
        val context = LocalContext.current

        Box(
            modifier = Modifier
                .wrapContentSize(Alignment.Center)
                .padding(top = 20.dp)
                .then(if (isClickable.value) Modifier.bounceClick() else Modifier)
                .clickable (
                    enabled = isClickable.value,
                    onClick = {
                        isPickerOpen.value = true
                        isClicked.value = true
                    }
                )


                .border(
                    width = 0.4.dp,
                    color = color, // Change to your desired border color
                    shape = CircleShape
                )
                .padding(8.dp)
        ){
            Row(modifier = Modifier
                .padding(start = 8.dp, end = 8.dp)
                ,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Image(painter = painterResource(id = R.drawable.repeated_task_icon),
                    contentDescription = null,
                    modifier = modifier
                       )
                Text(text = repeatableOption.value,
                    fontFamily = interDisplayFamily,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = color,)
                Box(modifier = Modifier
                    .clickable (
                        enabled = isClickable.value,
                        onClick = {
                            repeatableOption.value = "NO REPEAT"
                            if (addtaskCrossClick){
                                databaseRef.child(id).child("repeatedTaskTime").setValue("NO REPEAT")
                                isBoxVisible = false
                            }
                            if (updatetaskCrossClick){
                                coroutineScope.launch {
                                    try {
                                        val taskRef = database.reference.child("Task")
                                            .child(uid.toString())
                                            .child(id)

                                        val snapshot = taskRef.get().await()
                                        val data = snapshot.getValue(DataClass::class.java)

                                        if (data != null) {
                                            // Get current date as string
                                            val currentDate = SimpleDateFormat("MM/dd/yyyy", Locale.ENGLISH)
                                                .format(Date())

                                            // Create the updates map with non-null values
                                            val updates = mutableMapOf<String, Any>()

                                            // Add required updates with null checks
                                            updates["repeatedTaskTime"] = "NO REPEAT"
                                            updates["nextDueDate"] = data.notificationTime // This is Long so no type mismatch
                                            updates["nextDueDateForCompletedTask"] = currentDate // Using non-null current date
                                            updates["date"] = currentDate

                                            // Update in Firebase
                                            taskRef.updateChildren(updates).await()

                                            // Cancel notifications since it's no longer repeating
                                            cancelNotification(context, id)
                                            cancelNotificationManger(context, id)

                                            // Update local UI
                                            isBoxVisible = false

                                            Log.d("UpdateCross", "Successfully updated task $id with new dates")
                                        }
                                    } catch (e: Exception) {
                                        Log.e("UpdateCross", "Error updating task: ${e.message}")
                                    }
                                }
                                
}
                            if (unMarkCompletedCrossClick){
                                completedTasksRef.child(id).child("repeatedTaskTime").setValue("NO REPEAT")
                                isBoxVisible = false
                            }
                        }

                    )



                    ){
                    ThemedCrossImage(modifier = modifier)
                }




            }
        }


    }else if ( repeatableOption.value in listOf("DAILY","WEEKLY","MONTHLY","YEARLY")  ) {
        // Set isBoxVisible to true when repeatableOption is "DAILY"
        isBoxVisible = true
    }
}
@Composable
fun ThemedCalendarImage(modifier: Modifier) {
    val isDarkTheme = isSystemInDarkTheme()
    val imageRes = if (isDarkTheme) {
        R.drawable.dark_calendar_icon
    } else {
        R.drawable.light_calendar_icon
    }

    Image(
        painter = painterResource(id = imageRes),
        contentDescription = null,
        modifier = modifier
        )
}
@Composable
fun TextStyle(text:String) {
    Text(text = text,
    fontFamily = interDisplayFamily,
    fontWeight = FontWeight.Medium,
    fontSize = 11.sp,
    color = MaterialTheme.colors.secondary.copy(alpha = 0.25f)
   )
}
@Composable
fun ThemedTickImage() {
    val isDarkTheme = isSystemInDarkTheme()
    val imageRes = if (isDarkTheme) {
        R.drawable.dark_tick
    } else {
        R.drawable.light_tick
    }

    Image(
        painter = painterResource(id = imageRes),
        contentDescription = null,
    )
}

