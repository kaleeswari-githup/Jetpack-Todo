package com.example

import android.content.Context
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.dothings.R.DataClass
import com.example.ui.theme.Text1
import com.example.ui.theme.Text2
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener


object CounterWidget: GlanceAppWidget() {

    val countKey = intPreferencesKey("count")
    val database = FirebaseDatabase.getInstance()
    val user = FirebaseAuth.getInstance().currentUser
    val uid = user?.uid

    val databaseRef: DatabaseReference = database.reference.child("Task").child(uid.toString())

    // Initialize the data with default values or null
    var messageText: String? by mutableStateOf(null)
    var dateText: String? by mutableStateOf(null)

    // Function to fetch data from Firebase
    private fun fetchDataFromFirebase() {
        databaseRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (childSnapshot in dataSnapshot.children) {
                    val data = childSnapshot.getValue(DataClass::class.java)
                    data?.let {
                        // Assuming you want the first item's message and date
                        messageText = it.message
                        dateText = it.date
                        // You can also update other Composables here
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle database error
                Log.e("FirebaseError", "Database operation cancelled: $error")
            }
        })
    }

    init {
        fetchDataFromFirebase() // Fetch data when the widget is initialized
    }
    @Composable
    override fun Content() {

        Column(
            modifier = GlanceModifier

               // .size(150.dp)
                .fillMaxSize()
                .cornerRadius(300.dp)
                .background(Color.White),
            verticalAlignment = Alignment.Vertical.CenterVertically,
            horizontalAlignment = Alignment.Horizontal.CenterHorizontally
        ) {

                Text(
                    text =  messageText ?: "",
                    style = TextStyle(
                        fontWeight = FontWeight.Medium,
                        color = ColorProvider(Text1),
                        fontSize = 14.sp,
                    ),
                )
                Text(
                    text =  dateText ?: "",
                    modifier = GlanceModifier.padding(top = 8.dp),
                    style = TextStyle(
                        fontWeight = FontWeight.Medium,
                        color = ColorProvider(Text2),
                        fontSize = 11.sp
                    )
                )




        }
    }
}

class SimpleCounterWidgetReceiver: GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget
        get() = CounterWidget
}

class IncrementActionCallback: ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        updateAppWidgetState(context, glanceId) { prefs ->
            val currentCount = prefs[CounterWidget.countKey]
            if(currentCount != null) {
                prefs[CounterWidget.countKey] = currentCount + 1
            } else {
                prefs[CounterWidget.countKey] = 1
            }
        }
        CounterWidget.update(context, glanceId)
    }
}