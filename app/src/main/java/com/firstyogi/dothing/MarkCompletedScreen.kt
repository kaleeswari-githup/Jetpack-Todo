package com.firstyogi.dothing

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.firstyogi.ui.theme.*
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.*

@SuppressLint("UnusedMaterialScaffoldPaddingParameter", "SuspiciousIndentation")
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun MarkCompletedScreen(
    navController:NavController,
    isChecked:MutableState<Boolean>,
    ){
    val context = LocalContext.current
    val sharedPreferences = context.getSharedPreferences("MyAppSettings", Context.MODE_PRIVATE)
    val selectedMarkedItemId = remember { mutableStateOf("") }
    val database = FirebaseDatabase.getInstance()
    val user = FirebaseAuth.getInstance().currentUser
    val uid = user?.uid
    var completedTasksRef = database.reference.child("Task").child("CompletedTasks").child(uid.toString())
    val completedTasksCountState = remember { mutableStateOf(0) }
    val scaffoldState = rememberScaffoldState()
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val valueEventListener = object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            completedTasksCountState.value = snapshot.childrenCount.toInt()
        }

        override fun onCancelled(error: DatabaseError) {
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            scaffoldState.snackbarHostState.currentSnackbarData?.dismiss()
        }
    }
    DisposableEffect(Unit) {
        completedTasksRef.addValueEventListener(valueEventListener)

        onDispose {
            completedTasksRef.removeEventListener(valueEventListener)
        }
    }
    val onDeleteClick:(String)  -> Unit = {clickedTaskId ->
        var completedTasksRef = database.reference.child("Task").child("CompletedTasks").child(uid.toString())
        coroutineScope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            val data = completedTasksRef.child(clickedTaskId).get().await().getValue(DataClass::class.java)
            if (data != null) {
                completedTasksRef.child(clickedTaskId).removeValue()
                val snackbarResult = snackbarHostState.showSnackbar(
                    message = "Task deleted",
                    actionLabel = "Undo",
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
        }

    }
    val onUnMarkCompletedClick: (String) -> Unit = { clickedTaskId ->
        val taskRef = database.reference.child("Task").child(uid.toString()).child(clickedTaskId)
        val completedTasksRef = database.reference.child("Task").child("CompletedTasks").child(uid.toString()).child(clickedTaskId)
        val completedNewTaskRef = database.reference.child("Task").child("CompletedTasks").child(uid.toString()).push()
        completedTasksRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val data = snapshot.getValue(DataClass::class.java)
                if (data != null) {
                    completedTasksRef.removeValue()
                    taskRef.setValue(data)
                    coroutineScope.launch {
                        snackbarHostState.currentSnackbarData?.dismiss()
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
                                completedNewTaskRef.setValue(data)
                                taskRef.removeValue()
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

    fun saveIsChecked(isChecked: Boolean) {
        val editor = sharedPreferences.edit()
        editor.putBoolean("isChecked", isChecked)
        editor.apply()
    }
    val blurEffectBackground by animateDpAsState(targetValue = when{
        selectedMarkedItemId.value.isNotEmpty() -> 25.dp
        else -> 0.dp
    }
    )
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
                visible = true // Set the visibility to true to trigger the animation
            }
    Box(modifier = Modifier
                .blur(radius = blurEffectBackground)
                .fillMaxSize()
                .background(color = MaterialTheme.colors.background)
                .clickable(indication = null,
                    interactionSource = remember { MutableInteractionSource() }) { navController.popBackStack() },) {
ThemedGridImage()
                LazyColumn(modifier = Modifier.fillMaxSize(),
                    ) {
                    item{
                        val offsetY by animateDpAsState(
                            targetValue = if (visible) 0.dp else 32.dp,
                            animationSpec = tween(
                                durationMillis = 300,
                                delayMillis = 0,
                                easing = EaseOutCirc
                            ),

                        )
                        val opacity by animateFloatAsState(
                            targetValue = if (visible) 1f else 0f,
                            animationSpec = keyframes {
                                durationMillis = 300 // Total duration of the animation
                                0.3f at 100 // Opacity becomes 0.3f after 200ms
                                0.6f at 200 // Opacity becomes 0.6f after 500ms
                                1f at 300 // Opacity becomes 1f after 1000ms (end of the animation)
                            }
                        )
                        Box(modifier = Modifier
                            .fillMaxWidth()
                            .offset(y = offsetY)
                            .alpha(opacity)
                            .padding(start = 24.dp, end = 24.dp, top = 120.dp)
                            .background(
                                color = MaterialTheme.colors.primary,
                                shape = RoundedCornerShape(32.dp)
                            )
                            .clickable(indication = null,
                                interactionSource = remember { MutableInteractionSource() }) { },

                            ) {
                            Row(modifier = Modifier.padding(start = 24.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(modifier = Modifier
                                    .size(48.dp)
                                    .background(
                                        shape = CircleShape,
                                        color = MaterialTheme.colors.primary
                                    ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val firebaseAuth = FirebaseAuth.getInstance()
                                    val user = firebaseAuth.currentUser
                                    val photoUrl = user?.photoUrl
                                    val initials = user?.email?.take(1)?.toUpperCase()
                                    if (photoUrl != null) {
                                        AsyncImage(
                                            model = ImageRequest.Builder(LocalContext.current)
                                                .data(photoUrl)
                                                .build(),
                                            contentDescription = "Profile picture",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(CircleShape)
                                        )
                                    }else{
                                        Text(
                                            text = initials ?: "",
                                            color = Color.White,
                                            fontFamily = interDisplayFamily,
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                }
                                Column(modifier = Modifier.padding(start = 16.dp,top = 28.dp, bottom = 28.dp)) {
                                    ButtonTextWhiteTheme(text = ("${user?.displayName}").uppercase(),color = MaterialTheme.colors.secondary)
                                    Spacer(modifier = Modifier.padding(top = 4.dp))
                                    Text(
                                        text = if (user?.email?.length ?: 0 > 32) user?.email?.substring(0, 32) + "..." else user?.email.orEmpty(),
                                        fontFamily = interDisplayFamily,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Normal,
                                        color = MaterialTheme.colors.secondary,
                                        style = androidx.compose.ui.text.TextStyle(letterSpacing = 0.sp)
                                    )
                                }
                            }

                        }
                    }
                   item {
                       val offsetY by animateDpAsState(
                           targetValue = if (visible) 0.dp else 32.dp,
                           animationSpec = tween(durationMillis = 300, delayMillis = 100,easing = EaseOutCirc)
                       )
                       val opacity by animateFloatAsState(
                           targetValue = if (visible) 1f else 0f,
                           animationSpec = keyframes {
                               durationMillis = 300 // Total duration of the animation
                               0.3f at 100 // Opacity becomes 0.3f after 200ms
                               0.6f at 200 // Opacity becomes 0.6f after 500ms
                               1f at 300

                               delayMillis = 100
                           }
                       )
                       Box(modifier = Modifier
                           .fillMaxWidth()

                           .padding(start = 24.dp, end = 24.dp, top = 8.dp)
                           .offset(y = offsetY)
                           .alpha(opacity)
                           .background(
                               color = MaterialTheme.colors.primary,
                               shape = RoundedCornerShape(32.dp)
                           )
                           .clickable(indication = null,
                               interactionSource = remember { MutableInteractionSource() }) { },
                           contentAlignment = Alignment.Center) {
                           Column(modifier = Modifier,
                               verticalArrangement = Arrangement.Center,
                               horizontalAlignment = Alignment.CenterHorizontally
                           ) {
                               val completedTasksCount = completedTasksCountState.value

                               Spacer(modifier = Modifier.padding(top = 24.dp))
                               ButtonTextWhiteTheme(text = ("Completed ($completedTasksCount)").uppercase(),color = MaterialTheme.colors.secondary)
                               Spacer(modifier = Modifier.padding(top = 12.dp))
                               Box(
                                       modifier = Modifier
                                           .fillMaxWidth()
                                           .height(218.dp)
                                           .padding(start = 24.dp, end = 24.dp)
                                           .background(
                                               color = MaterialTheme.colors.background,
                                               shape = RoundedCornerShape(24.dp)
                                           ),
                                       contentAlignment = Alignment.Center
                                   ) {
                                       if (completedTasksCount > 0) {
                                       LazyRowCompletedTask(
                                            onDeleteClick, onUnMarkCompletedClick, selectedMarkedItemId, isChecked
                                       )
                                   }
                                       else {
                                           Text(
                                               text = ("No completed tasks").uppercase(),
                                               fontFamily = interDisplayFamily,
                                               fontSize = 13.sp,
                                               fontWeight = FontWeight.Medium,
                                               color = MaterialTheme.colors.secondary.copy(alpha = 0.50f),
                                             //  modifier = Modifier.padding(top = 24.dp)
                                           )
                                       }


                               }

                               Spacer(modifier = Modifier.padding(top = 24.dp))
                           }
                       }
                   }
                    item {
                        val offsetY by animateDpAsState(
                            targetValue = if (visible) 0.dp else 32.dp,
                            animationSpec = tween(durationMillis = 300, delayMillis = 200,easing =  EaseOutCirc)
                        )
                        val opacity by animateFloatAsState(
                            targetValue = if (visible) 1f else 0f,
                            animationSpec = keyframes {
                                durationMillis = 300 // Total duration of the animation
                                0.3f at 100 // Opacity becomes 0.3f after 200ms
                                0.6f at 200 // Opacity becomes 0.6f after 500ms
                                1f at 300

                                delayMillis = 200
                            }
                        )
                        Box(modifier = Modifier
                            .fillMaxWidth()

                            .offset(y = offsetY)
                            .alpha(opacity)
                            .height(72.dp)
                            .padding(start = 24.dp, end = 24.dp, top = 8.dp)
                            .background(
                                color = MaterialTheme.colors.primary,
                                shape = RoundedCornerShape(24.dp)
                            )
                            .clickable(indication = null,
                                interactionSource = remember { MutableInteractionSource() }) {
                                isChecked.value = !isChecked.value
                                saveIsChecked(isChecked.value)
                                if (isChecked.value) {
                                    coroutineScope.launch(Dispatchers.IO) {
                                        val mMediaPlayer =
                                            MediaPlayer.create(context, R.raw.toggle_sound)
                                        mMediaPlayer.start()
                                        delay(mMediaPlayer.duration.toLong())
                                        mMediaPlayer.release()
                                    }
                                }
                                Vibration(context)
                            },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(modifier = Modifier
                                .fillMaxSize()
                                .padding(start = 24.dp, end = 24.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween) {
                                Box() {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        ThemedSoundIcon()
                                        Spacer(modifier = Modifier.padding(start = 8.dp))
                                        ButtonTextWhiteTheme(text = ("Sound").uppercase(),color = MaterialTheme.colors.secondary)

                                    }

                                }

                                Box(modifier = Modifier) {
                                        Box(
                                            modifier = Modifier
                                                .clickable(indication = null,
                                                    interactionSource = remember { MutableInteractionSource() }) {
                                                    isChecked.value = !isChecked.value
                                                    saveIsChecked(isChecked.value)
                                                    if(isChecked.value){
                                                        coroutineScope.launch(Dispatchers.IO) {
                                                            val mMediaPlayer = MediaPlayer.create(context, R.raw.toggle_sound)
                                                            mMediaPlayer.start()
                                                            delay(mMediaPlayer.duration.toLong())
                                                            mMediaPlayer.release()
                                                        }
                                                    }


                                                    Vibration(context)
                                                }
                                        ) {
                                        Box(
                                            modifier = Modifier
                                                .size(48.dp, 28.dp)
                                                .background(
                                                    if (isChecked.value) MaterialTheme.colors.secondary else MaterialTheme.colors.background,
                                                    shape = CircleShape
                                                )
                                                , contentAlignment = Alignment.Center
                                        ) {

                                                Spacer(
                                                    modifier = Modifier
                                                        .padding(start = 4.dp, end = 4.dp)
                                                        .align(if (isChecked.value) Alignment.CenterEnd else Alignment.CenterStart)
                                                        .size(20.dp)
                                                        .background(
                                                            MaterialTheme.colors.primary,
                                                            CircleShape
                                                        )
                                                ) }
                                    }
                                }

                            }

                        }
                    }
                  item {
                      val offsetY by animateDpAsState(
                          targetValue = if (visible) 0.dp else 32.dp,
                          animationSpec = tween(durationMillis = 300, delayMillis = 300,easing = EaseOutCirc)
                      )
                      val opacity by animateFloatAsState(
                          targetValue = if (visible) 1f else 0f,
                          animationSpec = keyframes {
                              durationMillis = 300 // Total duration of the animation
                              0.3f at 100 // Opacity becomes 0.3f after 200ms
                              0.6f at 200 // Opacity becomes 0.6f after 500ms
                              1f at 300

                              delayMillis = 300
                          }
                      )
                      Box(modifier = Modifier
                          .fillMaxWidth()

                          .offset(y = offsetY)
                          .alpha(opacity)
                          .height(72.dp)
                          .padding(start = 24.dp, end = 24.dp, top = 8.dp)
                          .background(
                              color = MaterialTheme.colors.primary,
                              shape = RoundedCornerShape(24.dp)
                          )
                          .clickable(indication = null,

                              interactionSource = remember { MutableInteractionSource() }) {
                              val user = FirebaseAuth.getInstance().currentUser
                              val currentuserId = user?.uid
                              if (currentuserId != null) {
                                  cancelAllNotifications(context, currentuserId)
                              }
                              val auth = FirebaseAuth.getInstance()
                              // Sign out from Firebase
                              auth.signOut()
                              val googleSignInClient = GoogleSignIn.getClient(
                                  context,
                                  GoogleSignInOptions.DEFAULT_SIGN_IN
                              )
                              // Sign out from Google
                              googleSignInClient
                                  .signOut()
                                  .addOnCompleteListener {
                                      // Optional: Perform any additional actions after sign out
                                      val intent = Intent(context, SigninActivity::class.java)
                                      context.startActivity(intent)
                                      (context as Activity).overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                                      // onDismiss.invoke()
                                  }
                          },
                          contentAlignment = Alignment.Center
                      ) {
                          Row(modifier = Modifier
                              .fillMaxSize()
                              .padding(start = 24.dp, end = 24.dp),
                              verticalAlignment = Alignment.CenterVertically,
                              horizontalArrangement = Arrangement.SpaceBetween) {
                              Box() {
                                  Row(verticalAlignment = Alignment.CenterVertically) {
                                      ThemedLogoutIcon()
                                      Spacer(modifier = Modifier.padding(start = 8.dp))
                                      ButtonTextWhiteTheme(text = ("Log Out").uppercase(),color = MaterialTheme.colors.secondary)
                                  }

                              }

                              Box(modifier = Modifier

                                  .align(Alignment.CenterVertically)
                              ) {
                                  ThemedRightIcon()
                              }

                          }

                      }
                  }

                }

                CrossFloatingActionButton {
                    navController.popBackStack()

                }
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier.align(Alignment.BottomCenter),
                    snackbar = { CustomSnackbar(it)}
                )
    }
}
@Composable
fun ThemedRightIcon() {
    val isDarkTheme = isSystemInDarkTheme()
    val imageRes = if (isDarkTheme) {
        R.drawable.dark_right_icon
    } else {
        R.drawable.light_right_icon
    }

    Image(
        painter = painterResource(id = imageRes),
        contentDescription = null,

        )
}
@Composable
fun ThemedSoundIcon() {
    val isDarkTheme = isSystemInDarkTheme()
    val imageRes = if (isDarkTheme) {
        R.drawable.dark_sound_icon
    } else {
        R.drawable.light_sound_icon
    }

    Image(
        painter = painterResource(id = imageRes),
        contentDescription = null,

        )
}
@Composable
fun ThemedLogoutIcon() {
    val isDarkTheme = isSystemInDarkTheme()
    val imageRes = if (isDarkTheme) {
        R.drawable.dark_logout_icon
    } else {
        R.drawable.light_logout_icon
    }

    Image(
        painter = painterResource(id = imageRes),
        contentDescription = null,

        )
}

@OptIn(ExperimentalFoundationApi::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun LazyRowCompletedTask(
                         onDeletedClick: (String) -> Unit,
                         onUnMarkcompletedClick: (String) -> Unit,
                         selectedMarkedItemId: MutableState<String>,
                         isChecked: MutableState<Boolean>){
    val database = FirebaseDatabase.getInstance()
    val user = FirebaseAuth.getInstance().currentUser
    val uid = user?.uid
    var completedTasksRef = database.reference.child("Task").child("CompletedTasks").child(uid.toString())
    var cardDataList = remember {
        mutableStateListOf<DataClass>()
    }
    LaunchedEffect(Unit){
        val valueEventListener = object :ValueEventListener{
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                cardDataList.clear()
                for(childSnapshot in dataSnapshot.children){
                    val id = childSnapshot.key.toString()
                    val data = childSnapshot.getValue(DataClass::class.java)
                    data?.let {
                        cardDataList.add(it.copy(id = id))
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseError", "Database operation cancelled: $error")
            }
        }
        completedTasksRef.addValueEventListener(valueEventListener)
    }
    LazyRow(contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)){
        items(cardDataList.reversed(),key = {it.id}){cardData ->
            val originalDateFormat = DateTimeFormatter.ofPattern("MM/dd/yyyy")
            val desiredDateFormat = DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale.ENGLISH)
            val dateStringFromDatabase = cardData.date
            val formattedDate = if (!dateStringFromDatabase.isNullOrEmpty()) {
                val originalDate = LocalDate.parse(dateStringFromDatabase, originalDateFormat)
                val currentYear = LocalDate.now().year

                val desiredDateFormat = if (originalDate.year == currentYear) {
                    DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale.ENGLISH)
                } else {
                    DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale.ENGLISH)
                }

                originalDate.format(desiredDateFormat)
            } else {
                ""
            }

            MarkCompletedCircleDesign(
                id = cardData.id,
                message = cardData.message!!,
                time = cardData.time!!,
                date = formattedDate ,
            onDeletedClick,
            onUnMarkcompletedClick = onUnMarkcompletedClick,
                selectedMarkedItemId,
                modifier = Modifier.animateItemPlacement(),
                isChecked = isChecked
            )

        }
    }
}
@SuppressLint("UnrememberedMutableState")
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun MarkCompletedCircleDesign(
                              id:String,
                              message:String?,
                              time: String,
                              date:String,
                              onDeletedClick:(String) -> Unit,
                              onUnMarkcompletedClick:(String) -> Unit,
                              selectedMarkedItemId: MutableState<String>,
                              modifier: Modifier=Modifier,
                              isChecked: MutableState<Boolean>
                              ){

    Box(
        modifier = modifier
            .size(184.dp)
            .bounceClick()
            .background(color = MaterialTheme.colors.primary, shape = CircleShape)
            .clip(CircleShape)
            .clickable(indication = null,
                interactionSource = remember { MutableInteractionSource() }) {
                selectedMarkedItemId.value = id
            },
        contentAlignment = Alignment.Center
    ){
        val originalDate: LocalDate? = if (date.isNotEmpty()) {
            try {
                LocalDate.parse(date, DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale.ENGLISH))
            } catch (e: DateTimeParseException) {
                // Handle the parsing error, log it, or set originalDate to a default value
                null
            }
        } else {
            null
        }
        val currentYear = LocalDate.now().year
        val desiredDateFormat = originalDate?.let {
            if (it.year == currentYear) {
                DateTimeFormatter.ofPattern("EEE, d MMM", Locale.ENGLISH)
            } else {
                DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale.ENGLISH)
            }
        }
        val dateString = if (originalDate != null) {
            if (time.isNotEmpty()) {
                "${originalDate.format(desiredDateFormat)}, $time"
            } else {
                originalDate.format(desiredDateFormat)
            }
        } else {
            ""
        }
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ){
            ThemedFilledSquareImage(modifier = Modifier
                .padding(top = 32.dp)
                .clickable {
                    onUnMarkcompletedClick(id)

                })
            Text(
                text = buildAnnotatedString {
                    append("$message")
                    addStyle(
                        style = SpanStyle(
                            textDecoration = TextDecoration.LineThrough
                        ),
                        start = 0,
                        end = message!!.length
                    )
                },
                textAlign = TextAlign.Center,
                fontFamily = interDisplayFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                color = MaterialTheme.colors.secondary,
                modifier = Modifier.padding(top = 24.dp,start = 16.dp,end = 16.dp),
                style = androidx.compose.ui.text.TextStyle(letterSpacing = 0.sp)
            )
            Text(

                text = dateString,
                fontFamily = interDisplayFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colors.secondary.copy(alpha = 0.75f),
                modifier = Modifier.padding(top = 4.dp,start = 16.dp,end = 16.dp),
                style = androidx.compose.ui.text.TextStyle(letterSpacing = 0.sp)
            )
        }
        if (selectedMarkedItemId.value == id){
            UnMarkCompletedTaskScreen(
                selectedDate = mutableStateOf(date),
                selectedTime = mutableStateOf(time),
                textValue = message!!,
                id = id,
                openKeyboard = false,
                onDismiss ={ selectedMarkedItemId.value = "" },
                onDeletedClick,
                onUnMarkCompletedClick = onUnMarkcompletedClick,
                isChecked
            )
        }
    }
}

@Composable
fun ThemedFilledSquareImage(modifier: Modifier) {
    val isDarkTheme = isSystemInDarkTheme()

    val imageRes = if (isDarkTheme) {
        R.drawable.dark_black_square
    } else {
        R.drawable.light_black_square
    }

    Image(
        painter = painterResource(id = imageRes),
        contentDescription = null,
        modifier = modifier

    )

}