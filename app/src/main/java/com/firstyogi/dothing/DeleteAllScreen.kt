package com.firstyogi.dothing

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.toUpperCase
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.firstyogi.dothing.ui.theme.FABRed
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

@Composable
fun DeleteAllScreenPage(onDismiss:() -> Unit,
                        ){
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ){
        val clicked = remember { mutableStateOf(false) }
        ThemedBackground()
        Box(modifier = Modifier
            .fillMaxSize()
            .clickable(indication = null,
                interactionSource = remember { MutableInteractionSource() }) { onDismiss.invoke() },
            contentAlignment = Alignment.Center){
            Column(modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center) {
                DeleteAllScreenCircle(onDismiss)
                Row(modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 48.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .background(color = Color.White, shape = RoundedCornerShape(53.dp)),
                        contentAlignment = Alignment.Center

                        ){
                        Text(
                            text = stringResource(id = R.string.cancel).toUpperCase(),
                            fontFamily = interDisplayFamily,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.Black,
                            modifier = Modifier
                                .padding(top = 16.dp, start = 24.dp,end = 24.dp, bottom = 16.dp)
                                .clickable {
                                onDismiss.invoke()
                            }
                        )
                    }

                    Box(
                        modifier = Modifier
                            .background(color = FABRed, shape = RoundedCornerShape(53.dp)),
                        contentAlignment = Alignment.Center

                        ){
                        Text(text = stringResource(id = R.string.delete_all),
                            fontSize = 14.sp,
                            fontFamily = interDisplayFamily,
                            fontWeight = FontWeight.Medium,
                            color = Color.White,
                            modifier = Modifier
                                .padding(top = 16.dp, start = 24.dp,end = 24.dp, bottom = 16.dp)
                                .clickable {
                                    clicked.value = true

                                }
                        )
                    }


                }
            }

            if (clicked.value){
                DeleteAllCompletedTask(onDismiss)
            }
        }
    }

}
@Composable
fun DeleteAllScreenCircle(onDismiss: () -> Unit){

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
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 24.dp)
            .size(344.dp)
            .offset(y = offsetY)
            .scale(scale)
            .aspectRatio(1f)
            .clip(CircleShape)
            .background(color = MaterialTheme.colors.primary, shape = CircleShape)
            .clickable(indication = null,
                interactionSource = remember { MutableInteractionSource() }) { },

        contentAlignment = Alignment.Center
    ){
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(start = 32.dp, end = 32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally) {
              Text(
                  text = stringResource(id = R.string.deleteall_heading).toUpperCase(),
                  fontFamily = interDisplayFamily,
                  fontSize = 24.sp,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colors.secondary,
                  modifier = Modifier,
                  textAlign = TextAlign.Center,
                  lineHeight = 32.sp
              )
            Text(
                text = stringResource(id = R.string.deleteall_subtitle),
                fontFamily = interDisplayFamily,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colors.secondary.copy(alpha = 0.75f),
                modifier = Modifier.padding(top = 16.dp ),
                maxLines = 4,
                textAlign = TextAlign.Center,
                lineHeight = 24.sp,
                overflow = TextOverflow.Ellipsis
            )


        }
    }
}
@Composable
fun DeleteAllCompletedTask(onDismiss: () -> Unit,
                           ){
    val database = FirebaseDatabase.getInstance()
    val user = FirebaseAuth.getInstance().currentUser
    val uid = user?.uid
    var context = LocalContext.current
    var completedTasksRef = database.reference.child("Task").child("CompletedTasks").child(uid.toString())
    completedTasksRef.addListenerForSingleValueEvent(object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            for (taskSnapshot in snapshot.children) {
                val task = taskSnapshot.getValue(DataClass::class.java)
                if (task != null && task.repeatedTaskTime in listOf("DAILY", "WEEKLY", "MONTHLY", "YEARLY")) {
                    // If the task has a repeated task time value of interest, move it back to main tasks list
                    val databaseRef = database.reference.child("Task").child(uid.toString()).child(task.id)
                    databaseRef.setValue(task)
//                    val nextDueDate = calculateNextDueDate(System.currentTimeMillis(), task.repeatedTaskTime!!)
//                    scheduleNotification(
//                        context,
//                        nextDueDate,
//                        task.id,
//                        task.message!!,
//                        false,
//                        task.repeatedTaskTime!!
//                    )
                }
            }
            // Remove all completed tasks
            completedTasksRef.removeValue()
            // Invoke onDismiss after all operations are completed
            onDismiss.invoke()
        }

        override fun onCancelled(error: DatabaseError) {
            // Handle cancellation if needed
        }
    })
}