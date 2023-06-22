package com.example.Pages

import android.annotation.SuppressLint
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
import androidx.compose.material.SnackbarDefaults.backgroundColor
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.example.dothings.*
import com.example.dothings.R
import com.example.ui.theme.FABDarkColor
import com.example.ui.theme.NewtaskColorGray
import com.example.ui.theme.SurfaceGray
import com.example.ui.theme.Text1
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.*

@SuppressLint("UnrememberedMutableState")
@OptIn(ExperimentalComposeUiApi::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun UpdateTaskScreen(
    selectedDate: MutableState<String>,
    selectedTime:MutableState<String>,
    textValue:String,
    id:String,
    openKeyboard: Boolean,
    onDismiss : () -> Unit
) {
    var task = rememberSaveable {
        mutableStateOf(textValue)
    }

    val maxValue = 32
    val database = FirebaseDatabase.getInstance()
    val databaseRef = database.reference.child("Task")
    var context = LocalContext.current
    val onDoneClick:(String,String) -> Unit = { updatedDate,updatedTime ->
        val originalDateFormat = DateTimeFormatter.ofPattern("EEE, d MMM yyyy",Locale.ENGLISH) // Assuming the date format in the database is in ISO format
        val desiredDateFormat = DateTimeFormatter.ofPattern("MM/dd/yyyy") // Desired format: "EEE, d MMM"

        val dateStringFromDatabase = updatedDate // Retrieve the date string from the database

// Parse the date string with the original format

        val originalDate: LocalDate? = if (dateStringFromDatabase.isNotEmpty()) {
            LocalDate.parse(dateStringFromDatabase, originalDateFormat)
        } else {
            LocalDate.MIN // Assign LocalDate.MIN when dateStringFromDatabase is empty
        }

// Format the date with the desired format if originalDate is not LocalDate.MIN
        val formattedDate = if (originalDate != LocalDate.MIN) {
            originalDate?.format(desiredDateFormat) ?: ""
        } else {
            "" // Assign empty string if originalDate is LocalDate.MIN
        }


            val timeFormat = updatedTime.format(DateTimeFormatter.ofPattern("hh:mm a"))?.toUpperCase() ?: ""
            val updatedData = HashMap<String, Any>()
            updatedData["id"] = id
            updatedData["message"] = task.value ?: ""
            updatedData["time"] = timeFormat
            updatedData["date"] = formattedDate
            databaseRef.child(id).updateChildren(updatedData)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(context, "Updated successfully", Toast.LENGTH_SHORT).show()
                        onDismiss.invoke()
                    } else {
                        Toast.makeText(context, task.exception.toString(), Toast.LENGTH_SHORT).show()
                    }
                }


    }


    val onBackPressed: () -> Unit = {
        onDoneClick(selectedDate.value,selectedTime.value)
    }
    Dialog(onDismissRequest = onBackPressed ,
    properties = DialogProperties(
        dismissOnClickOutside = true,
        dismissOnBackPress = true,
        usePlatformDefaultWidth = false
    )
) {

    Box(modifier = Modifier
        .fillMaxSize()
        .background(color = SurfaceGray)
    ) {val dialogWindow = (LocalView.current.parent as DialogWindowProvider).window
        dialogWindow.setBackgroundDrawable(ColorDrawable(backgroundColor.toArgb()))
       // Image(painter = painterResource(id = R.drawable.grid_lines), contentDescription = null)
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center){
            Column(modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally) {


                UpdateCircleDesign(
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

                    )

                Box(
                    modifier = Modifier
                        .padding(bottom = 40.dp)

                ) {
                    UpdatedButtons( id = id)
                }

            }
        }

        CrossFloatingActionButton(onClick = {
            onDoneClick.invoke(selectedDate.value,selectedTime.value)
        })
    }
}

}


@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun UpdateCircleDesign(
    message: MutableState<String>,
    initialSelectedate: MutableState<String>,
    initialSelectedtime: MutableState<String> ,
    id:String,
    onTaskChange:(String) -> Unit,

    openKeyboard:Boolean,

){
   /* val selectedDate = remember { mutableStateOf(initialSelectedate.value) }
    val selectedTime = remember { mutableStateOf(initialSelectedtime.value) }*/
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    var isPickerOpen by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 24.dp, top = 38.dp)
            .background(bigRoundedCircleGradient, shape = CircleShape)
            .size(344.dp)
            .clip(CircleShape),
        contentAlignment = Alignment.Center
    ){

        Column(modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center) {
            val messageState = remember { mutableStateOf(message.value) }
            TextField(
                value = message.value,
                onValueChange = onTaskChange ,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 32.dp, end = 32.dp)
                    .focusRequester(focusRequester)
                    .onFocusChanged { focusState ->
                        if (!focusState.isFocused) {
                            keyboardController?.hide()
                        }
                    },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone ={
                        keyboardController?.hideSoftwareKeyboard()
                        focusManager.clearFocus(true)
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
                            .fillMaxWidth()
                            .padding(horizontal = 70.dp),
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

            )
            Box(
                modifier = Modifier
                    .wrapContentSize(Alignment.Center)
                    .padding(
                        top = 20.dp,
                        start = 48.dp,
                        end = 48.dp
                    )
                    .background(color = Color.White, shape = CircleShape)
                    .clickable {
                        isPickerOpen = true
                    }
                    .padding(4.dp)


            ) {
                if (initialSelectedate == null && initialSelectedtime == null){
                    Icon(
                        painter = painterResource(id = R.drawable.calendar_icon),
                        contentDescription = "calender_icon",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }else if(initialSelectedate != null && initialSelectedtime == null) {
                    val formatter = DateTimeFormatter.ofPattern("EEE, d MMM")
                    val formattedDate = initialSelectedate.value.format(formatter) ?: ""
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
                    val formattedDate = initialSelectedate.value.format(formatter) ?: ""
                    val dateString:String = formattedDate
                    val timeFormat = initialSelectedtime.value.format(DateTimeFormatter.ofPattern("hh:mm a"))?.toUpperCase() ?: ""
                    val timeString:String = timeFormat
                    Log.d("timestring","$timeString")
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
                    val pattern = "EEE, d MMM yyyy"
                    val locale = Locale.ENGLISH

                    val formatter = DateTimeFormatter.ofPattern(pattern, locale)
                        .withZone(ZoneId.of("America/New_York"))

                    val localDate = if (initialSelectedate.value.isEmpty()) {
                        LocalDate.now()
                    } else {
                        try {
                            LocalDate.parse(initialSelectedate.value, formatter)
                        } catch (e: DateTimeParseException) {
                            LocalDate.now()
                        }
                    }

                    UpdatedCalendarAndTimePickerScreen(
                        userSelectedDate = localDate,
                        userSelectedTime = initialSelectedtime.value,
                        onDismiss = { isPickerOpen = false },
                        onDateTimeSelected = { date, time ->
                            val defaultDateFormat = DateTimeFormatter.ofPattern("MM/dd/yyyy")
                            val desiredDateFormat = DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale.ENGLISH)

                            val defaultDateString = date
                            val parsedDate = LocalDate.parse(defaultDateString, defaultDateFormat)
                            val formattedDate = parsedDate.format(desiredDateFormat)

                            initialSelectedate.value = formattedDate
                            initialSelectedtime.value = time
                        },
                        id = id

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
fun UpdatedButtons(id: String){
    val coroutineScope = rememberCoroutineScope()
    val database = FirebaseDatabase.getInstance()
    val databaseRef = database.reference.child("Task")
    var context = LocalContext.current
    Box(modifier = Modifier
        .fillMaxWidth()
        .padding(start = 42.dp, end = 42.dp)
        .height(48.dp)
        .background(color = Color.White, shape = RoundedCornerShape(30.dp)),
contentAlignment = Alignment.Center
    ) {
        Row(modifier = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.clickable {
                coroutineScope.launch {
                    Log.d("MyTag", "id: $id")
                    databaseRef
                        .child(id)
                        .removeValue()
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                Toast.makeText(context, "Task Deleted", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, task.exception?.message, Toast.LENGTH_SHORT).show()
                            }
                        }
                }

            }
            ) {
               Image(painter = painterResource(id = R.drawable.trash_delete), contentDescription = null )
               Interfont(text = "Delete")
            }
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .fillMaxHeight()
                    .background(color = SurfaceGray)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Image(painter = painterResource(id = R.drawable.square), contentDescription = null)
                Interfont(text = "Mark completed")
            }
        }

    }
}
@Composable
fun Interfont(text:String){
    Text(
        text = text,
        fontFamily = interDisplayFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        color = Text1
    )
}

@Composable
fun CrossFloatingActionButton(onClick:() -> Unit){
    Box(modifier = Modifier
        .size(72.dp)
        .padding(top = 12.dp, start = 24.dp)
        .fillMaxWidth()
        .fillMaxHeight()


        .background(Color.Transparent)
    ) {
        androidx.compose.material.FloatingActionButton(
            modifier = Modifier
                .size(48.dp)
                .align(Alignment.BottomCenter)
                ,
            elevation = FloatingActionButtonDefaults.elevation(0.dp),
            onClick = {onClick.invoke()},
            shape = CircleShape,
            contentColor = Color.Black,
            backgroundColor = Color.White

            ) {
            Icon(imageVector = Icons.Default.Close, contentDescription = "")
        }
    }
}
