package com.example.Pages

import android.annotation.SuppressLint
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.dothings.*
import com.example.dothings.R
import com.example.ui.theme.FABRed
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.*

@SuppressLint("UnrememberedMutableState")
@OptIn(ExperimentalComposeUiApi::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun UpdateTaskScreen(
    selectedDate: MutableState<String>,
    selectedTime:MutableState<String>,
    textValue:String,
    id:String,
    openKeyboard: Boolean,
    onDismiss : () -> Unit,
    onMarkCompletedClick: (String) -> Unit,
    onDeleteClick: (String) -> Unit,
    isPickerOpen:MutableState<Boolean>,
    isAddDaskOpen:MutableState<Boolean>,
    index:Int,
    isChecked: MutableState<Boolean>,
    isUpdatePickerOpen:MutableState<Boolean>
) {
    var task = rememberSaveable {
        mutableStateOf(textValue)
    }

    val maxValue = 32
    val database = FirebaseDatabase.getInstance()
    val user = FirebaseAuth.getInstance().currentUser
    val uid = user?.uid
    val databaseRef: DatabaseReference = database.reference.child("Task").child(uid.toString())

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

        val formattedTime = if (updatedTime.isNotEmpty()) {
            LocalTime.parse(updatedTime, timeFormat)
        } else {
            // Handle the case where updatedTime is empty
            null // Or provide a default value as needed
        }

        val notificationTime: Long? = if (!formattedDate.isNullOrBlank() && formattedTime != null) {
            val dateTime = LocalDateTime.of(originalDate, formattedTime)
            dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        } else {
            null
        }
        val updatedData = HashMap<String, Any>()
        updatedData["id"] = id
        updatedData["message"] = task.value ?: ""
        updatedData["time"] = formattedTime?.format(timeFormat) ?: ""
        updatedData["date"] = formattedDate
        updatedData["notificationTime"] = notificationTime ?: 0L
        databaseRef.child(id).updateChildren(updatedData)
        onDismiss.invoke()
    }


    val onBackPressed: () -> Unit = {
        onDoneClick(selectedDate.value,selectedTime.value)
    }

    val blurEffectBackground by animateDpAsState(targetValue = when{
        isUpdatePickerOpen.value -> 25.dp
        else -> 0.dp
    })

    Dialog(onDismissRequest = onBackPressed ,
    properties = DialogProperties(
        dismissOnClickOutside = true,
        dismissOnBackPress = true,
        usePlatformDefaultWidth = false
    )
) {

    Box(modifier = Modifier
        .blur(radius = blurEffectBackground)
        .fillMaxSize()
        .clickable(indication = null,
            interactionSource = remember { MutableInteractionSource() }) { onDismiss.invoke() }
    ) {
        ThemedBackground()
       // Image(painter = painterResource(id = R.drawable.grid_lines), contentDescription = null)
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center){
            Column(modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally) {
                UpdateCircleDesign(
                    initialSelectedate = selectedDate ,
                    initialSelectedtime = selectedTime  ,
                    message = task,
                    onTaskChange = { newTask ->
                        if (newTask.length <= maxValue){
                            task.value = newTask
                        }
                    },
                    id = id,
                    openKeyboard = openKeyboard,
                    isUpdatePickerOpen = isUpdatePickerOpen,
                    isAddDaskOpen = isAddDaskOpen,
                    index = index,
                    isChecked = isChecked,

                    )

                Box(
                    modifier = Modifier
                        .padding(bottom = 40.dp)
                ) {
                    UpdatedButtons( id = id,onDismiss, onMarkCompletedClick = onMarkCompletedClick,onDeleteClick)
                }
            }
        }
        CrossFloatingActionButton(onClick = {
            onDoneClick.invoke(selectedDate.value,selectedTime.value)
        })
    }
}

}
@SuppressLint("SuspiciousIndentation")
@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalComposeUiApi::class, ExperimentalAnimationApi::class)
@Composable
fun UpdateCircleDesign(
    message: MutableState<String>,
    initialSelectedate: MutableState<String>,
    initialSelectedtime: MutableState<String> ,
    id:String,
    onTaskChange:(String) -> Unit,
    isUpdatePickerOpen:MutableState<Boolean>,
    openKeyboard:Boolean,
    isChecked: MutableState<Boolean>,
    index:Int,
    isAddDaskOpen:MutableState<Boolean>

){
   /* val selectedDate = remember { mutableStateOf(initialSelectedate.value) }
    val selectedTime = remember { mutableStateOf(initialSelectedtime.value) }*/
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
        updatedData["message"] = message.value
        databaseRef.child(id).updateChildren(updatedData)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(context, "Updated successfully", Toast.LENGTH_SHORT).show()

                } else {
                    Toast.makeText(context, task.exception.toString(), Toast.LENGTH_SHORT).show()
                }
            }
    }
    var visible by remember {
        mutableStateOf(false)
    }
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
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, top = 38.dp)
                .size(377.dp)
                .offset(y = offsetY)
                .scale(scale)
                // .alpha(opacity)
                .aspectRatio(1f)
                .clip(CircleShape)
                .background(color = MaterialTheme.colors.primary, shape = CircleShape)
                .clickable(indication = null,
                    interactionSource = remember { MutableInteractionSource() }) { },
            contentAlignment = Alignment.Center
        ){

            Column(modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center) {
                val messageState = remember { mutableStateOf(message.value) }
                TextField(
                    value = message.value,
                    onValueChange = onTaskChange ,
                    modifier = Modifier
                        .fillMaxWidth()
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
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone ={
                            keyboardController?.hideSoftwareKeyboard()
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
                                .padding(horizontal = 70.dp),
                            fontWeight = FontWeight.Medium,
                            fontSize = 24.sp,
                            color = MaterialTheme.colors.secondary.copy(alpha = 0.5f)
                        )
                    },

                    textStyle = LocalTextStyle.current.copy(
                        textAlign = TextAlign.Center,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = interDisplayFamily,
                        color = MaterialTheme.colors.secondary,
                        letterSpacing = 0.sp
                    ),

                    )
                if (isMessageFieldFocused.value){
                    TextStyle(text = "${message.value.length} / 32")
                }

                Box(
                    modifier = Modifier
                        .wrapContentSize(Alignment.Center)
                        .padding(
                            top = 20.dp,
                            start = 48.dp,
                            end = 48.dp
                        )
                        .bounceClick()
                        //   .background(color = SmallBox, shape = CircleShape)

                        .clickable(indication = null,
                            interactionSource = remember { MutableInteractionSource() }) {
                            isUpdatePickerOpen.value = true
                        }

                        .border(
                            width = 0.8.dp,
                            color = MaterialTheme.colors.secondary, // Change to your desired border color
                            shape = CircleShape
                        )


                        .padding(8.dp)


                ) {
                    if (initialSelectedate.value.isNullOrEmpty() && initialSelectedtime.value.isNullOrEmpty()){
                        ThemedCalendarImage()
                    }else if(initialSelectedate != null && initialSelectedtime == null) {
                        val formatter = DateTimeFormatter.ofPattern("EEE, d MMM")
                        val formattedDate = initialSelectedate.value.format(formatter) ?: ""
                        Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            ThemedCalendarImage()
                            Text(
                                text = formattedDate,
                                fontFamily = interDisplayFamily,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colors.secondary
                            )
                        }
                    }else{
                        val formatter = DateTimeFormatter.ofPattern("EEE, d MMM")
                        val formattedDate = initialSelectedate.value.format(formatter) ?: ""
                        val dateString:String = formattedDate
                        val timeFormat = initialSelectedtime.value.format(DateTimeFormatter.ofPattern("hh:mm a"))?.toUpperCase() ?: ""
                        val timeString:String = timeFormat
                        Log.d("timestring","$timeString")
                        Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                           ThemedCalendarImage()
                            Text(
                                text = dateString,
                                fontFamily = interDisplayFamily,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colors.secondary,
                                style = androidx.compose.ui.text.TextStyle(letterSpacing = 0.sp)
                            )
                            Text(
                                text = timeString,
                                fontFamily = interDisplayFamily,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colors.secondary,
                                style = androidx.compose.ui.text.TextStyle(letterSpacing = 0.sp))
                        }
                    }
                    if (isUpdatePickerOpen.value) {
                        val pattern = "EEE, d MMM yyyy"
                        val locale = Locale.ENGLISH

                        val formatter = DateTimeFormatter.ofPattern(pattern, locale)
                            .withZone(ZoneId.of("America/New_York"))

                        val localDate = if (initialSelectedate.value.isEmpty()) {
                            LocalDate.now()
                        } else {
                            try {
                                LocalDate.parse(initialSelectedate.value, formatter)
                            } catch (e: DateTimeParseException) {
                                LocalDate.now()
                            }
                        }

                        UpdatedCalendarAndTimePickerScreen(
                            userSelectedDate = localDate,
                            userSelectedTime = initialSelectedtime.value,
                            onDismiss = { isUpdatePickerOpen.value = false
                                keyboardController?.show()},
                            onDateTimeSelected = { date, time ->
                                val defaultDateFormat = DateTimeFormatter.ofPattern("MM/dd/yyyy")
                                val desiredDateFormat = DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale.ENGLISH)

                                val defaultDateString = date
                                val parsedDate = LocalDate.parse(defaultDateString, defaultDateFormat)
                                val formattedDate = parsedDate.format(desiredDateFormat)

                                initialSelectedate.value = formattedDate
                                initialSelectedtime.value = time
                            },
                            id = id,
                            invokeOnDoneClick = true,
                            UnMarkedDateandTime = false,
                            isChecked = isChecked,
                            message = message
                        )

                    }
                
            }
        }
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
fun UpdatedButtons(id: String,
                   onDismiss : () -> Unit,
                   onMarkCompletedClick: (String) -> Unit,
                   onDeleteClick: (String) -> Unit){
    val coroutineScope = rememberCoroutineScope()
    val database = FirebaseDatabase.getInstance()
    val user = FirebaseAuth.getInstance().currentUser
    val uid = user?.uid

    var visible by remember {
        mutableStateOf(false)
    }
    LaunchedEffect(Unit) {
        visible = true // Set the visibility to true to trigger the animation
    }
    val offsetY by animateDpAsState(
        targetValue = if (visible) 0.dp else 24.dp,
        animationSpec = tween(durationMillis = 300,easing = EaseOutCirc, delayMillis = 200)
    )
    val opacity by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = keyframes {
            durationMillis = 300 // Total duration of the animation
            0.0f at 0 // Opacity becomes 0.3f after 200ms
            // Opacity becomes 0.6f after 500ms
            1f at 300

            delayMillis = 200// Opacity becomes 1f after 1000ms (end of the animation)
        }

    )
    Box(modifier = Modifier
        .fillMaxWidth()
        .padding(start = 42.dp, end = 42.dp)
        .height(48.dp)
        .offset(y = offsetY)
        .alpha(opacity)
        .background(color = MaterialTheme.colors.primary, shape = RoundedCornerShape(30.dp))
        ,
contentAlignment = Alignment.Center
    ) {
        Row(modifier = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.clickable(indication = null,
                interactionSource = remember { MutableInteractionSource() }) {
                onDeleteClick(id)
                onDismiss.invoke()
            },
                verticalAlignment = Alignment.CenterVertically
            ) {
               ThemedTrashImage()
               Interfont(text = "Delete")
            }
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .fillMaxHeight()
                    .background(color = MaterialTheme.colors.background)

            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)
            , modifier = Modifier.clickable(indication = null,
                    interactionSource = remember { MutableInteractionSource() }) {
                    coroutineScope.launch {
                        onMarkCompletedClick(id)
                        onDismiss.invoke()
                    }
            },
            verticalAlignment = Alignment.CenterVertically) {
                ThemedSquareImage(modifier = Modifier)
                Interfont(text = "Mark completed")
            }
        }

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
fun Interfont(text:String){
    Text(
        text = text,
        fontFamily = interDisplayFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        color = MaterialTheme.colors.secondary,
        style = androidx.compose.ui.text.TextStyle(letterSpacing = 0.sp)
    )
}

@Composable
fun CrossFloatingActionButton(onClick:() -> Unit){
    Box(modifier = Modifier
        .size(72.dp)
        .padding(top = 12.dp, start = 24.dp)
        .fillMaxWidth()
        .fillMaxHeight()
        .background(Color.Transparent)
    ) {
        androidx.compose.material.FloatingActionButton(
            modifier = Modifier
                .size(48.dp)
                .align(Alignment.BottomCenter)
                .bounceClick()
                ,
            elevation = FloatingActionButtonDefaults.elevation(0.dp),
            onClick = {onClick.invoke()},
            shape = CircleShape,
            backgroundColor = MaterialTheme.colors.primary

            ) {
            ThemedCrossImage()
        }
    }
}
@Composable
fun ThemedCrossImage() {
    val isDarkTheme = isSystemInDarkTheme()
    val imageRes = if (isDarkTheme) {
        R.drawable.dark_cross_icon
    } else {
        R.drawable.light_cross_icon
    }

    Image(
        painter = painterResource(id = imageRes),
        contentDescription = null,
    )
}