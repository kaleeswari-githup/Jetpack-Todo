package com.firstyogi.dothing


import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
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
import androidx.navigation.NavController

import com.firstyogi.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.*


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

    ) {
    var task = rememberSaveable {
        mutableStateOf(textValue)
    }
    val repeatableOption = remember {
        mutableStateOf("No Repeat")
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
    var addTaskvisible = remember {
        mutableStateOf(false)
    }


    LaunchedEffect(Unit) {
        addTaskvisible.value = true
    }
    val onDoneClick: () -> Unit = {

        val trimmedText = task.value.trim()
        val areNotificationsEnabled = areNotificationsEnabled(context)
        val timeFormat = selectedTime?.value?.let {
            it.format(DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH)).toUpperCase()
        }

        val formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy")
        val formattedDate = selectedDate?.value?.format(formatter)

        val messageText = if (trimmedText.isBlank()) null else trimmedText
        val today = LocalDate.now()

        // Ensure that the selected date is not in the past.
        val userSelectedDate = if (formattedDate.isNullOrBlank()) {
            null
        } else {
            val selectedLocalDate = LocalDate.parse(formattedDate, formatter)
            if (selectedLocalDate.isBefore(today)) {
                // If the date is in the past, use today's date.
                today.format(formatter)
            } else {
                formattedDate
            }
        }

        // If both date and time are provided, calculate the notification time
        val notificationTime: Long? = if (!userSelectedDate.isNullOrBlank() && !timeFormat.isNullOrBlank()) {
            val combinedDateTime = "$userSelectedDate $timeFormat"
            val dateTimeFormat = SimpleDateFormat("MM/dd/yyyy hh:mm a", Locale.ENGLISH)
            dateTimeFormat.parse(combinedDateTime)?.time
        } else {
            // Set notificationTime to null if either date or time is missing
            null
        }

        // Skip the calculation if notificationTime is null
        val nextDueDate = if (notificationTime != null) {
            calculateNextDueDate(notificationTime, repeatableOption.value)
        } else {
            null  // Or set this to a default value like current time, depending on your logic
        }

        val nextDueDateForCompletedTask = if (nextDueDate != null) {
            SimpleDateFormat("MM/dd/yyyy", Locale.ENGLISH).format(Date(nextDueDate))
        } else {
            null
        }

        val data = DataClass(
            id = id,
            message = messageText ?: "",
            time = timeFormat ?: "",
            date = userSelectedDate ?: "",
            notificationTime = notificationTime ?: 0L,
            repeatedTaskTime = repeatableOption.value,
            nextDueDate = nextDueDate,
            nextDueDateForCompletedTask = nextDueDateForCompletedTask,
            startDate = userSelectedDate?:""
        )

        databaseRef.child(id).setValue(data)

        softwareKeyboardController?.hide()
       // navController.previousBackStackEntry?.savedStateHandle?.set("newTaskId", newTaskId)
        navController.popBackStack()
       // addTaskvisible.value = false

    }

    val blurEffectBackground by animateDpAsState(targetValue = when{
        isPickerOpen.value -> 10.dp
        else -> 0.dp
    }
    )


    val offsetY by animateDpAsState(
        targetValue = if (addTaskvisible.value) 0.dp else 42.dp,
        animationSpec = tween(durationMillis = 300, delayMillis = 100,easing = EaseOutCirc)
    )
    val opacity by animateFloatAsState(
        targetValue = if (addTaskvisible.value) 1f else 0f,
        animationSpec = keyframes {
            durationMillis = 300
            0.3f at 100
            0.6f at 200
            1f at 300
        }
    )
    val scaffoldState = rememberScaffoldState()
    var alreadyNavigated by remember { mutableStateOf(false) }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .blur(blurEffectBackground)
                .background(color = MaterialTheme.colors.background)

                .clickable(indication = null,
                    interactionSource = remember { MutableInteractionSource() }) {
                    if (!alreadyNavigated) {
                        alreadyNavigated = true
                        softwareKeyboardController?.hide()
                        navController.popBackStack()
                    }
                   // addTaskvisible.value = false
                }
        ) {
            //ThemedGridImage(modifier = Modifier)
            // CanvasShadow(modifier = Modifier.fillMaxSize())
            Box(
                modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                   // verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    AddDaskCircleDesign( mutableSelectedDate.value,
                        mutableSelectedTime.value,

                        task = task,
                        onTaskChange = {newTask ->
                            if (newTask.length <= maxValue){
                                task.value = newTask
                            }
                        },onDoneClick = onDoneClick,
                        isPickerOpen = isPickerOpen,
                        isChecked = isChecked,
                        repeatableOption = repeatableOption,
                        id = id,

                        softwareKeyboardController = softwareKeyboardController!!,

                        )




                    Box(
                        modifier = Modifier
                            .padding(top = 24.dp)
                            .offset(y = offsetY)
                            .alpha(opacity)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .bounceClick()
                                    .background(
                                        shape = RoundedCornerShape(53.dp),
                                        color = MaterialTheme.colors.secondary
                                    )
                                    .clickable(indication = null,
                                        interactionSource = remember { MutableInteractionSource() }) {
                                        if (!alreadyNavigated) {
                                            alreadyNavigated = true
                                            softwareKeyboardController?.hide()
                                            navController.popBackStack()
                                        }
                                       // addTaskvisible.value = false

                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                ButtonTextWhiteTheme(text = "Cancel",
                                    color = MaterialTheme.colors.primary,
                                    modifier = Modifier
                                        .padding(top = 16.dp, start = 24.dp,end = 24.dp, bottom = 16.dp))
                            }

                            Spacer(modifier = Modifier.padding(32.dp))

                                Box(
                                    modifier = Modifier
                                        .bounceClick()
                                        .background(
                                            shape = RoundedCornerShape(53.dp),
                                            color = MaterialTheme.colors.primary
                                        )
                                        .clickable(indication = null,
                                            interactionSource = remember { MutableInteractionSource() }) {
                                            if (!alreadyNavigated) {
                                                alreadyNavigated = true
                                                onDoneClick.invoke()
                                            }


                                        },
                                    contentAlignment = Alignment.Center
                                ){
                                    ButtonTextDarkTheme(text = "Create",
                                        modifier = Modifier.padding(top = 16.dp, start = 24.dp,end = 24.dp, bottom = 16.dp))

                                }




                        }

                    }
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 56.dp)
                  //  .alpha(alpha)
                ,
                contentAlignment = Alignment.TopEnd
            ) {
                CrossFloatingActionButton(
                    onClick = {
                        if (!alreadyNavigated) {
                            alreadyNavigated = true
                            softwareKeyboardController!!.hide()
                            addTaskvisible.value = false

                            navController.popBackStack()
                        }

                    },
                    visible = addTaskvisible
                )
            }

        }





}
private fun areNotificationsEnabled(context:Context): Boolean {
    val notificationManager = NotificationManagerCompat.from(context)
    return notificationManager.areNotificationsEnabled()
}


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

    softwareKeyboardController: SoftwareKeyboardController,


){
    var isClickable = remember { mutableStateOf(true) }
    val focusRequester = remember { FocusRequester() }
    var visible by remember {
        mutableStateOf(false)
    }
    val keyboardController = LocalSoftwareKeyboardController.current
    LaunchedEffect(Unit) {
        visible = true
            focusRequester.requestFocus()
            keyboardController?.show()


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
    Log.d("addtaskId","$id")

        Box(
            modifier = Modifier

                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, top = 88.dp)
                // .size(344.dp)



                .offset(y = offsetY)
                 .scale(scale)
                .aspectRatio(1f)
                .clip(CircleShape)
                .background(MaterialTheme.colors.secondary, shape = CircleShape)
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
                            .height(90.dp)
                            .wrapContentHeight()
                            .padding(start = 32.dp, end = 32.dp)
                            .focusRequester(focusRequester)
                           ,

                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Done,
                            capitalization = KeyboardCapitalization.Sentences),
                        maxLines = 3 ,
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
                                color = MaterialTheme.colors.primary.copy(alpha = 0.5f),
                                fontFamily = interDisplayFamily,
                                style = androidx.compose.ui.text.TextStyle(letterSpacing = 0.sp)
                            )
                        },

                        textStyle = LocalTextStyle.current.copy(
                            textAlign = TextAlign.Center,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = interDisplayFamily,
                            color = MaterialTheme.colors.primary,
                            letterSpacing = 0.5.sp

                        ),
                        singleLine = false
                    )
                }


                TextStyle(
                    text =
                "${task.value.length} / 32")
                Box(
                    modifier = Modifier
                        .wrapContentSize(Alignment.Center)
                        .padding(top = 20.dp, start = 8.dp, end = 8.dp)
                        .bounceClick()
                        .clickable(indication = null,
                            interactionSource = remember { MutableInteractionSource() }) {
                            isPickerOpen.value = true
                        }
                        .border(
                            width = 1.5.dp,
                            color = MaterialTheme.colors.primary.copy(alpha = 0.5f), // Change to your desired border color
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
                            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ThemedCalendarImage(modifier = Modifier)
                            Text(
                                text = formattedDate,
                                fontFamily = interDisplayFamily,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colors.primary,
                                style = androidx.compose.ui.text.TextStyle(letterSpacing = 0.5.sp)
                            )
                        }
                    }else{
                        val formatter = DateTimeFormatter.ofPattern("EEE, d MMM yyyy",Locale.ENGLISH)
                        val formattedDate = selectedDate.value?.format(formatter) ?: ""
                        val dateString:String = formattedDate
                        val timeFormat = selectedTime.value?.format(DateTimeFormatter.ofPattern("hh:mm a",Locale.ENGLISH))?.toUpperCase() ?: ""
                        val timeString:String = timeFormat.toUpperCase()
                        Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            ThemedCalendarImage(modifier =Modifier)
                            Text(
                                text = "${dateString}, $timeString",
                                fontFamily = interDisplayFamily,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colors.primary,
                                style = androidx.compose.ui.text.TextStyle(letterSpacing = 0.5.sp)
                            )

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
                    color = MaterialTheme.colors.primary,
                    modifier = Modifier,
                    isClickable = isClickable)

            }

        }


   /* LaunchedEffect(isSaveClicked.value) {
        if (!isSaveClicked.value) {
            // Allow the FloatingActionButton transition to complete before switching
            delay(300) // Match your animation duration
            isSaveClicked.value = true
        }
    }*/

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
    if (isBoxVisible && repeatableOption.value in listOf("Daily","Weekly","Monthly","Yearly") ){
        val context = LocalContext.current

        Box(
            modifier = Modifier
                .wrapContentSize(Alignment.Center)
                .padding(top = 20.dp, bottom = 20.dp, start = 8.dp, end = 8.dp)
                .then(if (isClickable.value) Modifier.bounceClick() else Modifier)
                .clickable(
                    enabled = isClickable.value,
                    onClick = {
                        isPickerOpen.value = true
                        isClicked.value = true
                    }
                )


                .border(
                    width = 1.5.dp,
                    color = color.copy(alpha = 0.5f), // Change to your desired border color
                    shape = CircleShape
                )
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ){
            Row(modifier = Modifier
              //  .padding(top = 4.dp, bottom = 4.dp)
                ,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ThemedRepeatedIconImage(modifier = modifier)
                Text(text = repeatableOption.value,
                    fontFamily = interDisplayFamily,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = color,
                    letterSpacing = 1.sp)
                Box(modifier = Modifier
                    .border(
                        width = 1.5.dp,
                        color = color.copy(alpha = 0.5f), // Change to your desired border color
                        shape = CircleShape
                    )
                    .clickable (
                        enabled = isClickable.value,
                        onClick = {
                            repeatableOption.value = "No Repeat"
                            if (addtaskCrossClick){
                                databaseRef.child(id).child("repeatedTaskTime").setValue("No Repeat")
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
                                            updates["repeatedTaskTime"] = "No Repeat"
                                            updates["nextDueDate"] = data.notificationTime!! // This is Long so no type mismatch
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
                                completedTasksRef.child(id).child("repeatedTaskTime").setValue("No Repeat")
                                isBoxVisible = false
                            }
                        }

                    )



                    ){
                    ThemedCrossImage(modifier = modifier)
                }




            }
        }


    }else if ( repeatableOption.value in listOf("Daily","Weekly","Monthly","Yearly")  ) {
        // Set isBoxVisible to true when repeatableOption is "DAILY"
        isBoxVisible = true
    }
}
@Composable
fun ThemedCalendarImage(modifier: Modifier) {
    val isDarkTheme = isSystemInDarkTheme()
    val imageRes = if (isDarkTheme) {
        R.drawable.calendar_light_theme
    } else {
        R.drawable.calendar_dark_theme
    }

    Image(
        painter = painterResource(id = imageRes),
        contentDescription = null,
        modifier = modifier
            .alpha(0.5f)


        )
}
@Composable
fun TextStyle(text:String) {
    Text(text = text,
    fontFamily = interDisplayFamily,
    fontWeight = FontWeight.Medium,
    fontSize = 11.sp,
    color = MaterialTheme.colors.primary.copy(alpha = 0.25f),
        letterSpacing = 1.sp
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

