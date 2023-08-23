package com.example

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.annotation.RequiresApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.commandiron.wheel_picker_compose.WheelTimePicker
import com.commandiron.wheel_picker_compose.core.TimeFormat
import com.commandiron.wheel_picker_compose.core.WheelPickerDefaults
import com.example.Pages.Vibration
import com.example.dothings.interDisplayFamily
import com.example.ui.theme.FABRed
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
val context = LocalContext.current
    Box(
        modifier = Modifier
            .padding(bottom = 24.dp)
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
                val context = LocalContext.current
                val lifecycleOwner = LocalLifecycleOwner.current
                val lifecycle = lifecycleOwner.lifecycle
                val currentOnTimeSelected = rememberUpdatedState(selectedTime.value)
                LaunchedEffect(selectedTime.value) {
                    ScrollabeVibration(context, lifecycle)
                }

              //  val parsedTime = parseTime(initialTime)
               // Log.d("userTime","$parsedTime")
                        WheelTimePicker(
                            startTime = initialTime!! ,
                            timeFormat = TimeFormat.AM_PM,
                            size = DpSize(width = 170.dp, height = 200.dp),
                            rowCount = 5,
                            textStyle = TextStyle(
                                fontFamily = interDisplayFamily,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
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
                fontSize = 12.sp
            )
                }

            }


        }


@SuppressLint("ServiceCast")
fun ScrollabeVibration(context: Context, lifecycle: Lifecycle) {
    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    val vibrationEffect2: VibrationEffect
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        vibrationEffect2 = VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
        vibrator.cancel()
        vibrator.vibrate(vibrationEffect2)
    }

    val observer = LifecycleEventObserver { _, event ->
        if (event == Lifecycle.Event.ON_DESTROY) {
            vibrator.cancel()
        }
    }
    lifecycle.addObserver(observer)
}

