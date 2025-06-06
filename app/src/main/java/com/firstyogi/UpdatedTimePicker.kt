package com.firstyogi.dothing

import android.annotation.SuppressLint
import android.media.MediaPlayer
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.commandiron.wheel_picker_compose.WheelTimePicker
import com.commandiron.wheel_picker_compose.core.TimeFormat
import com.commandiron.wheel_picker_compose.core.WheelPickerDefaults
//import com.commandiron.wheel_picker_compose.WheelTimePicker
//import com.commandiron.wheel_picker_compose.core.TimeFormat
//import com.commandiron.wheel_picker_compose.core.WheelPickerDefaults
import com.firstyogi.ui.theme.FABRed
import kotlinx.coroutines.delay
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.*
@SuppressLint("SuspiciousIndentation", "RememberReturnType")
@RequiresApi(Build.VERSION_CODES.O)
@Composable

fun UpdatedScrollableTimePicker(
    selectedTime: MutableState<LocalTime?>,
    onClearClick: () -> Unit,
    initialTime: LocalTime?,
    onTimeSelected: (LocalTime) -> Unit,
    isChecked: MutableState<Boolean>
) {
    var shouldUseCurrentTime by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .padding(bottom = 24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp)
                //.background(color = Color.Green)
                ,
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier

                ) {
                    val context = LocalContext.current
                    val mediaPlayer = remember { MediaPlayer.create(context, R.raw.slide_button_new) }
                    val selectedTimeText = selectedTime.value?.format(DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH))?.toUpperCase().orEmpty()
                    if (isChecked.value) {
                        LaunchedEffect(selectedTimeText) {
                            mediaPlayer.start()
                            delay(mediaPlayer.duration.toLong())
                            mediaPlayer.pause()
                            Vibration(context = context)
                        }
                    } else {
                        LaunchedEffect(selectedTimeText) {
                            Vibration(context = context)
                        }
                    }
                    Text(
                        text = selectedTimeText,

                        fontFamily = interDisplayFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        color = MaterialTheme.colors.primary,
                        style = androidx.compose.ui.text.TextStyle(letterSpacing = 0.5.sp),


                    )
                }

                val context = LocalContext.current



                Text(
                    text = "CLEAR",
                    modifier = Modifier
                        .clickable(indication = null,
                            interactionSource = remember { MutableInteractionSource() }) {
                            selectedTime.value = null
                            shouldUseCurrentTime = true
                            onClearClick()
                            Vibration(context)
                        }
                    ,
                    color = FABRed,
                    overflow = TextOverflow.Ellipsis,
                    fontFamily = interDisplayFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 11.sp,
                    style = androidx.compose.ui.text.TextStyle(letterSpacing = 1.sp)
                )
            }
            Box(modifier = Modifier.fillMaxWidth()
                .padding(top = 8.dp),
                contentAlignment = Alignment.Center){
                WheelTimePicker(
                    startTime = selectedTime.value ?: if (shouldUseCurrentTime) LocalTime.now() else initialTime!!,
                    timeFormat = TimeFormat.AM_PM,
                    size = DpSize(width = 170.dp, height = 200.dp),
                    rowCount = 5,
                    textStyle = TextStyle(
                        fontFamily = interDisplayFamily,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 1.sp
                    ),
                    textColor = MaterialTheme.colors.primary,
                    selectorProperties = WheelPickerDefaults.selectorProperties(
                        enabled = true,
                        color = MaterialTheme.colors.secondary,
                        border = BorderStroke(width = 0.dp, color = MaterialTheme.colors.primary.copy(alpha = 0.5f))
                    )
                ) { snappedTime ->
                    selectedTime.value = snappedTime
                    onTimeSelected(snappedTime)
                    shouldUseCurrentTime = false // Ensure we don't revert to current time unnecessarily
                }
            }

        }

    }
}




