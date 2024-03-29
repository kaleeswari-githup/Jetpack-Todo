package com.firstyogi.dothing

import android.annotation.SuppressLint
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.EaseOutCirc
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.firstyogi.ui.theme.FABRed
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.*

@SuppressLint("UnrememberedMutableState", "UnusedMaterialScaffoldPaddingParameter")
@OptIn(ExperimentalComposeUiApi::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun UnMarkCompletedTaskScreen(
    selectedDate: MutableState<String>,
    selectedTime: MutableState<String>,
    textValue:String,
    id:String,
    openKeyboard: Boolean,
    onDismiss : () -> Unit,
    onDeleteClick: (String) -> Unit,
    onUnMarkCompletedClick:(String) -> Unit,
    isChecked: MutableState<Boolean>,
    ) {
    var task = rememberSaveable {
        mutableStateOf(textValue)
    }
    val maxValue = 32
    var isPickerOpen = remember { mutableStateOf(false) }
    val blurEffectBackground by animateDpAsState(targetValue = when{
        isPickerOpen.value -> 10.dp
        else -> 0.dp
    })
    Dialog(onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnClickOutside = true,
            dismissOnBackPress = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(modifier = Modifier
               .blur(radius = blurEffectBackground)
            ) {
                ThemedBackground()
                Box(modifier = Modifier
                    .fillMaxSize()
                    .clickable(indication = null,
                        interactionSource = remember { MutableInteractionSource() }) { onDismiss.invoke() }
                    , contentAlignment = Alignment.Center){
                    Column(modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.CenterHorizontally) {
                        UnMarkCompletedCircleDesign(
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
                            isPickerOpen = isPickerOpen,
                            isChecked = isChecked,
                        )

                        Box(
                            modifier = Modifier
                                .padding(bottom = 40.dp)

                        ) {
                            UnMarkCompletedButtons( id = id,onDismiss,onDeleteClick ,onUnMarkCompletedClick)
                        }

                    }
                }

            CrossFloatingActionButton(onClick = {
                    onDismiss.invoke()
                })
            }
        }
    }
@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun UnMarkCompletedCircleDesign(
    message: MutableState<String>,
    initialSelectedate: MutableState<String>,
    initialSelectedtime: MutableState<String>,
    id:String,
    onTaskChange:(String) -> Unit,
    isPickerOpen: MutableState<Boolean>,
    openKeyboard:Boolean,
    isChecked: MutableState<Boolean>,

    ){
    val isMessageFieldFocused = remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
Log.d("initialSelecteddate","$initialSelectedate")
    val focusManager = LocalFocusManager.current
    val database = FirebaseDatabase.getInstance()
    val user = FirebaseAuth.getInstance().currentUser
    val uid = user?.uid
    val context = LocalContext.current
    val completedTasksRef = database.reference.child("Task").child("CompletedTasks").child(uid.toString())
    val onDoneClick: () -> Unit = {
        val updatedData = HashMap<String, Any>()
        updatedData["id"] = id
        updatedData["message"] = message.value.trim()
        completedTasksRef.child(id).updateChildren(updatedData)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    //Toast.makeText(context, "Updated successfully", Toast.LENGTH_SHORT).show()

                } else {
                    Toast.makeText(context, task.exception.toString(), Toast.LENGTH_SHORT).show()
                }
            }
    }
    var visible by remember {
        mutableStateOf(false)
    }
    LaunchedEffect(Unit) {
        visible = true
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
            .size(344.dp)
            .offset(y = offsetY)
            .scale(scale)
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
            val messageState = remember { mutableStateOf(message.value) }
            CompositionLocalProvider(LocalTextSelectionColors provides customTextSelectionColors){
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

                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done,
                        capitalization = KeyboardCapitalization.Sentences),
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
                        textDecoration = TextDecoration.LineThrough,
                    ),

                    )
            }

            if (isMessageFieldFocused.value){
                TextStyle(text = "${message.value.length} / 32")
            }
            Box(
                modifier = Modifier
                    .wrapContentSize(Alignment.Center)
                    .padding(
                        top = 20.dp
                    )
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
                val formatter = DateTimeFormatter.ofPattern("EEE, d MMM")
                val formattedDate = initialSelectedate.value.format(formatter) ?: ""
                val dateString:String = formattedDate
                val timeFormat = initialSelectedtime.value.format(DateTimeFormatter.ofPattern("hh:mm a"))?.toUpperCase() ?: ""
                val timeString:String = timeFormat
                Log.d("timestring","$timeString")
                if (initialSelectedate.value.isNullOrEmpty() && initialSelectedtime.value.isNullOrEmpty()){
                    ThemedCalendarImage()
                }else if(initialSelectedate.value.isNotEmpty() && initialSelectedtime.value.isNullOrEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        ThemedCalendarImage()
                        Text(
                            text = dateString,
                            fontFamily = interDisplayFamily,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colors.secondary,
                            style = androidx.compose.ui.text.TextStyle(letterSpacing = -0.sp)
                        )
                    }
                }else{

                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                       ThemedCalendarImage()
                        Text(
                            text = "$dateString, $timeString",
                            fontFamily = interDisplayFamily,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color =  MaterialTheme.colors.secondary,
                            style = androidx.compose.ui.text.TextStyle(letterSpacing = 0.sp)
                        )

                    }
                }
                if (isPickerOpen.value) {
                    val pattern = "EEE, d MMM yyyy"
                    val locale = Locale.ENGLISH

                    val formatter = DateTimeFormatter.ofPattern(pattern, locale)
                        .withZone(ZoneId.of("America/New_York"))
                    val selectedDate = initialSelectedate.value
                    val localDate = if (selectedDate.isEmpty()) {
                        LocalDate.now()
                    } else {
                        try {
                            LocalDate.parse(selectedDate, formatter)
                        } catch (e: DateTimeParseException) {
                            LocalDate.now()
                        }
                    }
                    Log.d("initialSelectedDate","$localDate")
                    UpdatedCalendarAndTimePickerScreen(
                        userSelectedDate = localDate,
                        userSelectedTime = initialSelectedtime.value,
                        onDismiss = { isPickerOpen.value = false
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
                        invokeOnDoneClick = false,
                        UnMarkedDateandTime = true,
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
@Composable
fun UnMarkCompletedButtons(id: String,
                           onDismiss : () -> Unit,
                           onDeleteClick : (String) -> Unit,
                           onUnMarkCompletedClick:(String) -> Unit)
{
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
            durationMillis = 300
            0.0f at 0
            1f at 300
            delayMillis = 200
        }

    )
    Box(modifier = Modifier
        .wrapContentWidth()
        .height(48.dp)
        .offset(y = offsetY)
        .alpha(opacity)
        .background(color = MaterialTheme.colors.primary, shape = RoundedCornerShape(30.dp)),
    ) {
        Row(modifier = Modifier
            .wrapContentWidth()
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
                ButtonTextWhiteTheme(text = "DELETE",color = MaterialTheme.colors.secondary)
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
                    onUnMarkCompletedClick(id)
                    onDismiss.invoke()
                },
                verticalAlignment = Alignment.CenterVertically) {
                ThemedSquareImage(modifier = Modifier)
                ButtonTextWhiteTheme(text = "MARK UNCOMPLETED",color = MaterialTheme.colors.secondary)
            }
        }

    }
}


