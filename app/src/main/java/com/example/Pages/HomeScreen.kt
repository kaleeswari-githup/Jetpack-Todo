package com.example.Pages

import android.annotation.SuppressLint
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import com.example.dothings.*
import com.example.dothings.R
import com.example.dothings.R.DataClass
import com.example.ui.theme.*
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun HomeScreen(navController: NavHostController){
    Box(modifier = Modifier
        .fillMaxSize()
        .background(color = SurfaceGray)){
        Image(painter = painterResource(id = R.drawable.grid_lines), contentDescription = null)
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            LazyGridLayout(navController = navController)
        }
        Column {
            TopSectionHomeScreen(image = R.drawable.home_icon)
            FloatingActionButton()
        }

    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun LazyGridLayout(modifier: Modifier = Modifier,navController: NavController){
    val database = FirebaseDatabase.getInstance()
    val databaseRef = database.reference.child("Task")
    val imageResource = R.drawable.square // Resource ID of the image

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
                TODO("Not yet implemented")
            }
        }
        databaseRef.addValueEventListener(valueEventListener)
    }
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(16.dp)
    ){
       items(cardDataList){cardData ->
            val index = cardDataList.indexOf(cardData)
            val animationDelay = index
           val originalDateFormat = DateTimeFormatter.ofPattern("MM/dd/yyyy") // Assuming the date format in the database is in ISO format
           val desiredDateFormat = DateTimeFormatter.ofPattern("EEE, d MMM yyyy",Locale.ENGLISH) // Desired format: "EEE, d MMM"

           val dateStringFromDatabase = cardData.date // Retrieve the date string from the database

// Parse the date string with the original format
           val formattedDate = if (dateStringFromDatabase!!.isNotEmpty()) {
               // Parse the date string with the original format
               val originalDate = LocalDate.parse(dateStringFromDatabase, originalDateFormat)
               // Format the date with the desired format
               originalDate.format(desiredDateFormat)
           } else {
               // Use an empty string if the date string is empty
               ""
           }

            RoundedCircleCardDesign(
                image = imageResource,
                message = cardData.message!!,
                 time = cardData.time!!,
                date = formattedDate,
                id = cardData.id,
            navController = navController,
            animationDelay = animationDelay)
        }
    }
}


@Composable
fun TopSectionHomeScreen(@DrawableRes image:Int){
    Row(modifier = Modifier
        .fillMaxWidth()
        .padding(top = 24.dp, start = 24.dp, end = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        val shape = RoundedCornerShape(16.dp)
        Text(
            text = "Do Things",
            fontFamily = interDisplayFamily,
            fontWeight = FontWeight.Black,
            fontSize = 24.sp,
            color = Color.White,
            modifier = Modifier
        )
        Box(modifier = Modifier
            .size(48.dp)
            .clip(shape)
            .background(color = SmallBox)

        ) {
            Image(
                painter = painterResource(image),
                contentDescription = "home_icon",
                modifier = Modifier
                    .size(24.dp)
                    .align(Alignment.Center)
            )
        }
    }
}


@SuppressLint("UnrememberedMutableState")
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun RoundedCircleCardDesign(modifier: Modifier = Modifier,
                            navController: NavController,
                            id:String,
                            image:Int,
                            message:String,
                            time: String,
                            date:String,
                            animationDelay: Int){
    val database = FirebaseDatabase.getInstance()
    val databaseRef = database.reference.child("Task")

    val coroutineScope = rememberCoroutineScope()
    var context = LocalContext.current
    val infiniteTransition = rememberInfiniteTransition()
    val painter:Painter = painterResource(image)
    val animateX = animationDelay % 2 == 0 // Determine if translationX animation should be applied
    val animateY = animationDelay % 2 != 0 // Determine if translationY animation should be applied

    val dx by infiniteTransition.animateFloat(
        initialValue = if (animateX) -0.4f else 0f,
        targetValue = if (animateX) 0.5f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing, delayMillis = animationDelay * 200),
            repeatMode = RepeatMode.Reverse
        )
    )

    val dy by infiniteTransition.animateFloat(
        initialValue = if (animateY) -0.4f else 0f,
        targetValue = if (animateY) 0.5f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing, delayMillis = animationDelay * 200),
            repeatMode = RepeatMode.Reverse
        )
    )

    val travelDistance = with(LocalDensity.current) { 10.dp.toPx() }
    var isUpdatedScreenOpen by remember {
        mutableStateOf(false)
    }
    androidx.compose.material.Surface(modifier = Modifier
        .graphicsLayer { translationX = dx * travelDistance
            translationY = dy * travelDistance}, shape = CircleShape) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(172.dp)
                .background(roundedCircleGradient, shape = CircleShape)
                .clip(CircleShape)
                .clickable {
                    isUpdatedScreenOpen = true
                },
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Image(
                    painter = painter,
                    contentDescription = "square image",
                    modifier = Modifier
                        .padding(top = 32.dp)
                        .clickable {
                            coroutineScope.launch {
                                Log.d("MyTag", "id: $id")
                                modifier.explodeOnClick(durationMillis = 3500)
                                databaseRef
                                    .child(id)
                                    .removeValue()
                                    .addOnCompleteListener { task ->
                                        if (task.isSuccessful) {
                                            Toast
                                                .makeText(
                                                    context,
                                                    "Task Deleted",
                                                    Toast.LENGTH_SHORT
                                                )
                                                .show()
                                        } else {
                                            Toast
                                                .makeText(
                                                    context,
                                                    task.exception?.message,
                                                    Toast.LENGTH_SHORT
                                                )
                                                .show()
                                        }
                                    }
                            }

                        }
                )
                Text(
                    text = "$message",
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

            if(isUpdatedScreenOpen){

                UpdateTaskScreen(
                    selectedDate = mutableStateOf(date ) ,
                    selectedTime = mutableStateOf(time) ,
                    textValue = message,
                    id = id,
                    openKeyboard = false,
                    onDismiss = {isUpdatedScreenOpen = false},

                )

            }

        }
    }

}

@SuppressLint("UnrememberedMutableState")
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun FloatingActionButton(){
    var isAddDaskScreenOpen by remember {
        mutableStateOf(false)
    }
    Box(modifier = Modifier
        .fillMaxWidth()
        .fillMaxHeight()
        .padding(bottom = 64.dp)
        .background(Color.Transparent)
    ) {
        androidx.compose.material.FloatingActionButton(
            modifier = Modifier
                .size(64.dp)
                .align(Alignment.BottomCenter),
           onClick = {isAddDaskScreenOpen = true},
            shape = CircleShape,
            contentColor = Color.White,
            backgroundColor = FABDarkColor

        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "")
        }
 if(isAddDaskScreenOpen){
     AddDaskScreen( selectedDate = mutableStateOf(null),
         selectedTime = mutableStateOf(null),
         textValue = "",
         onDismiss = {isAddDaskScreenOpen = false})
 }
    }
}