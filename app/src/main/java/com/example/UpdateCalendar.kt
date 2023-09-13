package com.example

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dothings.interDisplayFamily
import com.example.ui.theme.FABRed
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.*

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun UpdatedCalendar(
    userSelectedtime: String,
    startDate:LocalDate,
    selectedTime: MutableState<LocalTime?>,
    selectedDate: MutableState<LocalDate>,
    onDateSelected: (LocalDate) -> Unit,
    isChecked: MutableState<Boolean>
) {
    var isDoneButtonClicked by remember { mutableStateOf(false) }
    var isClearTextVisible by remember { mutableStateOf(true) }
    var isTimePickervisible by remember{ mutableStateOf(false) }
    var isDatePickervisible by remember{ mutableStateOf(false) }
    var newUserSelectedtime by remember { mutableStateOf(userSelectedtime) }


    Box(modifier = Modifier
        .fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Column(modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .background(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colors.primary)
            .animateContentSize(animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow))
            .clickable(indication = null,
                interactionSource = remember { MutableInteractionSource() }) {  }
        ) {
            // Month navigation buttons
          //  UpdatedShrinkCalendar(startDate = startDate, selectedDate = selectedDate)
            if (!isDatePickervisible && !isTimePickervisible){
                UpdatedShrinkCalendar(startDate = selectedDate.value, selectedDate = selectedDate)
            }else{
                val setDateText = if (!isTimePickervisible) {
                    isDatePickervisible = false
                    ""
                } else {
                    selectedDate.value.format(DateTimeFormatter.ofPattern("EEE, d MMM")).toString()
                }
                Row(modifier = Modifier

                    .fillMaxWidth()
                    .clickable (indication = null,
                        interactionSource = remember { MutableInteractionSource() }){
                        isDatePickervisible = true
                        isTimePickervisible = false
                    }
                    .padding(start = 24.dp, end = 24.dp, top = 24.dp,bottom = 24.dp)
                    ,
                    ) {
                    Text(
                        text = setDateText,
                        modifier = Modifier
                        ,
                        fontFamily = interDisplayFamily,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        style = androidx.compose.ui.text.TextStyle(letterSpacing = 0.sp),
                        color= MaterialTheme.colors.secondary
                    )
                }

            }

            Box(
                modifier = Modifier
                    .padding( bottom = 24.dp,)
                    .height(1.dp)
                    .fillMaxWidth()
                    .background(color = MaterialTheme.colors.background)
            )
            Log.d("SecondInitialtime","$userSelectedtime")
            if (isTimePickervisible){
                UpdatedScrollableTimePicker(
                    initialTime = parseTime(userSelectedtime) ,
                    selectedTime = selectedTime,
                    onClearClick = {
                        isDoneButtonClicked = true
                        isTimePickervisible = false
                        isClearTextVisible = false
                       newUserSelectedtime = ""

                    },
                    onTimeSelected = { time ->
                        selectedTime.value = time
                    },
                    isChecked = isChecked
                )


            }
            else {

                val setTimeText = if ( selectedTime.value != null) {
                    selectedTime.value?.format(DateTimeFormatter.ofPattern("hh:mm a"))?.toUpperCase()
                } else if (!isTimePickervisible && isDatePickervisible && selectedTime.value != null) {
                    parseTime(userSelectedtime)?.format(DateTimeFormatter.ofPattern("hh:mm a"))?.toUpperCase()
                } else {
                    "Set time"
                }
                Log.d("selectedtime.value","$selectedTime.value")
                Row(modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp)
                    .clickable(indication = null,
                        interactionSource = remember { MutableInteractionSource() }) { isTimePickervisible = true },
                    horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        text = setTimeText!!,
                        modifier = Modifier
                            .padding( bottom = 24.dp),
                        fontFamily = interDisplayFamily,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        style = androidx.compose.ui.text.TextStyle(letterSpacing = 0.sp),
                        color = MaterialTheme.colors.secondary
                    )

                    if ((newUserSelectedtime != null && newUserSelectedtime.isNotEmpty() && isClearTextVisible) || selectedTime.value != null) {
                        Text(
                            text = "Clear",
                            modifier = Modifier
                                .clickable(indication = null,
                                    interactionSource = remember { MutableInteractionSource() }) {
                             isDoneButtonClicked = true
                                    isClearTextVisible = false
                                    selectedTime.value = null
                                },
                            color = FABRed,
                            fontFamily = interDisplayFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp,
                            style = androidx.compose.ui.text.TextStyle(letterSpacing = 0.sp)
                        )
                    }
                }

            }
            LaunchedEffect(Unit){
                if (selectedTime.value == null && newUserSelectedtime.isNotEmpty()){
                    val formatter = DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH)
                    val localTime =  LocalTime.parse(newUserSelectedtime.toUpperCase(), formatter)
                    selectedTime.value = localTime
                }
            }



            LaunchedEffect(Unit) {
                selectedDate.value = startDate
            }


        }
    }


}


@RequiresApi(Build.VERSION_CODES.O)
fun parseTime(timeString: String?): LocalTime? {
    return try {
        if (timeString.isNullOrEmpty()) {
            LocalTime.now()
        } else {
            val formatter = DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH)
            LocalTime.parse(timeString.toUpperCase(), formatter)
        }
    } catch (e: DateTimeParseException) {
        null
    }
}


@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun UpdatedShrinkCalendar(
    startDate:LocalDate,
    selectedDate: MutableState<LocalDate>,
){
    val startMonth = YearMonth.from(startDate)
    var currentMonth by remember(startMonth) { mutableStateOf(startMonth) }
    var monthOffset by remember { mutableStateOf(0) }

    LaunchedEffect(monthOffset) {
        currentMonth = startMonth.plusMonths(monthOffset.toLong())
    }
    val days: List<LocalDate> = remember(currentMonth) {
        calculateDaysInMonth(currentMonth)
    }
    Log.d("userselecteddate","$selectedDate")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, start = 24.dp, end = 24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = currentMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
            fontFamily = interDisplayFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 15.sp,
            style = androidx.compose.ui.text.TextStyle(letterSpacing = 0.sp),
            modifier = Modifier
                .weight(1f),
            color = MaterialTheme.colors.secondary
        )
        IconButton(
            onClick = {
                monthOffset--
            }
        ) {
            Icon(
                Icons.Filled.KeyboardArrowLeft,
                contentDescription = "Previous Month",
               tint = MaterialTheme.colors.secondary)
        }

        IconButton(
            onClick = {
                monthOffset++
            }
        ) {
            Icon(Icons.Filled.KeyboardArrowRight,
                contentDescription = "Next Month",
                tint = MaterialTheme.colors.secondary)
        }
    }

    // Day labels
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp, bottom = 24.dp, start = 24.dp, end = 24.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        val daysOfWeek = arrayOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
        for (dayOfWeek in daysOfWeek) {
            Text(
                text = dayOfWeek,
                fontFamily = interDisplayFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                color = MaterialTheme.colors.secondary.copy(alpha = 0.5f),
                textAlign = TextAlign.Center,
                style = androidx.compose.ui.text.TextStyle(letterSpacing = 0.sp),
                modifier = Modifier.width(32.dp)
            )
        }
    }

    // Calendar days
    val weeks = days.chunked(7)


    weeks.forEach { week ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 22.dp, end = 22.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (day in week) {
                val isCurrentMonth = day.monthValue == currentMonth.monthValue
                val isCurrentDate = day == LocalDate.now()
                val isSelectedDate = day == selectedDate.value
                val textColor = when {
                    isSelectedDate -> Color.White
                    isCurrentDate -> FABRed
                    isCurrentMonth -> MaterialTheme.colors.secondary
                    else -> Color.Transparent
                }
                val background = if (isSelectedDate && isCurrentMonth) {
                    Color.Black
                } else {
                    Color.Transparent
                }
                val isClickable = isCurrentMonth
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(background, shape = CircleShape)
                        .clickable(indication = null,
                            interactionSource = remember { MutableInteractionSource() }) {
                            if (isClickable) {
                                selectedDate.value = day
                            }
                        }
                    ,
                    contentAlignment = Alignment.Center,
                ) {
                    if (isCurrentMonth) {
                        Text(
                            text = day.dayOfMonth.toString(),
                            fontFamily = interDisplayFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp,
                            style = androidx.compose.ui.text.TextStyle(letterSpacing = 0.sp),
                            color = textColor

                        )
                    }
                }
            }
        }
    }

}
@RequiresApi(Build.VERSION_CODES.O)
fun calculateDaysInMonth(month: YearMonth): List<LocalDate> {
    val firstOfMonth = month.atDay(1)
    val lastOfMonth = month.atEndOfMonth()
    val startDayOfWeek = firstOfMonth.dayOfWeek.value % 7
    val daysInMonth = month.lengthOfMonth()
    val days = mutableListOf<LocalDate>()
    repeat(startDayOfWeek) {
        days.add(firstOfMonth.minusDays((startDayOfWeek - it).toLong()))
    }
    days.addAll((1..daysInMonth).map { firstOfMonth.plusDays((it - 1).toLong()) })
    repeat(6 * 7 - days.size) {
        days.add(lastOfMonth.plusDays((it + 1).toLong()))
    }
    return days
}
