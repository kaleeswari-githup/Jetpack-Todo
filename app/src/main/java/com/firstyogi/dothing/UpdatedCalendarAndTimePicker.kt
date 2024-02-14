package com.firstyogi.dothing

import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

@OptIn(ExperimentalComposeUiApi::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun UpdatedCalendarAndTimePickerScreen(
    onDismiss: () -> Unit,
    userSelectedDate:LocalDate?,
    userSelectedTime: String?= "",
    onDateTimeSelected: (String, String) -> Unit,
    id:String,
    invokeOnDoneClick: Boolean = true,
    UnMarkedDateandTime:Boolean = true,
    isChecked: MutableState<Boolean>,
    message: MutableState<String>
) {

    var selectedDate = remember { mutableStateOf(LocalDate.now()) }
    var isTimePickerVisible by remember { mutableStateOf(false) }
    var selectedTime = remember {
        mutableStateOf(if (isTimePickerVisible) LocalTime.now() else null)
    }
    val database = FirebaseDatabase.getInstance()
    val user = FirebaseAuth.getInstance().currentUser
    val uid = user?.uid
    val databaseRef: DatabaseReference = database.reference.child("Task").child(uid.toString())
    val completedTasksRef = database.reference.child("Task").child("CompletedTasks").child(uid.toString())
    val context = LocalContext.current
    fun calculateNotificationTime(selectedDate: LocalDate, selectedTime: LocalTime): Long {
        val dateTime = LocalDateTime.of(selectedDate, selectedTime)
        return dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }
    val onDoneClick: (String, String) -> Unit = { updatedDate, updatedTime ->
        val dateFormatter = DateTimeFormatter.ofPattern("MM/dd/yyyy")

        val dateStringFromDatabase = updatedDate

        val originalDate: LocalDate? = if (dateStringFromDatabase.isNotEmpty()) {
            LocalDate.parse(dateStringFromDatabase, dateFormatter)
        } else {
            LocalDate.MIN
        }
        val formattedDate = originalDate?.format(dateFormatter) ?: ""
        val timeFormatter = DateTimeFormatter.ofPattern("hh:mm a",Locale.ENGLISH)
        val timeFormat = if (updatedTime.isNotEmpty()) {
            val updatedTimeString = LocalTime.parse(updatedTime, timeFormatter)
            updatedTimeString.format(DateTimeFormatter.ofPattern("hh:mm a",Locale.ENGLISH))?.toUpperCase() ?: ""
        } else {
            ""
        }
        val notificationTime = if (originalDate != null && updatedTime.isNotEmpty()) {
            val updatedTimeString = LocalTime.parse(updatedTime, timeFormatter)
            calculateNotificationTime(originalDate, updatedTimeString)
        } else {
            0L
        }
        val updatedData = HashMap<String, Any>()
        updatedData["message"] = message.value
        updatedData["id"] = id
        updatedData["time"] = timeFormat
        updatedData["date"] = formattedDate
        updatedData["notificationTime"] = notificationTime
        if (invokeOnDoneClick){
            databaseRef.child(id).updateChildren(updatedData)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        onDismiss.invoke()
                    } else {
                        Toast.makeText(context, task.exception.toString(), Toast.LENGTH_SHORT).show()
                    }
                }
        }
        if (UnMarkedDateandTime){
            completedTasksRef.child(id).updateChildren(updatedData)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        onDismiss.invoke()
                    } else {
                        Toast.makeText(context, task.exception.toString(), Toast.LENGTH_SHORT).show()
                    }
                }
        }

    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        ThemedBackground()
        Column(modifier = Modifier
            .fillMaxSize()
            .clickable(indication = null,
                interactionSource = remember { MutableInteractionSource() }) { onDismiss.invoke() }
            .background(color = Color.Transparent),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            var visible by remember {
                mutableStateOf(false)
            }

            LaunchedEffect(Unit) {
                visible = true // Set the visibility to true to trigger the animation

            }
            val scale by animateFloatAsState(
                targetValue = if (visible) 1f else 0f,
                animationSpec = tween(
                    easing = EaseOutCirc
                )

            )
            val offsetY by animateDpAsState(
                targetValue = if (visible) 0.dp else -48.dp,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessVeryLow
                )

            )

            Box(
                modifier = Modifier
                    .padding(start = 8.dp, end = 8.dp)
                    .offset(y = offsetY)
                    .scale(scale),

                contentAlignment = Alignment.Center
            ) {

                UpdatedCalendar(
                    selectedDate = selectedDate,
                    userSelectedtime = userSelectedTime?.toString() ?: "",
                    startDate = userSelectedDate ?: LocalDate.now(),
                    selectedTime = selectedTime,
                    isChecked = isChecked
                )

            }
            Row(modifier = Modifier
                .padding(top = 24.dp)
                .fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically) {
                var visible by remember {
                    mutableStateOf(false)
                }
                LaunchedEffect(Unit) {
                    visible = true // Set the visibility to true to trigger the animation

                }
                val offsetY by animateDpAsState(
                    targetValue = if (visible) 0.dp else 42.dp,
                    animationSpec = tween(durationMillis = 300, delayMillis = 100,easing = EaseOutCirc)
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
                Button(
                    onClick = {
                        onDismiss.invoke()
                    },
                    shape = RoundedCornerShape(53.dp),
                    modifier = Modifier
                        .size(width = 105.dp, height = 48.dp)
                        .offset(y = offsetY)
                        .alpha(opacity)
                        .bounceClick(),

                    colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.primary),
                   elevation = ButtonDefaults.elevation(0.dp)

                ) {
                    ButtonTextWhiteTheme(text = "CANCEL",color = MaterialTheme.colors.secondary)
                }
                Spacer(modifier = Modifier.padding(40.dp))
                Button(
                    onClick = {
                        Log.d("id","$id")
                        val dateFormatter = DateTimeFormatter.ofPattern("MM/dd/yyyy")
                        val dateString: String = selectedDate.value.format(dateFormatter)

                        val timeFormatter = DateTimeFormatter.ofPattern("hh:mm a",Locale.ENGLISH)
                        val timeString: String = selectedTime.value?.format(timeFormatter) ?: ""

                        Log.d("UpdatedDateString","$dateString")
                        onDateTimeSelected(dateString, timeString)
                        onDoneClick(dateString,timeString)
                        onDismiss.invoke()
                    },
                    shape = RoundedCornerShape(53.dp),
                    modifier = Modifier
                        .size(width = 105.dp, height = 48.dp)
                        .offset(y = offsetY)
                        .alpha(opacity)
                        .bounceClick(),
                    colors = ButtonDefaults.buttonColors(backgroundColor =MaterialTheme.colors.secondary),
                    elevation = ButtonDefaults.elevation(0.dp)

                    ) {
                   ThemedTickImage()
                    Spacer(modifier = Modifier.padding(start = 8.dp))
                    ButtonTextDarkTheme(text = "DONE")

                }
            }

        }
    }



}