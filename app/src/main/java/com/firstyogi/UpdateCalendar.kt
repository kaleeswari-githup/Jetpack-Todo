package com.firstyogi.dothing

import android.annotation.SuppressLint
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.firstyogi.ui.theme.FABRed
import org.w3c.dom.Text
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit
import java.util.*
import kotlin.math.absoluteValue

@SuppressLint("UnrememberedMutableState", "UnusedBoxWithConstraintsScope")
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun UpdatedCalendar(
    userSelectedtime: String,
    startDate: LocalDate,
    selectedTime: MutableState<LocalTime?>,
    selectedDate: MutableState<LocalDate>,
    isChecked: MutableState<Boolean>,
    repeatableOption: MutableState<String>,
    isClicked: MutableState<Boolean>,
    selectedRepeatoption:MutableState<String>
) {

    Log.d("repeatedtask","$repeatableOption")
    var isDoneButtonClicked by remember { mutableStateOf(false) }
    var isClearTextVisible by remember { mutableStateOf(true) }
    var isTimePickervisible by remember { mutableStateOf(false) }
    var isDatePickervisible by remember { mutableStateOf(false) }
    var newUserSelectedtime by remember { mutableStateOf(userSelectedtime) }
    var shouldUseCurrentTime by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose {
            isClicked.value = false
        }
    }
    LaunchedEffect(startDate) {
        if (selectedDate.value == startDate) {
            selectedDate.value = startDate
        }
    }
    BoxWithConstraints(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .wrapContentHeight()
                .background(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colors.primary)
                .animateContentSize(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                )
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }) { }
        ) {
            if (!isDatePickervisible && !isTimePickervisible && !isClicked.value) {
                UpdatedShrinkCalendar(startDate = selectedDate.value, selectedDate = selectedDate)
            } else {
                val setDateText = if (!isTimePickervisible && !isClicked.value) {
                    isDatePickervisible = false
                    ""
                }
                else{

                    selectedDate.value.format(DateTimeFormatter.ofPattern("EEE, d MMM", Locale.ENGLISH)).toString().toUpperCase()
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }) {
                            isDatePickervisible = true
                            isTimePickervisible = false
                            isClicked.value =
                                false // Reset isClicked when clicking on the date picker
                        }
                        .padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 24.dp),
                ) {
                    ButtonTextWhiteTheme(text = setDateText, color = MaterialTheme.colors.secondary,modifier = Modifier)
                }
            }
            Box(
                modifier = Modifier
                    .padding(bottom = 24.dp)
                    .height(1.dp)
                    .fillMaxWidth()
                    .background(color = MaterialTheme.colors.background)
            )
            if (isTimePickervisible) {
                UpdatedScrollableTimePicker(
                    initialTime = if (shouldUseCurrentTime) LocalTime.now() else parseTime(userSelectedtime),
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
                // Divider
                Box(
                    modifier = Modifier
                        .padding(bottom = 24.dp)
                        .height(1.dp)
                        .fillMaxWidth()
                        .background(color = MaterialTheme.colors.background)
                )
                isClicked.value = false

            } else {
                val setTimeText = if (selectedTime.value != null) {
                    selectedTime.value?.format(DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH))?.toUpperCase()
                } else if (!isTimePickervisible && isDatePickervisible && selectedTime.value != null) {
                    parseTime(userSelectedtime)?.format(DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH))?.toUpperCase()
                } else {
                    "SET TIME"
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, end = 24.dp)
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }) {
                            isTimePickervisible = true
                        },
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = setTimeText!!,
                        modifier = Modifier.padding(bottom = 24.dp),
                        fontFamily = interDisplayFamily,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        style = androidx.compose.ui.text.TextStyle(letterSpacing = 1.sp),
                        color = MaterialTheme.colors.secondary
                    )

                    if ((newUserSelectedtime != null && newUserSelectedtime.isNotEmpty() && isClearTextVisible) || selectedTime.value != null) {
                        Text(
                            text = "CLEAR",
                            modifier = Modifier.clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {
                                isDoneButtonClicked = true
                                isClearTextVisible = false
                                selectedTime.value = null
                                shouldUseCurrentTime = true
                            },
                            color = FABRed,
                            fontFamily = interDisplayFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp,
                            style = androidx.compose.ui.text.TextStyle(letterSpacing = 1.sp)
                        )
                    }
                }
                // Divider
                Box(
                    modifier = Modifier
                        .padding(bottom = 24.dp)
                        .height(1.dp)
                        .fillMaxWidth()
                        .background(color = MaterialTheme.colors.background)
                )
            }


            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, bottom = 24.dp)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }) {
                        isClicked.value = true
                        isTimePickervisible = false
                        isDatePickervisible = false
                    },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!isClicked.value) {
                    Text(
                        text = selectedRepeatoption.value,
                        fontFamily = interDisplayFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        color = if (isClicked.value) FABRed else MaterialTheme.colors.secondary,
                        style = androidx.compose.ui.text.TextStyle(letterSpacing = 1.sp),
                    )
                }
                if (isClicked .value ) {
                    isTimePickervisible = false
                    RepeatedTaskScreen(
                        initiallySelectedOption = selectedRepeatoption.value, // Pass selected option
                        onRepeatOptionSelected = { selectedOption ->
                            selectedRepeatoption.value = selectedOption
                          //  isClicked.value = false // Close repeat options after selection
                        })

                }
            }

            LaunchedEffect(Unit) {
                if (selectedTime.value == null && newUserSelectedtime.isNotEmpty()) {
                    val formatter = DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH)
                    val localTime = LocalTime.parse(newUserSelectedtime.toUpperCase(), formatter)
                    selectedTime.value = localTime
                }
            }
          /*  LaunchedEffect(Unit) {
                selectedDate.value = startDate
            }*/
        }
    }
}


@Composable
fun RepeatedTaskScreen(
    initiallySelectedOption: String,
    onRepeatOptionSelected: (String) -> Unit
) {
    val textOptions = listOf("NO REPEAT", "DAILY", "WEEKLY", "MONTHLY", "YEARLY")
    val clickedIndex = remember {
        mutableStateOf(textOptions.indexOf(initiallySelectedOption)) // Initialize with selected option
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        textOptions.forEachIndexed { index, text ->
            RepeatedTaskText(
                text = text,
                isClicked = index == clickedIndex.value,
                onClick = {
                    clickedIndex.value = index
                    onRepeatOptionSelected(text) // Update selected option
                }
            )
        }
    }
}

@Composable
fun RepeatedTaskText(text : String, isClicked: Boolean, onClick: () -> Unit){
    val context = LocalContext.current
    Box(modifier = Modifier
        .fillMaxWidth()
        .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {
            onClick()
            Vibration(context)
        }){
        Row(modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            Text(text = text,
                fontFamily = interDisplayFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                color = if (isClicked) FABRed else MaterialTheme.colors.secondary,
                style = androidx.compose.ui.text.TextStyle(letterSpacing = 1.sp),
            )
            if (isClicked){
                Box(modifier = Modifier
                    .size(4.dp)
                    .background(shape = CircleShape, color = FABRed))
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
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun UpdatedShrinkCalendar(
    startDate: LocalDate,
    selectedDate: MutableState<LocalDate>,
) {
    val startMonth = YearMonth.from(startDate)
    val totalPageCount = 1000
    val pastMonthCount = totalPageCount / 2
    val pagerState = rememberPagerState(
        initialPage = pastMonthCount,
        pageCount = { totalPageCount }
    )

    val context = LocalContext.current

    // Ensure pager state stays in sync with the selected date
    LaunchedEffect(selectedDate.value) {
        val selectedMonth = YearMonth.from(selectedDate.value)
        val currentPageMonth = startMonth.plusMonths((pagerState.currentPage - pastMonthCount).toLong())

        // Only scroll if the selected month is not the current visible month
        if (selectedMonth != currentPageMonth) {
            val tarrgetPage = pastMonthCount + ChronoUnit.MONTHS.between(startMonth, selectedMonth).toInt()
            pagerState.scrollToPage(tarrgetPage)
        }
    }

    HorizontalPager(
        state = pagerState,
        modifier = Modifier,
                contentPadding = PaddingValues(end = 48.dp)
    ) { page ->
        val month = startMonth.plusMonths((page - pastMonthCount).toLong())
        val daysInMonth = calculateDaysInMonth(month)
        val pageOffset = calculateCurrentOffsetForPage(page, pagerState)
        val alpha = 1f - 0.8f * pageOffset.absoluteValue.coerceIn(0f, 1f)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer { this.alpha = alpha }
        ) {
            // Month header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, start = 24.dp, end = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = month.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)).uppercase(),
                    fontFamily = interDisplayFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    style = androidx.compose.ui.text.TextStyle(letterSpacing = 1.sp),
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colors.secondary
                )
            }

            // Day labels (Weekdays)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp, bottom = 24.dp, start = 24.dp, end = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val daysOfWeek = arrayOf("SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT")
                for (dayOfWeek in daysOfWeek) {
                    Text(
                        text = dayOfWeek,
                        fontFamily = interDisplayFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp,
                        color = MaterialTheme.colors.secondary.copy(alpha = 0.5f),
                        textAlign = TextAlign.Center,
                        style = androidx.compose.ui.text.TextStyle(letterSpacing = 1.sp),
                        modifier = Modifier.width(32.dp)
                    )
                }
            }

            // Calendar days
            val weeks = daysInMonth.chunked(7)
            weeks.forEach { week ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 22.dp, end = 22.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (day in week) {
                        val isCurrentMonth = day.monthValue == month.monthValue
                        val isCurrentDate = day == LocalDate.now()
                        val isSelectedDate = day == selectedDate.value
                        val textColor = when {
                            isSelectedDate -> MaterialTheme.colors.primary
                            isCurrentDate -> FABRed
                            isCurrentMonth -> MaterialTheme.colors.secondary
                            else -> Color.Transparent
                        }
                        val background = if (isSelectedDate && isCurrentMonth) {
                            MaterialTheme.colors.secondary
                        } else {
                            Color.Transparent
                        }
                        val isClickable = isCurrentMonth
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(background, shape = CircleShape)
                                .clickable(
                                    indication = null,
                                    interactionSource = remember { MutableInteractionSource() }
                                ) {
                                    if (isClickable) {
                                        selectedDate.value = day
                                        Vibration(context)
                                    }
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (isCurrentMonth) {
                                Text(
                                    text = day.dayOfMonth.toString(),
                                    fontFamily = interDisplayFamily,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp,
                                    style = androidx.compose.ui.text.TextStyle(letterSpacing = 1.sp),
                                    color = textColor
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
@OptIn(ExperimentalFoundationApi::class)
fun calculateCurrentOffsetForPage(
    page: Int,
    pagerState: PagerState
): Float {
    return (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
}
@OptIn(ExperimentalFoundationApi::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun UdatedShrinkCalendar(
    startDate:LocalDate,
    selectedDate: MutableState<LocalDate>,
){
    val startMonth = YearMonth.from(startDate)

    val context = LocalContext.current
    val initialPage = remember(startDate, selectedDate.value) {
        val monthsBetween = ChronoUnit.MONTHS.between(startMonth, YearMonth.from(selectedDate.value))
        monthsBetween.toInt() // Calculate months difference
    }

    val pagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { 1000 } // Large number to allow extensive scrolling (can also be infinite if needed)
    )
    HorizontalPager(
        // pageCount = Int.MAX_VALUE, // Infinite scrolling (limited by implementation)
        state = pagerState,
        modifier = Modifier.fillMaxWidth()
    ) { pageIndex ->
        val currentMonth = startMonth.plusMonths(pageIndex.toLong())

        val days: List<LocalDate> = remember(currentMonth) {
            calculateDaysInMonth(currentMonth)
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, start = 24.dp, end = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = (currentMonth.format(
                        DateTimeFormatter.ofPattern(
                            "MMMM yyyy",
                            Locale.ENGLISH
                        )
                    )).uppercase(),
                    fontFamily = interDisplayFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    style = androidx.compose.ui.text.TextStyle(letterSpacing = 1.sp),
                    modifier = Modifier
                        .weight(1f),
                    color = MaterialTheme.colors.secondary
                )


            }

            // Day labels
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp, bottom = 24.dp, start = 24.dp, end = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val daysOfWeek = arrayOf("SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT")
                for (dayOfWeek in daysOfWeek) {
                    Text(
                        text = dayOfWeek,
                        fontFamily = interDisplayFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp,
                        color = MaterialTheme.colors.secondary.copy(alpha = 0.5f),
                        textAlign = TextAlign.Center,
                        style = androidx.compose.ui.text.TextStyle(letterSpacing = 1.sp),
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
                            isSelectedDate -> MaterialTheme.colors.primary
                            isCurrentDate -> FABRed
                            isCurrentMonth -> MaterialTheme.colors.secondary
                            else -> Color.Transparent
                        }
                        val background = if (isSelectedDate && isCurrentMonth) {
                            MaterialTheme.colors.secondary
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
                                        Vibration(context)
                                    }
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (isCurrentMonth) {
                                Text(
                                    text = day.dayOfMonth.toString(),
                                    fontFamily = interDisplayFamily,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp,
                                    style = androidx.compose.ui.text.TextStyle(letterSpacing = 1.sp),
                                    color = textColor

                                )
                            }
                        }
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
