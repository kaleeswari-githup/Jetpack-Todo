package com.example.Pages

import android.annotation.SuppressLint
import android.os.Build
import android.util.Log
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.example.dothings.R
import com.example.dothings.R.DataClass
import com.example.dothings.interDisplayFamily
import com.example.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.*

@OptIn(ExperimentalAnimationApi::class)
@SuppressLint("RememberReturnType", "SuspiciousIndentation")
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun AddDaskScreen(
    selectedDate: MutableState<LocalDate?>,
    selectedTime: MutableState<LocalTime?>,
    textValue:String,
    onDismiss: () -> Unit,
    isPickerOpen: MutableState<Boolean>,
    modifier: Modifier

) {
    var task = rememberSaveable {
        mutableStateOf(textValue)
    }

    val mutableSelectedTime = remember { mutableStateOf(selectedTime) }
    val mutableSelectedDate = remember { mutableStateOf(selectedDate) }
    val database = FirebaseDatabase.getInstance()
    val user = FirebaseAuth.getInstance().currentUser
    val uid = user?.uid
    val databaseRef:DatabaseReference = database.reference.child("Task").child(uid.toString())

    val maxValue = 32
    val onDoneClick:() -> Unit = {
        val timeFormat = if (selectedTime != null && selectedTime.value != null) {
            selectedTime.value!!.format(DateTimeFormatter.ofPattern("hh:mm a")).toUpperCase()
        } else {
            null
        }
        val formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy")
        val formattedDate = if (selectedDate != null && selectedDate.value != null) {
            selectedDate.value!!.format(formatter)
        } else {
            null
        }

        val messageText = if (task.value.isNullOrBlank()) null else task.value
        val userSelectedDate = if (formattedDate.isNullOrBlank()) null else formattedDate
        val userSelectedTime = if (timeFormat.isNullOrBlank()) null else timeFormat
        val id:String = databaseRef.push().key.toString()
        Log.d("StoreTag", "id: $id")
        val notificationTime: Long? = if (userSelectedDate != null && userSelectedTime != null) {
            val combinedDateTime = "$userSelectedDate $userSelectedTime"
            val dateTimeFormat = SimpleDateFormat("MM/dd/yyyy hh:mm a", Locale.getDefault())
            val date: Date = dateTimeFormat.parse(combinedDateTime)
            date?.time
        } else {
            null
        }
        val data = DataClass(id,messageText ?: "",userSelectedTime ?: "",userSelectedDate ?: "", notificationTime =notificationTime ?: 0L )
        databaseRef.child(id).setValue(data)
        onDismiss.invoke()
    }

    val blurEffectBackground by animateDpAsState(targetValue = when{
        isPickerOpen.value -> 25.dp
        else -> 0.dp
    }
    )

        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(
                dismissOnClickOutside = true,
                dismissOnBackPress = true,
                usePlatformDefaultWidth = false)

        ){

            var visible by remember {
                mutableStateOf(false)
            }
           (LocalView.current.parent as DialogWindowProvider)?.window?.setDimAmount(0.1f)
            val offsetY by animateDpAsState(
                targetValue = if (visible) 0.dp else 400.dp,
                animationSpec = tween(durationMillis = 300, delayMillis = 100,easing = EaseOutCirc)
            )

           /* Box(modifier = Modifier.fillMaxSize()
                .blur(60.dp)
                .background(color = SurfaceGray.copy(alpha = 0.9f))
                )*/
            // Image(painter = painterResource(id = R.drawable.grid_lines), contentDescription = null)
                Box(modifier = Modifier

                     .blur(radius = blurEffectBackground)
                    .fillMaxSize()
                    // .offset(y = offsetY)
                    .clickable(indication = null,
                        interactionSource = remember { MutableInteractionSource() }) { onDismiss.invoke() }
                ) {

                    Column(modifier = Modifier,
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally) {

                        AddDaskCircleDesign( mutableSelectedDate.value,mutableSelectedTime.value,task = task, onTaskChange = {newTask ->
                            if (newTask.length <= maxValue){
                                task.value = newTask
                            }
                        },onDoneClick = onDoneClick,isPickerOpen)
                        TwoButtons(
                            onDoneClick = onDoneClick,
                            onDismiss = onDismiss)
                    }
                    CrossFloatingActionButton {
                        onDismiss.invoke()
                    }
                }




    }


}



@SuppressLint("SuspiciousIndentation")
@OptIn(ExperimentalComposeUiApi::class, ExperimentalAnimationApi::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun AddDaskCircleDesign(
    selectedDate: MutableState<LocalDate?>,
    selectedTime: MutableState<LocalTime?>,
    task:MutableState<String>,
    onTaskChange: (String) -> Unit,
    onDoneClick: () -> Unit,
    isPickerOpen: MutableState<Boolean>){
    val focusRequester = remember { FocusRequester() }
    val softwareKeyboardController = LocalSoftwareKeyboardController.current
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
    val opacity by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = keyframes {
            durationMillis = 300 // Total duration of the animation
            0.3f at 100 // Opacity becomes 0.3f after 200ms
            0.6f at 200 // Opacity becomes 0.6f after 500ms
            1f at 300


        }
    )
        Box(
            modifier = Modifier

                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, top = 38.dp)
                .size(344.dp)
                .offset(y = offsetY)
                .scale(scale)
              //  .alpha(opacity)

                .aspectRatio(1f)

                .clip(CircleShape)

                .background( color = Color.White, shape = CircleShape)
                .clickable(indication = null,
                    interactionSource = remember { MutableInteractionSource() }) { },

            contentAlignment = Alignment.Center
        ) {
            Column(modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center) {
                TextField(
                    value = task.value,
                    onValueChange = onTaskChange ,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 32.dp, end = 32.dp, top = 60.dp)
                        .focusRequester(focusRequester)
                        .onFocusChanged { focusState ->
                            if (focusState.isFocused) {
                                focusRequester.requestFocus()
                                //softwareKeyboardController?.show()
                            }
                        },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone ={
                            onDoneClick()
                        }
                    ),
                    colors = TextFieldDefaults.textFieldColors(
                        backgroundColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = FABDarkColor
                    ),
                    placeholder = {
                        Text(text = "New Task",
                            modifier = Modifier
                                .fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Medium,
                            fontSize = 24.sp,
                            color = NewtaskColorGray
                        )
                    },

                    textStyle = LocalTextStyle.current.copy(
                        textAlign = TextAlign.Center,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = interDisplayFamily,
                        color = Text1
                    ),
                    maxLines = 2
                )

                TextStyle(text = "${task.value.length} / 32")
                Box(
                    modifier = Modifier
                        .wrapContentSize(Alignment.Center)
                        .padding(top = 48.dp)
                        .bounceClick()
                        //.background(color = SmallBox, shape = CircleShape)
                        .clickable(indication = null,
                            interactionSource = remember { MutableInteractionSource() }) {
                            isPickerOpen.value = true
                        }
                        .border(
                            width = 0.8.dp,
                            color = Color.Black.copy(alpha = 0.8f), // Change to your desired border color
                            shape = CircleShape
                        )
                       // .padding(top = 4.dp,start = 8.dp,end = 8.dp, bottom =  4.dp)
                        .padding(8.dp)


                ) {
                    if (selectedDate.value == null && selectedTime.value == null){
                        Icon(
                            painter = painterResource(id = R.drawable.calendar_icon),
                            contentDescription = "calender_icon",
                            modifier = Modifier
                        )


                    }else if(selectedDate != null && selectedTime == null) {
                        val formatter = DateTimeFormatter.ofPattern("EEE, d MMM")
                        val formattedDate = selectedDate.value?.format(formatter) ?: ""
                        Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            Icon(
                                painter = painterResource(id = R.drawable.calendar_icon),
                                contentDescription = "calender_icon",
                                modifier = Modifier
                            )
                            Text(
                                text = formattedDate,
                                fontFamily = interDisplayFamily,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = Text1
                            )
                        }
                    }else{
                        val formatter = DateTimeFormatter.ofPattern("EEE, d MMM")
                        val formattedDate = selectedDate.value?.format(formatter) ?: ""
                        val dateString:String = formattedDate
                        val timeFormat = selectedTime.value?.format(DateTimeFormatter.ofPattern("hh:mm a"))?.toUpperCase() ?: ""
                        val timeString:String = timeFormat
                        Log.d("Time string","$timeString")
                        Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            Icon(
                                painter = painterResource(id = R.drawable.calendar_icon),
                                contentDescription = "calender_icon",
                            )
                            Text(
                                text = dateString,
                                fontFamily = interDisplayFamily,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = Text1
                            )
                            Text(
                                text = timeString,
                                fontFamily = interDisplayFamily,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = Text1)
                        }
                    }
                    if (isPickerOpen.value) {
                        UpdatedCalendarAndTimePickerScreen(
                            onDismiss = { isPickerOpen.value = false },
                            onDateTimeSelected = {date,time ->
                                val dateFormatter = DateTimeFormatter.ofPattern("MM/dd/yyyy")
                                val parsedDate = LocalDate.parse(date, dateFormatter)
                                val timeFormatter = DateTimeFormatter.ofPattern("hh:mm a")
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
                            userSelectedTime = if (selectedDate.value == null) null else selectedTime.value?.format(DateTimeFormatter.ofPattern("hh:mm a")),
                            invokeOnDoneClick = false,
                            UnMarkedDateandTime = false
                        )
                    }

                }



        }
    }

    LaunchedEffect(Unit) {
        delay(100)
        focusRequester.requestFocus()
        softwareKeyboardController?.show()
    }

}

@Composable
fun TextStyle(text:String){
    Text(text = text,
    fontFamily = interDisplayFamily,
    fontWeight = FontWeight.Medium,
    fontSize = 11.sp,
    color = VeryLightGray
   )
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun TwoButtons(
    onDoneClick: () -> Unit,
    onDismiss: () -> Unit,
) {

    var visible by remember {
        mutableStateOf(false)
    }
    LaunchedEffect(Unit) {
        visible = true // Set the visibility to true to trigger the animation
    }
    val offsetY by animateDpAsState(
        targetValue = if (visible) 0.dp else 48.dp,
        animationSpec = tween(durationMillis = 300,easing = EaseOutCirc, delayMillis = 300)
    )
    val opacity by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = keyframes {
            durationMillis = 300 // Total duration of the animation
            0.0f at 0 // Opacity becomes 0.3f after 200ms
             // Opacity becomes 0.6f after 500ms
            1f at 300

        delayMillis = 300// Opacity becomes 1f after 1000ms (end of the animation)
        }

    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 32.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(width = 105.dp, height = 48.dp)
                .offset(y = offsetY)
                .alpha(opacity)
                .bounceClick()

                .background(shape = RoundedCornerShape(53.dp), color = Color.White)
                .clickable(indication = null,
                    interactionSource = remember { MutableInteractionSource() }) {
                    onDismiss.invoke()
                }
                ,
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Cancel",
                fontFamily = interDisplayFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp,
                color = Text1
            )
        }

        Spacer(modifier = Modifier.padding(40.dp))
        Button(onClick = {
            onDoneClick.invoke()
        },
            shape = RoundedCornerShape(53.dp),
            modifier = Modifier
                .size(width = 105.dp, height = 48.dp)
                .bounceClick()
                .offset(y = offsetY)
                .alpha(opacity),
            colors = ButtonDefaults.buttonColors(backgroundColor = Color.Black),
            elevation = ButtonDefaults.elevation(0.dp)



        ) {

            Image(
                painter = painterResource(id = R.drawable.tick),
                contentDescription = "Save Tick",
                modifier = Modifier
                    .size(16.dp)
            )
            Text(
                text = "Save",
                fontFamily = interDisplayFamily,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                style = androidx.compose.ui.text.TextStyle(letterSpacing = 0.sp),
                color = Color.White,
                modifier = Modifier.padding(start = 12.dp)
            )
            }

        }


}

@Composable
fun RadialGradientBox(){
    Canvas(modifier = Modifier.fillMaxSize()) {
        val gradientColors = listOf(
            Color(0xFFFF972A),
            Color(0xFFFD7A11),
            Color(0xFFFF852F)
        )
        val gradientCenter = Offset(0.2031f * size.width, 0.0938f * size.height)
        val gradientRadius = 0.9062f * size.width.coerceAtMost(size.height)

        val gradientBrush = Brush.radialGradient(
            colors = gradientColors,
            center = gradientCenter,
            radius = gradientRadius
        )

        drawRoundRect(
            brush = gradientBrush,
            cornerRadius = CornerRadius(x = 73f,y = 73f),
            size = size
        )
    }
}


