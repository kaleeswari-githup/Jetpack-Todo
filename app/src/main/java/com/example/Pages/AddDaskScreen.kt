package com.example.Pages

import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.*
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.example.dothings.R
import com.example.dothings.R.DataClass
import com.example.dothings.bigRoundedCircleGradient
import com.example.dothings.interDisplayFamily
import com.example.ui.theme.*
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter


@OptIn(ExperimentalComposeUiApi::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun AddDaskScreen(
    selectedDate: MutableState<LocalDate?>,
    selectedTime: MutableState<LocalTime?>,
    textValue:String,
    onDismiss: () -> Unit,
) {
    var task = rememberSaveable {
        mutableStateOf(textValue)
    }
    val mutableSelectedTime = remember { mutableStateOf(selectedTime) }
    val mutableSelectedDate = remember { mutableStateOf(selectedDate) }
    var context = LocalContext.current
    val database = FirebaseDatabase.getInstance()
    val databaseRef = database.reference.child("Task")
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
        val data = DataClass(id,messageText ?: "",userSelectedTime ?: "",userSelectedDate ?: "")
        databaseRef.child(id).setValue(data).addOnCompleteListener{task ->
            if(task.isSuccessful){
                Toast.makeText(context, "Added successfully", Toast.LENGTH_SHORT).show()
                onDismiss.invoke()

            }else{
                Toast.makeText(context, task.exception.toString(), Toast.LENGTH_SHORT).show()
            }

        }
    }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnClickOutside = true,
            dismissOnBackPress = true,
            usePlatformDefaultWidth = false
            )
    ){
        val dialogWindow = (LocalView.current.parent as DialogWindowProvider).window

// Set the background color with opacity
        val backgroundColor = Color(1f, 1f, 1f, 0.6f) // White color with 50% opacity
        dialogWindow.setBackgroundDrawable(ColorDrawable(backgroundColor.toArgb()))
        Box(modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            ) {

            Column(modifier = Modifier,
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally) {

                AddDaskCircleDesign( mutableSelectedDate.value,mutableSelectedTime.value,task = task, onTaskChange = {newTask ->
                    if (newTask.length <= maxValue){
                        task.value = newTask
                    }
                },onDoneClick = onDoneClick)
                TwoButtons(
                    onDoneClick = onDoneClick,
                onDismiss = onDismiss)
            }
        }
    }

}



@OptIn(ExperimentalComposeUiApi::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun AddDaskCircleDesign(
    selectedDate: MutableState<LocalDate?>,
    selectedTime: MutableState<LocalTime?>,
    task:MutableState<String>,
    onTaskChange: (String) -> Unit,
    onDoneClick: () -> Unit){
    val focusRequester = remember { FocusRequester() }
    val softwareKeyboardController = LocalSoftwareKeyboardController.current
    var isPickerOpen by remember { mutableStateOf(false) }
    Box(
    modifier = Modifier
        .fillMaxWidth()
        .padding(start = 24.dp, end = 24.dp, top = 38.dp)
        .background(bigRoundedCircleGradient, shape = CircleShape)
        .size(344.dp)
        .clip(CircleShape),
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
            keyboardOptions = KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Done),
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
        .background(color = Color.White, shape = CircleShape)
        .clickable {
            isPickerOpen = true
        }
        .padding(4.dp)

) {
    if (selectedDate == null && selectedTime == null){
        Icon(
            painter = painterResource(id = R.drawable.calendar_icon),
            contentDescription = "calender_icon",
            modifier = Modifier.align(Alignment.Center)
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
    if (isPickerOpen) {
        UpdatedCalendarAndTimePickerScreen(
            onDismiss = { isPickerOpen = false },
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
            userSelectedTime = null,
            userSelectedDate = null
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 32.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
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
            onClick = onDoneClick,
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
                text = "Save",
                fontFamily = interDisplayFamily,
                fontSize = 15.sp,
                fontWeight = FontWeight.Normal,
                color = Color.White,
                modifier = Modifier.padding(start = 16.dp)
            )
        }
    }

}
