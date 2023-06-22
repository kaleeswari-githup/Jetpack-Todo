package com.example

import android.annotation.SuppressLint
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.commandiron.wheel_picker_compose.WheelTimePicker
import com.commandiron.wheel_picker_compose.core.TimeFormat
import com.commandiron.wheel_picker_compose.core.WheelPickerDefaults
import com.example.dothings.interDisplayFamily
import com.example.ui.theme.FABDarkColor
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.*

@SuppressLint("SuspiciousIndentation")
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun UpdatedScrollableTimePicker(
    selectedTime: MutableState<LocalTime?>,
    onClearClick: () -> Unit,
    initialTime: LocalTime?,
    onTimeSelected: (LocalTime) -> Unit
){
    Box(
        modifier = Modifier
            .padding(start = 8.dp, end = 8.dp)
            .fillMaxWidth()
            .wrapContentHeight()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                modifier = Modifier.padding(start = 24.dp),
                text = selectedTime.value?.format(DateTimeFormatter.ofPattern("hh:mm a"))?.toUpperCase().orEmpty(),
                fontFamily = interDisplayFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp
            )
            Box(
                modifier = Modifier
                    .wrapContentSize()
                    .align(Alignment.CenterVertically)
            ) {


              //  val parsedTime = parseTime(initialTime)
               // Log.d("userTime","$parsedTime")
                        WheelTimePicker(
                            startTime = initialTime!! ,
                            timeFormat = TimeFormat.AM_PM,
                            size = DpSize(width = 160.dp, height = 160.dp),
                            textStyle = TextStyle(
                                fontFamily = interDisplayFamily,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            selectorProperties = WheelPickerDefaults.selectorProperties(
                                enabled = true,
                                color = Color.White,
                                border = BorderStroke(width = 0.dp, color = Color.White)
                            )
                        ) { snappedTime ->
                            selectedTime.value = snappedTime
                            onTimeSelected(snappedTime)
                        }
                    }

            Text(
                text = "Clear",
                modifier = Modifier
                    .clickable {
                        selectedTime.value = null
                        onClearClick()
                    }
                    .padding(end = 24.dp),
                color = FABDarkColor,
                fontFamily = interDisplayFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp
            )
                }

            }


        }



