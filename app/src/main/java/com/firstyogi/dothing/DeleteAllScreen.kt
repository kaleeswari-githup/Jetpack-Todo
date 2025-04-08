package com.firstyogi.dothing

import android.util.Log
import androidx.compose.animation.core.EaseOutCirc
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.toUpperCase
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.wear.tiles.ModifiersBuilders.Padding
import com.firstyogi.dothing.ui.theme.FABRed
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.delay

@Composable
fun DeleteAllScreenPage(onDismiss:() -> Unit,
                        cardDataList: List<DataClass>
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
            Column(modifier = Modifier.fillMaxSize()
                ,
                verticalArrangement = Arrangement.Center) {
                DeleteAllScreenCircle(cardDataList = cardDataList)
                Row(modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 48.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .background(color = MaterialTheme.colors.secondary, shape = RoundedCornerShape(53.dp)),
                        contentAlignment = Alignment.Center

                        ){
                        Text(
                            text = stringResource(id = R.string.cancel),
                            fontFamily = interDisplayFamily,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colors.primary,
                            letterSpacing = 1.sp,
                            modifier = Modifier
                                .padding(top = 16.dp, start = 24.dp, end = 24.dp, bottom = 16.dp)
                                .clickable {
                                    onDismiss.invoke()
                                }
                        )
                    }
                    Spacer(modifier = Modifier.padding(32.dp))
                    Box(
                        modifier = Modifier
                            .background(color = MaterialTheme.colors.primary, shape = RoundedCornerShape(53.dp)),
                        contentAlignment = Alignment.Center

                        ){
                        Text(text = stringResource(id = R.string.delete_all),
                            fontSize = 14.sp,
                            fontFamily = interDisplayFamily,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colors.secondary,
                            letterSpacing = 1.sp,
                            modifier = Modifier
                                .padding(top = 16.dp, start = 24.dp, end = 24.dp, bottom = 16.dp)
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
fun DeleteAllScreenCircle(cardDataList:List<DataClass>){

    var visible by remember {
        mutableStateOf(false)
    }
    LaunchedEffect(Unit) {
        visible = true

    }
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(
            easing = EaseOutCirc
        )

    )
    val offsetY by animateDpAsState(
        targetValue = if (visible) 0.dp else -48.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessVeryLow
        )

    )
    Box(
        modifier = Modifier

            .wrapContentSize()
            .padding(start = 24.dp, end = 24.dp)

            .offset(y = offsetY)
            .scale(scale)
            .background(color = MaterialTheme.colors.secondary, shape = RoundedCornerShape(24.dp))

            .clickable(indication = null,
                interactionSource = remember { MutableInteractionSource() }) { },

    ){
        Box(modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        MaterialTheme.colors.primaryVariant,
                        MaterialTheme.colors.secondary
                    ),
                    center = Offset(x = 500f, y = 10f), // Adjust the center of the gradient
                    radius = 700f
                ), shape = RoundedCornerShape(24.dp)
            )
        )
        Column(modifier = Modifier
            .fillMaxWidth()
            .padding(top = 32.dp, bottom = 48.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(218.dp)
                //  .padding(start = 24.dp, end = 24.dp)

                ,
                contentAlignment = Alignment.Center
            ){
                LazyRowDeletedAllItems(cardDataList = cardDataList)
            }

              Text(
                  text = stringResource(id = R.string.deleteall_heading),
                  fontFamily = interDisplayFamily,
                  fontSize = 24.sp,
                  fontWeight = FontWeight.Medium,
                  color = MaterialTheme.colors.primaryVariant,
                  textAlign = TextAlign.Center,
                  lineHeight = 32.sp,
                  letterSpacing = 0.5.sp,
                  modifier = Modifier.padding(start = 48.dp, end = 48.dp,top = 64.dp)
              )
            Text(
                text = stringResource(id = R.string.deleteall_subtitle),
                fontFamily = interDisplayFamily,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colors.primary.copy(alpha = 0.5f),
                modifier = Modifier.padding(top = 16.dp, start = 48.dp,end = 48.dp ),
                maxLines = 4,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp,
                overflow = TextOverflow.Ellipsis,
                letterSpacing = 0.5.sp
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
                if (task != null && task.repeatedTaskTime in listOf("Daily", "Weekly", "Monthly", "Yearly")) {
                    // If the task has a repeated task time value of interest, move it back to main tasks list
                    val databaseRef = database.reference.child("Task").child(uid.toString()).child(task.id)
                    databaseRef.setValue(task)
                    val nextDueDate = calculateNextDueDate(System.currentTimeMillis(), task.repeatedTaskTime!!)
                    scheduleNotification(
                        context,
                        nextDueDate,
                        task.id,
                        task.message!!,
                        false,
                        task.repeatedTaskTime!!
                    )
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



@Composable
fun LazyRowDeletedAllItems( cardDataList: List<DataClass>){
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // We simulate infinite scrolling by repeating the list


    // Start auto-scroll
    LaunchedEffect(Unit) {
        while (true) {
            delay(30) // smoothness/speed
            val firstVisible = listState.firstVisibleItemIndex
            val scrollOffset = listState.firstVisibleItemScrollOffset

            listState.scrollToItem(firstVisible, scrollOffset + 2) // adjust speed by px
        }
    }
        LazyRow(
            state = listState,
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(cardDataList.reversed()) { cardData ->
                DeletedTaskCircle(
                    id = cardData.id,
                    date = cardData.date!!,
                    time = cardData.time!!,
                    message = cardData.message!!,
                    repeatOption = cardData.repeatedTaskTime!!,
                    inputDateFormat = "MM/dd/yyyy",
                    topContent = {
                        ThemedFilledSquareImage(modifier = Modifier.padding(top = 8.dp))
                    }
                )
                Log.d("DeleteDate","${cardData.date}")
            }


    }
}