package com.example

import android.annotation.SuppressLint
import android.os.Build
import android.os.Handler
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.background
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.unit.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.example.dothings.R.DataClass
import com.example.dothings.interDisplayFamily
import com.example.ui.theme.Text1
import com.example.ui.theme.Text2
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.sql.Time
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale


object CounterWidget: GlanceAppWidget() {

    val countKey = intPreferencesKey("count")
    val database = FirebaseDatabase.getInstance()
    val user = FirebaseAuth.getInstance().currentUser
    val uid = user?.uid

    val databaseRef: DatabaseReference = database.reference.child("Task").child(uid.toString())

    // Initialize the data with default values or null
    var messageText by mutableStateOf<String?>(null)
    var dateText  = mutableStateOf<Date?>(null)
    var timeText = mutableStateOf<Date?>(null)
    var idText by mutableStateOf<String?>(null)
   
    // Function to fetch data from Firebase
    private fun fetchDataFromFirebase() {
        databaseRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                // Get the first item in the database
                val currentDateTime = Calendar.getInstance()
                var nearestDateTimeDifference = Long.MAX_VALUE
                var nearestItem: DataSnapshot? = null

                // Loop through the database items to find the one with the nearest date and time
                for (itemSnapshot in dataSnapshot.children) {
                    val item = itemSnapshot.getValue(DataClass::class.java)
                    val itemDate = item?.date // Replace 'date' with your actual date field
                    val itemTime = item?.time // Replace 'time' with your actual time field

                    if (itemDate != null && itemTime != null) {
                        val itemDateTime = Calendar.getInstance()
                        itemDateTime.time = SimpleDateFormat("MM/dd/yyyy hh:mm a").parse("$itemDate $itemTime")

                        val dateTimeDifference = Math.abs(itemDateTime.timeInMillis - currentDateTime.timeInMillis)
                        if (dateTimeDifference < nearestDateTimeDifference) {
                            nearestDateTimeDifference = dateTimeDifference
                            nearestItem = itemSnapshot
                        }
                    }
                }

                // Check if we found a nearest item
                if (nearestItem != null) {
                    // Update the widget with the nearest item's data
                    messageText = nearestItem.getValue(DataClass::class.java)?.message
                    val nearestItemDate = SimpleDateFormat("MM/dd/yyyy").parse(nearestItem.getValue(DataClass::class.java)?.date)
                    dateText.value = nearestItemDate
                    val nearestItemTime = SimpleDateFormat("hh:mm a").parse(nearestItem.getValue(DataClass::class.java)?.time)
                    timeText.value = nearestItemTime
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle database error
            }
        })

    }

    init {
        fetchDataFromFirebase() // Fetch data when the widget is initialized
    }
    @SuppressLint("RememberReturnType")
    @RequiresApi(Build.VERSION_CODES.O)
    @Composable
    override fun Content() {

val isOpen = remember{
    mutableStateOf(false)
}


        Column(
            modifier = GlanceModifier
                // .size(150.dp)
                .fillMaxSize()
                .cornerRadius(300.dp)
                .background(day = Color.White, night = Color.Black)
              //  .clickable(actionStartActivity<>())

                ,
            verticalAlignment = Alignment.Vertical.CenterVertically,
            horizontalAlignment = Alignment.Horizontal.CenterHorizontally
        ) {
            Text(
                text =  messageText ?: "",
                style = TextStyle(
                    fontWeight = FontWeight.Medium,
                    color = ColorProvider(day = Color.Black, night = Color.White),
                    fontSize = 14.sp,
                ),
            )
            Row() {
                Text(
                    text = dateText.value?.let { SimpleDateFormat("EEE, d MMM ,", Locale.getDefault()).format(it) } ?: "",
                    modifier = GlanceModifier.padding(top = 8.dp),
                    style = TextStyle(
                        fontWeight = FontWeight.Medium,
                        color = ColorProvider(day = Color.Black, night = Color.White),
                        fontSize = 11.sp
                    )
                )
                Text(
                    text =  timeText.value?.let { SimpleDateFormat("hh:mm a", Locale.getDefault()).format(it) } ?: "",
                    modifier = GlanceModifier.padding(top = 8.dp),
                    style = TextStyle(
                        fontWeight = FontWeight.Medium,
                        color = ColorProvider(day = Color.Black, night = Color.White),
                        fontSize = 11.sp
                    )
                )
            }





        }
    }
}
@SuppressLint("UnrememberedMutableState")
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun MarkCompletedCircleDesign(

                              message:String,
                              time: String,
                              date:String,

){

    val database = FirebaseDatabase.getInstance()
    val user = FirebaseAuth.getInstance().currentUser
    val uid = user?.uid

    Box(
        modifier = Modifier
            .size(172.dp)
            .background(color = Color.White, shape = CircleShape)
            .clip(CircleShape)


        ,

        contentAlignment = androidx.compose.ui.Alignment.Center
    ){
        androidx.compose.foundation.layout.Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
        ){



            androidx.compose.material.Text(

                text = "$date, $time",
                fontFamily = interDisplayFamily,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                fontSize = 11.sp,
                color = Text2,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

    }
}


