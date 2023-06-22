package com.example.Pages

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.example.UpdatedCalendar
import com.example.dothings.R
import com.example.dothings.interDisplayFamily
import com.example.ui.theme.FABDarkColor
import com.example.ui.theme.Text1
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalComposeUiApi::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun UpdatedCalendarAndTimePickerScreen(
    onDismiss: () -> Unit,
    userSelectedDate:LocalDate?,
    userSelectedTime: String?= "",
    onDateTimeSelected: (String, String) -> Unit,
    id:String) {
    var selectedDate = remember { mutableStateOf(LocalDate.now()) }
    var isTimePickerVisible by remember { mutableStateOf(false) }

    var selectedTime = remember {
        mutableStateOf(if (isTimePickerVisible) LocalTime.now() else null)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        (LocalView.current.parent as DialogWindowProvider)?.window?.setDimAmount(0.1f)
        Column(modifier = Modifier,
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier.padding(start = 8.dp,end = 8.dp),
                contentAlignment = Alignment.Center) {

                UpdatedCalendar(
                    selectedDate = selectedDate,
                    userSelectedtime = userSelectedTime?.toString() ?: "",
                    startDate = userSelectedDate ?: LocalDate.now(),
                    onDateSelected = { date ->
                        selectedDate.value = date
                    },
                    selectedTime = selectedTime,
                )

            }
            Row(modifier = Modifier
                .padding(top = 24.dp)
                .fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Cancel",
                    modifier = Modifier.clickable {
                        onDismiss.invoke()
                    },
                    fontFamily = interDisplayFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp,
                    color = Text1
                )
                Spacer(modifier = Modifier.padding(40.dp))
                Button(
                    onClick = {
                        Log.d("id","$id")
                        val dateFormatter = DateTimeFormatter.ofPattern("MM/dd/yyyy")
                        val dateString: String = selectedDate.value.format(dateFormatter)

                        val timeFormatter = DateTimeFormatter.ofPattern("hh:mm a")
                        val timeString: String = selectedTime.value?.format(timeFormatter) ?: ""

                        Log.d("UpdatedDateString","$dateString")
                        onDateTimeSelected(dateString, timeString)
                        onDismiss.invoke()
                    },
                    shape = RoundedCornerShape(53.dp),
                    modifier = Modifier
                        .size(width = 105.dp, height = 48.dp),
                    colors = ButtonDefaults.buttonColors(backgroundColor = FABDarkColor),

                    ) {
                    Image(
                        painter = painterResource(id = R.drawable.tick),
                        contentDescription = "Save Tick",
                        modifier = Modifier
                            .size(16.dp)
                    )
                    Text(
                        text = "Done",
                        fontFamily = interDisplayFamily,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color.White,
                        modifier = Modifier.padding(start = 16.dp)
                    )
                }
            }

        }
    }



}