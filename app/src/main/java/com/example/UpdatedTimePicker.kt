package com.example

import android.annotation.SuppressLint
import android.media.MediaPlayer
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.commandiron.wheel_picker_compose.WheelTimePicker
import com.commandiron.wheel_picker_compose.core.TimeFormat
import com.commandiron.wheel_picker_compose.core.WheelPickerDefaults
import com.example.Pages.Vibration
import com.example.dothings.R
import com.example.dothings.interDisplayFamily
import com.example.ui.theme.FABRed
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
){
val context = LocalContext.current
    Box(
        modifier = Modifier
            .padding(bottom = 24.dp)
           // .fillMaxWidth()
           // .wrapContentHeight()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(1f),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .padding(start = 24.dp)
                    .width(80.dp) // Set a fixed width to match the width of WheelTimePicker
            ) {
                val context = LocalContext.current
                val mediaPlayer = remember { MediaPlayer.create(context, R.raw.slide_time) }
                val selectedTimeText = selectedTime.value?.format(DateTimeFormatter.ofPattern("hh:mm a"))?.toUpperCase().orEmpty()
              if (isChecked.value){
                  LaunchedEffect(selectedTimeText) {
                      mediaPlayer.start()
                      // Delay to play the sound effect, adjust the delay as needed
                      delay(mediaPlayer.duration.toLong())
                      mediaPlayer.pause()

                  }
              }else{
                  Vibration(context = context)
              }
                Text(
                    text = selectedTimeText,
                    fontFamily = interDisplayFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    color = MaterialTheme.colors.secondary,
                    style = androidx.compose.ui.text.TextStyle(letterSpacing = 1.sp)
                )
            }
            val context = LocalContext.current
                        WheelTimePicker(
                            startTime = initialTime!! ,
                            timeFormat = TimeFormat.AM_PM,
                            size = DpSize(width = 170.dp, height = 200.dp),
                            rowCount = 5,
                            textStyle = TextStyle(
                                fontFamily = interDisplayFamily,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                letterSpacing = 1.sp

                            ),
                            textColor = MaterialTheme.colors.secondary ,
                            selectorProperties = WheelPickerDefaults.selectorProperties(
                                enabled = true,
                                color = MaterialTheme.colors.primary,
                                border = BorderStroke(width = 0.dp, color = MaterialTheme.colors.primary)
                            )

                        ) { snappedTime ->
                            selectedTime.value = snappedTime
                            onTimeSelected(snappedTime)


                        }
                  //  }

            Text(
                text = "CLEAR",
                modifier = Modifier
                    .clickable(indication = null,
                        interactionSource = remember { MutableInteractionSource() }) {
                        selectedTime.value = null
                        onClearClick()
                        Vibration(context)

                    }
                    .padding(end = 24.dp),
                color = FABRed,
                fontFamily = interDisplayFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                style = androidx.compose.ui.text.TextStyle(letterSpacing = 1.sp)
            )
                }

            }


        }




