package com.example.Pages

import android.annotation.SuppressLint
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.annotation.RequiresApi
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController
import com.example.dothings.*
import com.example.dothings.R
import com.example.dothings.R.DataClass
import com.example.ui.theme.FABRed
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.*



@SuppressLint("UnrememberedMutableState")
@OptIn(ExperimentalComposeUiApi::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun UpdateTaskScreen(
    navController: NavController,
    id: String?,
    openKeyboard: Boolean,
     ) {
    var isUpdatePickerOpen = remember { mutableStateOf(false) }
    val maxValue = 32
    val database = FirebaseDatabase.getInstance()
    val user = FirebaseAuth.getInstance().currentUser
    val uid = user?.uid
    val databaseRef: DatabaseReference = database.reference.child("Task").child(uid.toString())
    var data by remember { mutableStateOf<DataClass?>(null) }
    var dataClassMessage by remember { mutableStateOf("") }
    var dataClassDate= remember { mutableStateOf("") }
    var dataClassTime = remember{ mutableStateOf("") }
    Log.d("UpdateMessage","$dataClassMessage")
    Log.d("UpdateDate","$dataClassDate")
    Log.d("UpdateTime","$dataClassTime")

    DisposableEffect(Unit) {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val selectedData = snapshot.child(id.toString()).getValue(DataClass::class.java)
                if (selectedData != null) {
                    data = selectedData

                        val originalDateFormat = DateTimeFormatter.ofPattern("MM/dd/yyyy")
                        val desiredDateFormat = DateTimeFormatter.ofPattern("EEE, d MMM yyyy")

                        // Check if the date is not empty before attempting to parse
                        if (!selectedData.date.isNullOrBlank()) {
                            try {
                                val parsedDate = LocalDate.parse(selectedData.date, originalDateFormat)
                                dataClassDate.value = parsedDate.format(desiredDateFormat)
                            } catch (e: DateTimeParseException) {
                                // Handle parsing error if needed
                                Log.e("DateParsingError", "Error parsing date: ${selectedData.date}", e)
                            }
                        } else {
                            // Handle the case where the date is empty
                            dataClassDate.value = ""
                        }


                    // Assuming 'message' is a property of DataClass
                    dataClassMessage = selectedData.message ?: ""
                 //   dataClassDate.value = selectedData.date?:""
                    dataClassTime.value = selectedData.time?:""


                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle error if needed
            }
        }

        databaseRef.addListenerForSingleValueEvent(listener)

        onDispose {
            // Remove the listener when the composable is no longer in the view hierarchy
            databaseRef.removeEventListener(listener)
        }
    }

//Log.d("selectedDate","$selectedDate.value")
        var context = LocalContext.current
        val onDoneClick:(String,String) -> Unit = { updatedDate,updatedTime ->
            val originalDateFormat = DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale.ENGLISH)
            val desiredDateFormat = DateTimeFormatter.ofPattern("MM/dd/yyyy")
            val timeFormat = DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH) // Ensure to set the proper Locale

            val dateStringFromDatabase = updatedDate

            val originalDate: LocalDate? = if (dateStringFromDatabase.isNotEmpty()) {
                LocalDate.parse(dateStringFromDatabase, originalDateFormat)
            } else {
                null
            }

            val formattedDate = originalDate?.format(desiredDateFormat) ?: ""

            val timeFormats = listOf(
                DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH),
                DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH).withLocale(Locale.US),
                // Add more variations as needed
            )

            var formattedTime: LocalTime? = null

            for (format in timeFormats) {
                try {
                    formattedTime = LocalTime.parse(updatedTime.toUpperCase(), format)
                    break
                } catch (e: DateTimeParseException) {
                    // Handle exception or try the next format
                }
            }

            val notificationTime: Long? = if (!formattedDate.isNullOrBlank() && formattedTime != null) {
                val dateTime = LocalDateTime.of(originalDate, formattedTime)
                dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            } else {
                null
            }
            val updatedData = HashMap<String, Any>()
            updatedData["id"] = id.toString()
            updatedData["message"] =dataClassMessage
            updatedData["time"] = formattedTime?.format(timeFormat) ?: ""
            updatedData["date"] = formattedDate
            updatedData["notificationTime"] = notificationTime ?: 0L
            databaseRef.child(id.toString()).updateChildren(updatedData)
            navController.navigate(Screen.Home.route)
        }

    BackHandler {
        onDoneClick(dataClassDate.value, dataClassTime.value)
    }


        val onBackPressed: () -> Unit = {
            onDoneClick(dataClassDate.value, dataClassTime.value)
        }

        val snackbarHostState = remember { SnackbarHostState() }
        val coroutineScope = rememberCoroutineScope()
        val onDeleteClick:(String) -> Unit = {clickedTaskId ->
            val databaseRef = database.reference.child("Task").child(uid.toString())
            val taskRef = database.reference.child("Task").child(uid.toString()).child(clickedTaskId)

            coroutineScope.launch {
                snackbarHostState.currentSnackbarData?.dismiss()
                val data = databaseRef.child(clickedTaskId).get().await().getValue(DataClass::class.java)

                if (data != null) {
                    databaseRef.child(clickedTaskId).removeValue()
                    cancelNotifications(context, clickedTaskId)
                    val snackbarResult = snackbarHostState.showSnackbar(
                        message = "TASK DELETED",
                        actionLabel = "UNDO",
                        duration = SnackbarDuration.Short
                    )
                    when (snackbarResult) {
                        SnackbarResult.Dismissed -> {
                            databaseRef.child(clickedTaskId).removeValue()
                        }
                        SnackbarResult.ActionPerformed -> {
                            taskRef.setValue(data)
                        }
                    }
                }
            }

        }
        val onMarkCompletedClick: (String) -> Unit = { clickedTaskId ->
            val taskRef = database.reference.child("Task").child(uid.toString()).child(clickedTaskId)
            val taskNewRef = database.reference.child("Task").child(uid.toString()).push()
            var completedTasksRef = database.reference.child("Task").child("CompletedTasks").child(uid.toString()).push()
            taskRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val data = snapshot.getValue(DataClass::class.java)
                    if (data != null) {
                        taskRef.removeValue()
                        completedTasksRef.setValue(data)
                        cancelNotifications(context, data.id)
                        coroutineScope.launch {
                            snackbarHostState.currentSnackbarData?.dismiss()
                            val result = snackbarHostState.showSnackbar(
                                message = "TASK COMPLETED",
                                actionLabel = "UNDO",
                                duration = SnackbarDuration.Short
                            )
                            when (result) {
                                SnackbarResult.ActionPerformed -> {
                                    //Do Something
                                    taskNewRef.setValue(data)
                                    completedTasksRef.removeValue()

                                }
                                SnackbarResult.Dismissed -> {
                                    //Do Something
                                    completedTasksRef.setValue(data)
                                }

                            }
                        }

                    }
                }
                override fun onCancelled(error: DatabaseError) {
                    Log.e("FirebaseError", "Database operation cancelled: $error")
                }
            })
        }


     /*   Dialog(onDismissRequest = onBackPressed ,
            properties = DialogProperties(
                dismissOnClickOutside = true,
                dismissOnBackPress = true,
                usePlatformDefaultWidth = false,

            )
        ){*/
           // ThemedBackground()
    BackHandler {
        onDoneClick.invoke(dataClassDate.value, dataClassTime.value)
    }
            Box(modifier = Modifier

                .fillMaxSize()
                .background(color = MaterialTheme.colors.background)
            ) {

                // Image(painter = painterResource(id = R.drawable.grid_lines), contentDescription = null)
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center){
                    Column(modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.CenterHorizontally) {
                        UpdateCircleDesign(
                            onTaskChange = { newTask ->

                                if (newTask.length <= maxValue){
                                    dataClassMessage = newTask
                                }
                            },
                            id = id.toString(),
                            openKeyboard = openKeyboard,
                            isUpdatePickerOpen = isUpdatePickerOpen,
                            dataClassMessage = dataClassMessage,
                            selectedDate =dataClassDate,
                            selectedTime = dataClassTime


                        )

                        Box(
                            modifier = Modifier
                                .padding(bottom = 40.dp)
                        ) {
                            UpdatedButtons( id = id.toString(), navController = navController, onMarkCompletedClick =onMarkCompletedClick,onDeleteClick)
                        }
                    }
                }
                CrossFloatingActionButton(onClick = {
                    onDoneClick.invoke(dataClassDate.value, dataClassTime.value)
                })


        }




}


  //  Log.d("dateValue","$selectedStringDate")



@SuppressLint("SuspiciousIndentation")
@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalComposeUiApi::class, ExperimentalAnimationApi::class)
@Composable
fun UpdateCircleDesign(
  //  navController: NavController,
    dataClassMessage: String,
    selectedDate: MutableState<String>,
    selectedTime: MutableState<String>,
    id:String,
    onTaskChange:(String) -> Unit,
    isUpdatePickerOpen:MutableState<Boolean>,
    openKeyboard:Boolean,
   // isChecked: MutableState<Boolean>,

){

    val isMessageFieldFocused = remember { mutableStateOf(false) }

    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    val focusManager = LocalFocusManager.current
    val database = FirebaseDatabase.getInstance()
    val user = FirebaseAuth.getInstance().currentUser
    val uid = user?.uid
    val databaseRef: DatabaseReference = database.reference.child("Task").child(uid.toString())
    val context = LocalContext.current
    val onDoneClick: () -> Unit = {
            val updatedData = HashMap<String, Any>()
            updatedData["id"] = id
            updatedData["message"] = dataClassMessage

            databaseRef.child(id).updateChildren(updatedData)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        // Toast.makeText(context, "Updated successfully", Toast.LENGTH_SHORT).show()

                    } else {
                        Toast.makeText(context, task.exception.toString(), Toast.LENGTH_SHORT).show()
                    }
                }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, top = 54.dp)
                .size(344.dp)
                // .offset(y = offsetY)
                // .scale(scale)
                // .alpha(opacity)
                .aspectRatio(1f)
                .clip(CircleShape)
                .background(color = MaterialTheme.colors.primary, shape = CircleShape)
                .clickable() { },
            contentAlignment = Alignment.Center
        ){
            val customTextSelectionColors = TextSelectionColors(
                handleColor = Color.Red,
                backgroundColor = Color.Red.copy(alpha = 0.4f)
            )
            Column(modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center) {
               // val messageState = remember { mutableStateOf(message.value) }
                CompositionLocalProvider(LocalTextSelectionColors provides customTextSelectionColors){
                    TextField(
                        value = dataClassMessage,
                        onValueChange = onTaskChange ,
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight()
                            .padding(start = 32.dp, end = 32.dp)
                            .focusRequester(focusRequester)
                            .onFocusChanged { focusState ->
                                if (focusState.isFocused) {
                                    isMessageFieldFocused.value = true
                                } else {
                                    keyboardController?.hide()
                                    isMessageFieldFocused.value = false
                                }
                            },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done,
                                capitalization = KeyboardCapitalization.Sentences),
                        keyboardActions = KeyboardActions(
                            onDone ={
                                keyboardController?.hideSoftwareKeyboard()
                                focusManager.clearFocus(true)
                                onDoneClick.invoke()
                            }
                        ),
                        colors = TextFieldDefaults.textFieldColors(
                            backgroundColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            cursorColor = FABRed
                        ),
                        placeholder = {
                            Text(text = "Task name",
                                modifier = Modifier
                                    .fillMaxWidth()
                                ,
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Medium,
                                fontSize = 24.sp,
                                fontFamily = interDisplayFamily,
                                color = MaterialTheme.colors.secondary.copy(alpha = 0.5f),
                                style = androidx.compose.ui.text.TextStyle(letterSpacing = 0.sp)
                            )
                        },

                        textStyle = LocalTextStyle.current.copy(
                            textAlign = TextAlign.Center,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = interDisplayFamily,
                            color = MaterialTheme.colors.secondary,
                            letterSpacing = -1.sp
                        ),

                        )
                }

                if (isMessageFieldFocused.value){
                    TextStyle(text = "${dataClassMessage.length} / 32")
                }

    Box(
        modifier = Modifier
            .wrapContentSize(Alignment.Center)
            .padding(
                top = 20.dp,
                start = 32.dp,
                end = 32.dp
            )
            .bounceClick()
            //   .background(color = SmallBox, shape = CircleShape)

            .clickable() {
                isUpdatePickerOpen.value = true
            }

            .border(
                width = 0.4.dp,
                color = MaterialTheme.colors.secondary, // Change to your desired border color
                shape = CircleShape
            )


            .padding(8.dp)


    ) {

        val formatter = DateTimeFormatter.ofPattern("EEE, d MMM")
        val formattedDate = selectedDate.value.format(formatter) ?: ""
        val dateString:String = formattedDate

        Log.d("FormattedDate","$formattedDate")
      //  val dateString:String = formattedDate
        val timeFormat = selectedTime.value.format(DateTimeFormatter.ofPattern("hh:mm a"))?.toUpperCase() ?: ""
        val timeString:String = timeFormat
        if (selectedDate.value.isNullOrEmpty() && selectedTime.value.isNullOrEmpty()){
            ThemedCalendarImage()
        }else if(selectedDate.value.isNotEmpty() && selectedTime.value.isNullOrEmpty()) {
          //  var dateString = formatDate(selectedDate = selectedDate).toString()
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                ThemedCalendarImage()
                Text(
                    text = selectedDate.value,
                    fontFamily = interDisplayFamily,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colors.secondary,
                    style = androidx.compose.ui.text.TextStyle(letterSpacing = 0.sp)
                )
            }
        }else{
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                ThemedCalendarImage()
                Text(
                    text = "${selectedDate.value}, $timeString",
                    fontFamily = interDisplayFamily,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colors.secondary,
                    style = androidx.compose.ui.text.TextStyle(letterSpacing = 0.sp)
                )
            }
        }
        if (isUpdatePickerOpen.value) {
            val pattern = "EEE, d MMM yyyy"
            val locale = Locale.ENGLISH

            val formatter = DateTimeFormatter.ofPattern(pattern, locale)
                .withZone(ZoneId.of("America/New_York"))
            val selectedDateString = selectedDate.value
            val localDate = if (selectedDateString.isEmpty()) {
                LocalDate.now()
            } else {
                try {
                    LocalDate.parse(selectedDateString, formatter)
                } catch (e: DateTimeParseException) {
                    LocalDate.now()
                }
            }
            Log.d("CheckselectedDate","$localDate")
            UpdatedCalendarAndTimePickerScreen(
                userSelectedDate = localDate,

                userSelectedTime = selectedTime.value,
                onDismiss = { isUpdatePickerOpen.value = false
                    keyboardController?.show()},
                onDateTimeSelected = { date, time ->
                    val defaultDateFormat = DateTimeFormatter.ofPattern("MM/dd/yyyy")
                    val desiredDateFormat = DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale.ENGLISH)

                    val defaultDateString = date
                    val parsedDate = LocalDate.parse(defaultDateString, defaultDateFormat)
                    val formattedDate = parsedDate.format(desiredDateFormat)

                    selectedDate.value = formattedDate
                    selectedTime.value = time
                   // Log.d("Checkdateformat","$date")
                },
                id = id,
                invokeOnDoneClick = true,
                UnMarkedDateandTime = false,
                // isChecked = isChecked,
                message = dataClassMessage
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



@SuppressLint("UnrememberedMutableState")
@Composable
fun UpdatedButtons(id: String,
                navController: NavController,
                   onMarkCompletedClick: (String) -> Unit,
                   onDeleteClick: (String) -> Unit){
    val coroutineScope = rememberCoroutineScope()
    val database = FirebaseDatabase.getInstance()
    val user = FirebaseAuth.getInstance().currentUser
    val uid = user?.uid

    var visible by remember {
        mutableStateOf(false)
    }
    LaunchedEffect(Unit) {
        visible = true // Set the visibility to true to trigger the animation
    }
    val offsetY by animateDpAsState(
        targetValue = if (visible) 0.dp else 24.dp,
        animationSpec = tween(durationMillis = 300,easing = EaseOutCirc, delayMillis = 200)
    )
    val opacity by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = keyframes {
            durationMillis = 300 // Total duration of the animation
            0.0f at 0 // Opacity becomes 0.3f after 200ms
            // Opacity becomes 0.6f after 500ms
            1f at 300

            delayMillis = 200// Opacity becomes 1f after 1000ms (end of the animation)
        }

    )
    Box(modifier = Modifier
        .wrapContentWidth()
        .height(48.dp)
        .offset(y = offsetY)
        .alpha(opacity)
        .background(color = MaterialTheme.colors.primary, shape = RoundedCornerShape(30.dp))
        ,
contentAlignment = Alignment.Center
    ) {
        Row(modifier = Modifier
            .wrapContentWidth()
            .padding(start = 24.dp, end = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.clickable(indication = null,
                interactionSource = remember { MutableInteractionSource() }) {
                onDeleteClick(id)
              //  onDismiss.invoke()
                navController.popBackStack()
            },
                verticalAlignment = Alignment.CenterVertically
            ) {
               ThemedTrashImage()
               ButtonTextWhiteTheme(text = "DELETE")
            }
            Box(
                modifier = Modifier
                    .padding(start = 12.dp)
                    .width(1.dp)
                    .fillMaxHeight()
                    .background(color = MaterialTheme.colors.background)

            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)
            , modifier = Modifier
                    .padding(12.dp)
                    .clickable(indication = null,
                        interactionSource = remember { MutableInteractionSource() }) {
                        coroutineScope.launch {
                            onMarkCompletedClick(id)
                            navController.popBackStack()
                        }
                    },
            verticalAlignment = Alignment.CenterVertically) {
                ThemedSquareImage(modifier = Modifier)
                ButtonTextWhiteTheme(text = "MARK COMPLETED")
            }
        }

    }
}

/*class NotificationActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Extract the data from the intent
        val taskId = intent.getStringExtra("taskId")

        setContent {
            // Display the UpdateTaskScreen Composable
            UpdateTaskScreenContent(taskId)
        }
    }
}*/
@Composable
fun ThemedTrashImage() {
    val isDarkTheme = isSystemInDarkTheme()
    val imageRes = if (isDarkTheme) {
        R.drawable.dark_trash_delete
    } else {
        R.drawable.light_trash_delete
    }

    Image(
        painter = painterResource(id = imageRes),
        contentDescription = null,
    )
}
@Composable
fun ButtonTextWhiteTheme(text:String){
    Text(
        text = text,
        fontFamily = interDisplayFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        color = MaterialTheme.colors.secondary,
        style = androidx.compose.ui.text.TextStyle(letterSpacing = 1.sp)
    )
}
@Composable
fun ButtonTextDarkTheme(text:String){
    Text(
        text = text,
        fontFamily = interDisplayFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        color = MaterialTheme.colors.primary,
        style = androidx.compose.ui.text.TextStyle(letterSpacing = 1.sp)
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
                .bounceClick()
                ,
            elevation = FloatingActionButtonDefaults.elevation(0.dp),
            onClick = {onClick.invoke()},
            shape = CircleShape,
            backgroundColor = MaterialTheme.colors.primary

            ) {
            ThemedCrossImage()
        }
    }
}
@Composable
fun ThemedCrossImage() {
    val isDarkTheme = isSystemInDarkTheme()
    val imageRes = if (isDarkTheme) {
        R.drawable.dark_cross_icon
    } else {
        R.drawable.light_cross_icon
    }

    Image(
        painter = painterResource(id = imageRes),
        contentDescription = null,
    )
}