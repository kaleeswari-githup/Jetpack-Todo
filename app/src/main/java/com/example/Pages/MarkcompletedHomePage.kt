package com.example.Pages

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.example.dothings.R
import com.example.dothings.R.DataClass
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun MarkCompletedHomescreen(onDismiss: () -> Unit){
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
            .fillMaxSize()
        ){
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
             LazyGridCompletedTask()
            }
            CrossFloatingActionButton {
                onDismiss.invoke()
            }
        }
    }

}
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun LazyGridCompletedTask(){
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
        val valueEventListener = object : ValueEventListener {
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
    LazyVerticalGrid(columns = GridCells.Fixed(2),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(16.dp)
        ){
        items(cardDataList.reversed()){cardData ->
            val originalDateFormat = DateTimeFormatter.ofPattern("MM/dd/yyyy")
            val desiredDateFormat = DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale.ENGLISH)

            val dateStringFromDatabase = cardData.date
            val formattedDate = if (dateStringFromDatabase!!.isNotEmpty()) {

                val originalDate = LocalDate.parse(dateStringFromDatabase, originalDateFormat)

                originalDate.format(desiredDateFormat)
            } else {

                ""
            }
          /*  MarkCompletedCircleDesign(
                image = imageResource,
                id = cardData.id,
                message = cardData.message!!,
                time = cardData.time!!,
                date = formattedDate,

            )*/

        }
    }
}