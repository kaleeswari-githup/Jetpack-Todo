package com.example.Pages

import android.annotation.SuppressLint
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.dothings.R
import com.example.dothings.R.DataClass
import com.example.dothings.Screen
import com.example.dothings.interDisplayFamily
import com.example.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun MarkCompletedScreen(navController:NavController,onDismiss: () -> Unit, selectedMarkedItemId: MutableState<String>){
    val database = FirebaseDatabase.getInstance()
    val user = FirebaseAuth.getInstance().currentUser
    val uid = user?.uid
    var completedTasksRef = database.reference.child("Task").child("CompletedTasks").child(uid.toString())
    var isChecked by remember { mutableStateOf(false) }

    val completedTasksCountState = remember { mutableStateOf(0) }
    val scaffoldState = rememberScaffoldState()
    val coroutineScope = rememberCoroutineScope()
    val databaseRef: DatabaseReference = database.reference.child("Task").child(uid.toString())
    val context = LocalContext.current
    val valueEventListener = object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            completedTasksCountState.value = snapshot.childrenCount.toInt()
        }

        override fun onCancelled(error: DatabaseError) {
            // Handle onCancelled event if needed
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
            scaffoldState.snackbarHostState.currentSnackbarData?.dismiss()
            val data = completedTasksRef.child(clickedTaskId).get().await().getValue(DataClass::class.java)
            if (data != null) {
                completedTasksRef.child(clickedTaskId).removeValue()
                val snackbarResult = scaffoldState.snackbarHostState.showSnackbar(
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

        completedTasksRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val data = snapshot.getValue(DataClass::class.java)
                if (data != null) {
                    completedTasksRef.removeValue()
                    coroutineScope.launch {
                        scaffoldState.snackbarHostState.currentSnackbarData?.dismiss()
                        val snackbarResult = scaffoldState.snackbarHostState.showSnackbar(
                            message = "Task UnMark as Completed",
                            actionLabel = "Undo",
                            duration = SnackbarDuration.Short
                        )
                        when (snackbarResult) {
                            SnackbarResult.Dismissed -> {
                                // Snackbar dismissed, no action needed
                                completedTasksRef.removeValue()
                                taskRef.setValue(data)
                            }
                            SnackbarResult.ActionPerformed -> {
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


    val blurEffectBackground by animateDpAsState(targetValue = when{
        selectedMarkedItemId.value.isNotEmpty() -> 25.dp
        else -> 0.dp
    }
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnClickOutside = true,
            dismissOnBackPress = true,
            usePlatformDefaultWidth = false
        )
    ){

        (LocalView.current.parent as DialogWindowProvider)?.window?.setDimAmount(0.1f)
        Scaffold(
            scaffoldState = scaffoldState,
            modifier = Modifier.fillMaxSize(),
            backgroundColor = Color.Transparent,

            ){
            var visible by remember { mutableStateOf(false) }

            // Use LaunchedEffect to animate the 'visible' variable
            LaunchedEffect(Unit) {
                visible = true // Set the visibility to true to trigger the animation
            }

            // Use animateDpAsState to animate the scale for the Box

            Box(modifier = Modifier
                .blur(radius = blurEffectBackground)

                .fillMaxSize()
                .clickable(indication = null,
                    interactionSource = remember { MutableInteractionSource() }) { onDismiss.invoke() },

                ) {

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
                            .background(color = Color.White, shape = RoundedCornerShape(32.dp))
                            .clickable(indication = null,
                                interactionSource = remember { MutableInteractionSource() }) { },

                            ) {
                            Row(modifier = Modifier.padding(start = 24.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(modifier = Modifier
                                    .size(48.dp)
                                    .background(shape = CircleShape, color = SurfaceGray),
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
                                    Text(text = "${user?.displayName}",
                                        fontFamily = interDisplayFamily,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color.Black
                                    )
                                    Text(text = "${user?.email}",
                                        fontFamily = interDisplayFamily,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Normal,
                                        color = Text2
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
                           .background(color = Color.White, shape = RoundedCornerShape(32.dp))
                           .clickable(indication = null,
                               interactionSource = remember { MutableInteractionSource() }) { },
                           contentAlignment = Alignment.Center) {
                           Column(modifier = Modifier,
                               verticalArrangement = Arrangement.Center,
                               horizontalAlignment = Alignment.CenterHorizontally
                           ) {
                               Text(text = "Completed (${completedTasksCountState.value})",
                                   fontFamily = interDisplayFamily,
                                   fontSize = 15.sp,
                                   fontWeight = FontWeight.Medium,
                                   color = Color.Black,
                                   modifier = Modifier.padding(top = 24.dp))
                               Spacer(modifier = Modifier.padding(top = 12.dp))
                               Box(modifier = Modifier
                                   .fillMaxWidth()

                                   .height(204.dp)
                                   .padding(start = 24.dp, end = 24.dp)
                                   .background(
                                       color = MarkCompleteBack,
                                       shape = RoundedCornerShape(24.dp)
                                   )
                                   ,
                                   contentAlignment = Alignment.Center) {

                                   LazyRowCompletedTask(onDismiss,onDeleteClick,onUnMarkCompletedClick,selectedMarkedItemId)

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
                            .background(color = Color.White, shape = RoundedCornerShape(32.dp))
                            .clickable(indication = null,
                                interactionSource = remember { MutableInteractionSource() }) { },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(modifier = Modifier
                                .fillMaxSize()
                                .padding(start = 24.dp, end = 24.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween) {
                                Box() {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Image(painter = painterResource(id = R.drawable.sound), contentDescription = null)
                                        Text(text = "Sound",
                                            fontFamily = interDisplayFamily,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color.Black,
                                            modifier = Modifier.padding(start = 8.dp)
                                        )
                                    }

                                }
                                val animatedCheckedState = remember { mutableStateOf(isChecked) }
                                Box(modifier = Modifier) {
                                    Box(
                                        modifier = Modifier
                                            .clickable(indication = null,
                                                interactionSource = remember { MutableInteractionSource() }) { isChecked = !isChecked }
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(48.dp, 28.dp)
                                                .background(
                                                    if (isChecked) Color.Black else SmallBox,
                                                    shape = CircleShape
                                                )
                                                , contentAlignment = Alignment.Center
                                        ) {

                                                Spacer(
                                                    modifier = Modifier
                                                        .padding(start = 4.dp, end = 4.dp)
                                                        .align(if (isChecked) Alignment.CenterEnd else Alignment.CenterStart)
                                                        .size(20.dp)
                                                        .background(Color.White, CircleShape)

                                                )


                                        }
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
                          .background(color = Color.White, shape = RoundedCornerShape(32.dp))
                          .clickable(indication = null,
                              interactionSource = remember { MutableInteractionSource() }) {
                              val auth = FirebaseAuth.getInstance()
                              // Sign out from Firebase
                              auth.signOut()

                              // Sign out from Google
                              googleSignInClient
                                  .signOut()
                                  .addOnCompleteListener {
                                      // Optional: Perform any additional actions after sign out
                                      navController.navigate(route = Screen.Main.route)
                                      onDismiss.invoke()
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
                                      Image(painter = painterResource(id = R.drawable.logout), contentDescription = null)
                                      Text(text = "Log Out",
                                          fontFamily = interDisplayFamily,
                                          fontSize = 15.sp,
                                          fontWeight = FontWeight.Medium,
                                          color = Color.Black,
                                          modifier = Modifier.padding(start = 8.dp)
                                      )

                                  }

                              }

                              Box(modifier = Modifier

                                  .align(Alignment.CenterVertically)
                              ) {
                                  Image(painter = painterResource(id = R.drawable.right), contentDescription = null)
                              }

                          }

                      }
                  }

                }

                CrossFloatingActionButton {
                    onDismiss.invoke()
                }
            }
        }

    }

}

@OptIn(ExperimentalFoundationApi::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun LazyRowCompletedTask(onDismiss: () -> Unit,
                         onDeletedClick: (String) -> Unit,
                         onUnMarkcompletedClick: (String) -> Unit,
                         selectedMarkedItemId: MutableState<String>){
    val database = FirebaseDatabase.getInstance()
    val user = FirebaseAuth.getInstance().currentUser
    val uid = user?.uid
    var completedTasksRef = database.reference.child("Task").child("CompletedTasks").child(uid.toString())
    var cardDataList = remember {
        mutableStateListOf<DataClass>()
    }
    val imageResource = R.drawable.black_square
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
            val formattedDate = if (dateStringFromDatabase!!.isNotEmpty()) {

                val originalDate = LocalDate.parse(dateStringFromDatabase, originalDateFormat)

                originalDate.format(desiredDateFormat)
            } else {

                ""
            }
            MarkCompletedCircleDesign(
                image = imageResource,
                id = cardData.id,
                message = cardData.message!!,
                time = cardData.time!!,
                date = formattedDate ,
            onDismiss = onDismiss,
            onDeletedClick,
            onUnMarkcompletedClick = onUnMarkcompletedClick,
                selectedMarkedItemId,
                modifier = Modifier.animateItemPlacement()
            )

        }
    }
}
@SuppressLint("UnrememberedMutableState")
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun MarkCompletedCircleDesign(image:Int,
                              id:String,
                              message:String,
                              time: String,
                              date:String,
                              onDismiss: () -> Unit,
                              onDeletedClick:(String) -> Unit,
                              onUnMarkcompletedClick:(String) -> Unit,
                              selectedMarkedItemId: MutableState<String>,
                              modifier: Modifier=Modifier
                              ){
    val painter: Painter = painterResource(image)
    val database = FirebaseDatabase.getInstance()
    val user = FirebaseAuth.getInstance().currentUser
    val uid = user?.uid
    val completedTasksItem = remember { mutableStateOf("") }

    var completedTasksRef = database.reference.child("Task").child("CompletedTasks").child(uid.toString())
    val databaseRef: DatabaseReference = database.reference.child("Task").child(uid.toString())
    Box(
        modifier = modifier
            .size(172.dp)
            .bounceClick()
            .background(color = Color.White, shape = CircleShape)
            .clip(CircleShape)
            .clickable(indication = null,
                interactionSource = remember { MutableInteractionSource() }) {
                selectedMarkedItemId.value = id
            }

            ,

        contentAlignment = Alignment.Center
    ){
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ){
            Image(
                painter = painter ,
                contentDescription = "square image",
                modifier = Modifier
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
                        end = message.length
                    )
                },
                textAlign = TextAlign.Center,
                fontFamily = interDisplayFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp,
                color = Text1,
                modifier = Modifier.padding(top = 24.dp,start = 16.dp,end = 16.dp)
            )
            Text(

                text = "$date $time",
                fontFamily = interDisplayFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp,
                color = Text2,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        if (selectedMarkedItemId.value == id){
            UnMarkCompletedTaskScreen(
                selectedDate = mutableStateOf(date),
                selectedTime = mutableStateOf(time),
                textValue = message,
                id = id,
                openKeyboard = false,
                onDismiss ={ selectedMarkedItemId.value = "" },
                onDeletedClick,
                onUnMarkCompletedClick = onUnMarkcompletedClick
            )
        }
    }
}
