package com.firstyogi.dothing

import android.annotation.SuppressLint
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.EaseOutBack
import androidx.compose.animation.core.EaseOutCirc
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.toUpperCase
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController
import com.firstyogi.ui.theme.FABRed
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.*

@SuppressLint("UnrememberedMutableState", "UnusedMaterialScaffoldPaddingParameter")
@OptIn(ExperimentalComposeUiApi::class, ExperimentalSharedTransitionApi::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun UnMarkCompletedTaskScreen(
    snackbarHostState:SnackbarHostState,
    id:String?,
    openKeyboard: Boolean,
    isChecked: MutableState<Boolean>,
    animatedVisibilityScope: AnimatedVisibilityScope,
    sharedTransitionScope: SharedTransitionScope,
    navController: NavController
    ) {

    val context = LocalContext.current
   Log.d("unmarkcompletedid","$id")
    val database = FirebaseDatabase.getInstance()
    val user = FirebaseAuth.getInstance().currentUser
    val uid = user?.uid
    var completedTasksRef = database.reference.child("Task").child("CompletedTasks").child(uid.toString())

    var data by remember { mutableStateOf<DataClass?>(null) }
    var dataClassMessage = remember { mutableStateOf("") }
    var dataClassDate= remember { mutableStateOf("") }
    var dataClassTime = remember{ mutableStateOf("") }
    val repeatableOption = remember {
        mutableStateOf("")
    }
    val scaffoldState = rememberScaffoldState()
    val coroutineScope = rememberCoroutineScope()
    val maxValue = 32
    var isPickerOpen = remember { mutableStateOf(false) }
    val blurEffectBackground by animateDpAsState(targetValue = when{
        isPickerOpen.value -> 10.dp
        else -> 0.dp
    })
    var unmarkCompletevisible = remember {
        mutableStateOf(false)
    }
    LaunchedEffect(Unit) {
        unmarkCompletevisible.value = true // Set the visibility to true to trigger the animation
    }
    DisposableEffect(Unit) {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                Log.d("onDataChange", "Data snapshot received")
                val map = snapshot.child(id.toString()).value as? Map<*, *>
                val selectedData = map?.let {
                    DataClass(
                        id = it["id"] as? String?: "",
                        message = it["message"] as? String ?: "",
                        time = it["time"] as? String ?: "",
                        date = it["date"] as? String ?: "",
                        notificationTime = when (val nt = it["notificationTime"]) {
                            is Long -> nt
                            is String -> nt.toLongOrNull() ?: 0L
                            else -> 0L
                        },
                        repeatedTaskTime = it["repeatedTaskTime"] as? String ?: "",
                        nextDueDate = when (val nd = it["nextDueDate"]) {
                            is Long -> nd
                            is String -> nd.toLongOrNull()
                            else -> null
                        },
                        //nextDueDateForCompletedTask = it["nextDueDateForCompletedTask"] as? String ?: "",
                        formatedDateForWidget = it["formatedDateForWidget"] as? String ?: ""
                    )
                }
                if (selectedData != null) {
                    data = selectedData
                    val originalDateFormat = DateTimeFormatter.ofPattern("MM/dd/yyyy")
                    val desiredDateFormat = DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale.ENGLISH)

                    if (!selectedData.date.isNullOrBlank()) {
                        try {
                            val parsedDate = LocalDate.parse(selectedData.date, originalDateFormat)
                            dataClassDate.value = parsedDate.format(desiredDateFormat)
                        } catch (e: DateTimeParseException) {
                            Log.e("DateParsingError", "Error parsing date: ${selectedData.date}", e)
                        }
                    } else {
                        dataClassDate.value = ""
                    }
                    dataClassMessage.value = selectedData.message ?: ""
                    dataClassTime.value = selectedData.time ?: ""
                    repeatableOption.value = selectedData.repeatedTaskTime ?: ""
                    Log.d("unmarkcompleteddataclassname", "${selectedData.message}")
                } else {
                    Log.e("onDataChange", "Selected data is null for ID: $id")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseError", "Database operation cancelled: $error")
            }
        }

        Log.d("DisposableEffect", "Adding Firebase listener")
        completedTasksRef.addListenerForSingleValueEvent(listener)

        onDispose {
            Log.d("DisposableEffect", "Removing Firebase listener")
            completedTasksRef.removeEventListener(listener)
        }
    }
    val onDeleteClick:(String)  -> Unit = {clickedTaskId ->
        var completedTasksRef = database.reference.child("Task").child("CompletedTasks").child(uid.toString())
      /*  coroutineScope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            val data = completedTasksRef.child(clickedTaskId).get().await().getValue(DataClass::class.java)
            if (data != null) {
                completedTasksRef.child(clickedTaskId).removeValue()
                val snackbarResult = snackbarHostState.showSnackbar(
                    message = "TASK DELETED",
                    actionLabel = "UNDO",
                    duration = SnackbarDuration.Short
                )
                when (snackbarResult) {
                    SnackbarResult.Dismissed -> {
                        completedTasksRef.child(clickedTaskId).removeValue()
                    }
                    SnackbarResult.ActionPerformed -> {
                        completedTasksRef.child(clickedTaskId).setValue(data)
                    }
                }
            }

        }*/
        navController.previousBackStackEntry?.savedStateHandle?.apply {
            set("snackbarDeleteMessage","Task deleted")
            set("taskId",clickedTaskId)

        }
        navController.popBackStack()
        unmarkCompletevisible.value = false

    }
    val onUnMarkCompletedClick: (String) -> Unit = { clickedTaskId ->
        val taskRef = database.reference.child("Task").child(uid.toString()).child(clickedTaskId)
        val completedTasksRef = database.reference.child("Task").child("CompletedTasks").child(uid.toString()).child(clickedTaskId)
        val completedNewTaskRef = database.reference.child("Task").child("CompletedTasks").child(uid.toString()).child(clickedTaskId)
        completedTasksRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val data = snapshot.getValue(DataClass::class.java)
                if (data != null) {
                    val originalDate = data.date
                    //  val userSelectedFirstDate = data.userSelectedFirstDate
                    // data.id = taskRef.key?:""
                    // data.date = userSelectedFirstDate
                    Log.d("unmarkcompletid","${completedNewTaskRef.key}")
                    completedTasksRef.removeValue()
                    taskRef.setValue(data)
                    scheduleNotification(
                        context,
                        data.notificationTime!!,
                        data.id,
                        data.message ?: "",
                        false,
                        data.repeatedTaskTime!!
                    )
                 /*   coroutineScope.launch {
                       // snackbarHostState.currentSnackbarData?.dismiss()

                        val snackbarResult = snackbarHostState.showSnackbar(
                            message = "TASK MARKED UNCOMPLETED",
                            actionLabel = "UNDO",
                            duration = SnackbarDuration.Short
                        )
                        when (snackbarResult) {
                            SnackbarResult.Dismissed -> {
                                taskRef.setValue(data)
                            }
                            SnackbarResult.ActionPerformed -> {
                                //data.date = data.nextDueDate
                                completedNewTaskRef.setValue(data)
                                taskRef.removeValue()
                                cancelNotification(context, data.id)

                            }
                        }
                    }*/

                    navController.previousBackStackEntry?.savedStateHandle?.apply {
                        set("snackbarUncompleteMessage","Task marked uncompleted")
                        set("taskId",clickedTaskId)

                    }

                    navController.popBackStack()
                    unmarkCompletevisible.value = false
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseError", "Database operation cancelled: $error")
            }
        })
    }
Log.d("UnMarkCompletedPageMessage","$dataClassMessage")

    Box(modifier = Modifier
        .blur(radius = blurEffectBackground)
        .fillMaxSize()
        .background(color = MaterialTheme.colors.background)
    ) {
        //ThemedGridImage(modifier = Modifier)
        Box(modifier = Modifier
            .fillMaxSize()
            .clickable(indication = null,
                interactionSource = remember { MutableInteractionSource() }) {
                unmarkCompletevisible.value = false
                navController.popBackStack()
            }
            , contentAlignment = Alignment.Center){
            Column(modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally) {
                UnMarkCompletedCircleDesign(
                    initialSelectedate = dataClassDate ,
                    initialSelectedtime = dataClassTime ,
                    message = dataClassMessage,
                    onTaskChange = { newTask ->
                        if (newTask.length <= maxValue){
                            dataClassMessage.value = newTask
                        }
                    },
                    id = id.toString(),
                    openKeyboard = openKeyboard,
                    isPickerOpen = isPickerOpen,
                    isChecked = isChecked,
                    repeatableOption = repeatableOption,
                    animatedVisibilityScope = animatedVisibilityScope,
                    sharedTransitionScope = sharedTransitionScope
                )

                Box(
                    modifier = Modifier
                        .padding(bottom = 40.dp)

                ) {
                    UnMarkCompletedButtons(
                        id = id.toString(),
                        navController = navController,
                        onDeleteClick ,onUnMarkCompletedClick,
                        visible = unmarkCompletevisible)
                }

            }
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 56.dp),
            contentAlignment = Alignment.TopEnd
        ){
            CrossFloatingActionButton(
                onClick = {
                    unmarkCompletevisible.value = false
                navController.popBackStack()
            },
                visible = unmarkCompletevisible
                )
        }

    }



    }
@SuppressLint("UnrememberedMutableState")
@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalComposeUiApi::class, ExperimentalSharedTransitionApi::class)
@Composable
fun UnMarkCompletedCircleDesign(
    message: MutableState<String>,
    initialSelectedate: MutableState<String>,
    initialSelectedtime: MutableState<String>,
    id:String,
    onTaskChange:(String) -> Unit,
    isPickerOpen: MutableState<Boolean>,
    openKeyboard:Boolean,
    isChecked: MutableState<Boolean>,
    repeatableOption: MutableState<String>,
    animatedVisibilityScope:AnimatedVisibilityScope,
    sharedTransitionScope: SharedTransitionScope

    ){
    var isClickable = remember { mutableStateOf(true) }
    val isMessageFieldFocused = remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
Log.d("initialSelecteddate","$initialSelectedate")
    val focusManager = LocalFocusManager.current
    val database = FirebaseDatabase.getInstance()
    val user = FirebaseAuth.getInstance().currentUser
    val uid = user?.uid
    val context = LocalContext.current
    val completedTasksRef = database.reference.child("Task").child("CompletedTasks").child(uid.toString())
    val onDoneClick: () -> Unit = {
        val updatedData = HashMap<String, Any>()
        updatedData["id"] = id
        updatedData["message"] = message.value.trim()
        completedTasksRef.child(id).updateChildren(updatedData)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    //Toast.makeText(context, "Updated successfully", Toast.LENGTH_SHORT).show()

                } else {
                    Toast.makeText(context, task.exception.toString(), Toast.LENGTH_SHORT).show()
                }
            }
    }
    var visible by remember {
        mutableStateOf(false)
    }
    LaunchedEffect(Unit) {
        visible = true
    }
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = spring(
            dampingRatio = 0.7f,
            stiffness = Spring.StiffnessVeryLow
        )
    )
    val offsetY by animateDpAsState(
        targetValue = if (visible) 0.dp else 400.dp,
        animationSpec = spring(
            dampingRatio = 0.45f,
            stiffness = Spring.StiffnessMediumLow
        )
    )
    with(sharedTransitionScope){
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, top = 88.dp)
                .renderInSharedTransitionScopeOverlay(
                    zIndexInOverlay = 1f
                )
               // .size(344.dp)
                .sharedBounds(
                    rememberSharedContentState(key = "boundsUnMark-$id"),
                    animatedVisibilityScope = animatedVisibilityScope,
                   // enter = fadeIn(tween(durationMillis = 300, easing = EaseOutBack )),
                  //  exit = fadeOut(tween(durationMillis = 300, easing = EaseOutBack )),
                    boundsTransform = { initialRect, targetRect ->
                        spring(
                            dampingRatio = 0.8f,
                            stiffness = 380f
                        )

                    },
                    placeHolderSize = SharedTransitionScope.PlaceHolderSize.animatedSize
                )
                 // .offset(y = offsetY)
                 //.scale(scale)
                .aspectRatio(1f)
                .clip(CircleShape)
                .background(color = MaterialTheme.colors.secondary, shape = CircleShape)
                .clickable(indication = null,
                    interactionSource = remember { MutableInteractionSource() }) { },

            contentAlignment = Alignment.Center
        ){
            val customTextSelectionColors = TextSelectionColors(
                handleColor = Color.Red,
                backgroundColor = Color.Red.copy(alpha = 0.4f)
            )
            var isClicked = remember { mutableStateOf(false) }
            Column(modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center) {
                val messageState = remember { mutableStateOf(message.value) }
                CompositionLocalProvider(LocalTextSelectionColors provides customTextSelectionColors){
                    TextField(
                        value = message.value,
                        onValueChange = onTaskChange ,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(90.dp)
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
                        maxLines = 3,

                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done,
                            capitalization = KeyboardCapitalization.Sentences),
                        keyboardActions = KeyboardActions(
                            onDone ={
                                keyboardController?.hide()
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
                                    .fillMaxWidth(),
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Medium,
                                fontSize = 24.sp,
                                color = MaterialTheme.colors.primary.copy(alpha = 0.5f),
                                fontFamily = interDisplayFamily,
                                style = androidx.compose.ui.text.TextStyle(letterSpacing = 0.sp)
                            )
                        },

                        textStyle = LocalTextStyle.current.copy(
                            textAlign = TextAlign.Center,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = interDisplayFamily,
                            color = MaterialTheme.colors.primary,
                            textDecoration = TextDecoration.LineThrough,
                        ),

                        )
                }

                if (isMessageFieldFocused.value){
                    TextStyle(text = "${message.value.length} / 32")
                }
                Box(
                    modifier = Modifier
                       // .wrapContentSize(Alignment.Center)
                        .padding(
                            top = 20.dp,start = 8.dp,end = 8.dp
                        )
                      //  .bounceClick()
                      /*  .clickable(indication = null,
                            interactionSource = remember { MutableInteractionSource() }) {
                            isPickerOpen.value = true
                        }*/
                        .border(
                            width = 1.5.dp,
                            color = MaterialTheme.colors.primary.copy(alpha = 0.5f), // Change to your desired border color
                            shape = CircleShape
                        )
                        .padding(8.dp)
                ) {
                    val formatter = DateTimeFormatter.ofPattern("EEE, d MMM")
                    val formattedDate = initialSelectedate.value.format(formatter) ?: ""
                    val dateString:String = formattedDate
                    val timeFormat = initialSelectedtime.value.format(DateTimeFormatter.ofPattern("hh:mm a"))?.toUpperCase() ?: ""
                    val timeString:String = timeFormat
                    Log.d("timestring","$timeString")
                    if (initialSelectedate.value.isNullOrEmpty() && initialSelectedtime.value.isNullOrEmpty()){
                        ThemedCalendarImage(modifier = Modifier.alpha(0.5f))
                    }else if(initialSelectedate.value.isNotEmpty() && initialSelectedtime.value.isNullOrEmpty()) {
                        Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            ThemedCalendarImage(modifier = Modifier.alpha(0.5f))
                            Text(
                                text = dateString,
                                fontFamily = interDisplayFamily,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colors.primary.copy(alpha = 0.5f),
                                style = androidx.compose.ui.text.TextStyle(letterSpacing = -0.sp)
                            )
                        }
                    }else{

                        Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            ThemedCalendarImage(modifier = Modifier)
                            Text(
                                text = "${dateString}, ${timeString}",
                                fontFamily = interDisplayFamily,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color =  MaterialTheme.colors.primary.copy(alpha = 0.5f),
                                style = androidx.compose.ui.text.TextStyle(letterSpacing = 0.sp)
                            )

                        }
                    }
                    if (isPickerOpen.value) {
                        val pattern = "EEE, d MMM yyyy"
                        val locale = Locale.ENGLISH

                        val formatter = DateTimeFormatter.ofPattern(pattern, locale)
                            .withZone(ZoneId.of("America/New_York"))
                        val selectedDate = initialSelectedate.value
                        val localDate = if (selectedDate.isEmpty()) {
                            LocalDate.now()
                        } else {
                            try {
                                LocalDate.parse(selectedDate, formatter)
                            } catch (e: DateTimeParseException) {
                                LocalDate.now()
                            }
                        }
                        Log.d("initialSelectedDate","$localDate")
                        UpdatedCalendarAndTimePickerScreen(
                            userSelectedDate = localDate,
                            userSelectedTime = initialSelectedtime.value,
                            onDismiss = { isPickerOpen.value = false
                                keyboardController?.show()},
                            onDateTimeSelected = { date, time ->
                                val defaultDateFormat = DateTimeFormatter.ofPattern("MM/dd/yyyy")
                                val desiredDateFormat = DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale.ENGLISH)

                                val defaultDateString = date
                                val parsedDate = LocalDate.parse(defaultDateString, defaultDateFormat)
                                val formattedDate = parsedDate.format(desiredDateFormat)

                                initialSelectedate.value = formattedDate
                                initialSelectedtime.value = time
                            },
                            id = id,
                            invokeOnDoneClick = false,

                            isChecked = isChecked,
                            message = message,
                            repeatableOption = repeatableOption,
                            isClicked = isClicked,


                            )

                    }
                }

                RepeatedTaskBoxImplement(repeatableOption = repeatableOption,
                    isPickerOpen = isPickerOpen,
                    isClicked = mutableStateOf(false),
                    id = id,
                    addtaskCrossClick = false,
                    unMarkCompletedCrossClick = false,
                    color = MaterialTheme.colors.primary.copy(alpha = 0.5f),
                    modifier = Modifier,
                    isClickable = isClickable)
            }
        }
    }
    LaunchedEffect(Unit) {

            isClickable.value = false

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
fun UnMarkCompletedButtons(id: String,
                           navController: NavController,
                           onDeleteClick : (String) -> Unit,
                           onUnMarkCompletedClick:(String) -> Unit,
                           visible:MutableState<Boolean>)
{
    val coroutineScope = rememberCoroutineScope()
    val offsetY by animateDpAsState(
        targetValue = if (visible.value) 0.dp else 24.dp,
        animationSpec = tween(durationMillis = 300,easing = EaseOutCirc, delayMillis = 200)
    )
    val opacity by animateFloatAsState(
        targetValue = if (visible.value) 1f else 0f,
        animationSpec = keyframes {
            durationMillis = 300
            0.0f at 0
            1f at 300
            delayMillis = 200
        }

    )
    AnimatedVisibility(
        visible = visible.value,

        enter = slideInVertically(
            initialOffsetY = { 96 }, // Starts off-screen at the top
            animationSpec = spring(
                dampingRatio = 0.6f,
                stiffness = Spring.StiffnessVeryLow

            )
        ),

        exit = slideOutVertically(
            targetOffsetY = { 96 }, // Exits off-screen at the bottom
            animationSpec = tween(
                durationMillis = 500,
                easing = EaseOutCirc,
            )
        ) + fadeOut( // Combine slide and opacity for exit
            animationSpec = tween(
                durationMillis = 300,
                easing = EaseOutCirc,

                )
        )
    ){
        Box(modifier = Modifier
            .wrapContentWidth()
            .padding(start = 24.dp,end = 24.dp)
            .height(48.dp)
           // .offset(y = offsetY)
           // .alpha(opacity)
            .background(color = MaterialTheme.colors.secondary, shape = RoundedCornerShape(30.dp)),
            contentAlignment = Alignment.Center
        ) {
            Row(modifier = Modifier
                //.fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.clickable(indication = null,
                        interactionSource = remember { MutableInteractionSource() }) {

                        onDeleteClick(id)
                        // navController.popBackStack()


                    },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ThemedTrashImage()
                    ButtonTextWhiteTheme(text = "Delete",color = MaterialTheme.colors.primary,modifier = Modifier)
                }
                Box(
                    modifier = Modifier
                        .padding(start = 12.dp)
                        .width(1.dp)
                        .fillMaxHeight()
                        .background(color = MaterialTheme.colors.background)

                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .padding(start = 12.dp)
                        .clickable(indication = null,
                            interactionSource = remember { MutableInteractionSource() }) {

                            onUnMarkCompletedClick(id)
                            // navController.popBackStack()


                        },
                    verticalAlignment = Alignment.CenterVertically) {
                    ThemedSquareImage(modifier = Modifier)
                    ButtonTextWhiteTheme(text = "Mark Uncomplete",color = MaterialTheme.colors.primary,modifier = Modifier)
                }
            }

        }
    }

}


