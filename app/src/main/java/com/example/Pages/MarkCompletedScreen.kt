package com.example.Pages

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
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
import com.example.dothings.roundedCircleGradient
import com.example.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun MarkCompletedScreen(navController:NavController,onDismiss: () -> Unit){
    val database = FirebaseDatabase.getInstance()
    val user = FirebaseAuth.getInstance().currentUser
    val uid = user?.uid
    var completedTasksRef = database.reference.child("Task").child("CompletedTasks").child(uid.toString())
    var isChecked by remember { mutableStateOf(false) }
    var isMarkcompletedHomeOpen = remember { mutableStateOf(false) }
    val completedTasksCountState = remember { mutableStateOf(0) }
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

    val blurEffectBackground by animateDpAsState(targetValue = when{
        isMarkcompletedHomeOpen.value -> 25.dp
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
        Box(modifier = Modifier
            .blur(radius = blurEffectBackground)
            .fillMaxSize(),

        ) {

            Column(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, top = 120.dp)
                    .background(color = Color.White, shape = RoundedCornerShape(32.dp)),

                    ) {
                    Row(modifier = Modifier.padding(start = 24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier
                            .size(48.dp)
                            .background(shape = CircleShape, color = FABColor),
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
                Box(modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, top = 8.dp)
                    .background(color = Color.White, shape = RoundedCornerShape(32.dp)),
                    contentAlignment = Alignment.Center) {
                    Column(modifier = Modifier,
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "Completed",
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
                            .background(color = MarkCompleteBack, shape = RoundedCornerShape(24.dp)),
                        contentAlignment = Alignment.Center) {

                        LazyRowCompletedTask()

                        }
                        Spacer(modifier = Modifier.padding(top = 24.dp))
                        if (completedTasksCountState.value > 0) {
                            Text(
                                text = "View All (${completedTasksCountState.value})",
                                fontFamily = interDisplayFamily,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = NewOrange,
                                modifier = Modifier

                            )
                        }
                        Spacer(modifier = Modifier.padding(top = 24.dp))
                    }
                }
                Box(modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .padding(start = 24.dp, end = 24.dp, top = 8.dp)
                    .background(color = Color.White, shape = RoundedCornerShape(32.dp)),
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

                        Box(modifier = Modifier) {
                            Box(
                                modifier = Modifier
                                    .clickable(indication = null,
                                        interactionSource = remember { MutableInteractionSource() }) { isChecked = !isChecked }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp, 24.dp)
                                        .background(
                                            if (isChecked) NewOrange else Color.Gray,
                                            shape = CircleShape
                                        )
                                ) {
                                    Spacer(
                                        modifier = Modifier
                                            .align(if (isChecked) Alignment.CenterEnd else Alignment.CenterStart)
                                            .size(22.dp)
                                            .background(Color.White, CircleShape)
                                    )
                                }
                            }
                        }

                    }

                }
                Box(modifier = Modifier
                    .fillMaxWidth()
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

                CrossFloatingActionButton {
                    onDismiss.invoke()
                }
            /*    if (isMarkcompletedHomeOpen.value){
                    MarkCompletedHomescreen(
                        onDismiss = {isMarkcompletedHomeOpen.value = false}
                    )


                }*/
        }
    }

}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun LazyRowCompletedTask(){
    val database = FirebaseDatabase.getInstance()
    val user = FirebaseAuth.getInstance().currentUser
    val uid = user?.uid
    var completedTasksRef = database.reference.child("Task").child("CompletedTasks").child(uid.toString())
    var cardDataList = remember {
        mutableStateListOf<DataClass>()
    }
    cardDataList.add(0, DataClass())
    val imageResource = R.drawable.yesorangecheckbox
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
        items(cardDataList.reversed().take(5)){cardData ->
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
                date = formattedDate )

        }
    }
}
@Composable
fun MarkCompletedCircleDesign(image:Int,
                              id:String,
                              message:String,
                              time: String,
                              date:String){
    val painter: Painter = painterResource(image)
    val database = FirebaseDatabase.getInstance()
    val user = FirebaseAuth.getInstance().currentUser
    val uid = user?.uid
    val completedTasksItem = remember { mutableStateOf("") }
    var completedTasksRef = database.reference.child("Task").child("CompletedTasks").child(uid.toString())
    val databaseRef: DatabaseReference = database.reference.child("Task").child(uid.toString())
    Box(
        modifier = Modifier
            .size(172.dp)
            .background(roundedCircleGradient, shape = CircleShape)
            .clip(CircleShape),
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
                        completedTasksRef.child(id).get().addOnSuccessListener { completedTaskSnapshot ->
                            // Get the completed task snapshot
                            val completedTask = completedTaskSnapshot.getValue(DataClass::class.java)

                            // Remove the completed task from "CompletedTasks"
                            completedTasksRef.child(id).removeValue()
                                // Add the completed task to "Task"
                                val newTaskRef = databaseRef.push()
                                newTaskRef.setValue(completedTask)

                        }
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
    }
}
@Composable
fun MarktoTaskItem(){
    val database = FirebaseDatabase.getInstance()
    val user = FirebaseAuth.getInstance().currentUser
    val uid = user?.uid
    val databaseRef:DatabaseReference = database.reference.child("Task").child(uid.toString())
    var cardDataList = remember {
        mutableStateListOf<DataClass>()
    }
    cardDataList.add(0, DataClass())

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
        databaseRef.addValueEventListener(valueEventListener)
    }
}