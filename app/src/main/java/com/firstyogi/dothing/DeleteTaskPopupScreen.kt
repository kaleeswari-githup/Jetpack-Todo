package com.firstyogi.dothing

import android.media.MediaPlayer
import android.util.Log
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.EaseOutCirc
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController
import com.firstyogi.dothing.ui.theme.FABRed
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

@Composable
fun DeleteTaskScreenPage(
    onDismiss:() -> Unit,
    onDeleteClick:(String)->Unit,
    navController: NavController,
    id:String,
    textHeading:String,
    textDiscription:String,
    date:String,
    time:String,
    message: String,
    repeatOption : String){

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
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally) {
                DeleteTaskScreenCircle(
                    textHeading,
                    textDiscription,
                    id = id,
                    date = date,
                    time = time,
                    message = message,
                    repeatOption = repeatOption
                    )
                Row(modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 32.dp, bottom = 24.dp),
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
                if (clicked.value){
                    DeleteCompletedTask(onDismiss,onDeleteClick,navController,id = id)
                }
            }


        }
    }

}
@Composable
fun DeleteTaskScreenCircle(
    textHeading:String,
    textDiscription:String,
    id:String,
    date:String,
    time:String,
    message:String,
    repeatOption:String){

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
            // .size(344.dp)
            .offset(y = offsetY)
            .scale(scale)

            .background(color = MaterialTheme.colors.secondary, shape = RoundedCornerShape(24.dp))
            .clickable(indication = null,
                interactionSource = remember { MutableInteractionSource() }) { },

      //  contentAlignment = Alignment.Center
    ){
        Column(modifier = Modifier
            .padding(start = 48.dp, end = 48.dp,top = 32.dp, bottom = 48.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally) {

            DeletedTaskCircle(
                id = id,
                date = date ,
                time = time,
                message = message,
                repeatOption = repeatOption,
                showShadow = true
            )

            Text(
                text = textHeading,
                fontFamily = interDisplayFamily,
                fontSize = 24.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colors.primaryVariant,
                modifier = Modifier.padding(top = 64.dp),
               // maxLines = 2,
                letterSpacing = 1.sp,
                textAlign = TextAlign.Center,
                lineHeight = 32.sp,
               // overflow = TextOverflow.Ellipsis
            )
            Text(
                text = textDiscription,
                fontFamily = interDisplayFamily,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colors.primary.copy(alpha = 0.5f),
                modifier = Modifier

                    .padding(top = 12.dp),
                maxLines = 3,
                textAlign = TextAlign.Center,
                letterSpacing = 0.5.sp,
                lineHeight = 20.sp,
                overflow = TextOverflow.Ellipsis
            )

        }
    }
}
@Composable
fun DeletedTaskCircle(id: String,
                      date:String,
                      time:String,
                      message:String,
                      repeatOption:String,
                      inputDateFormat: String = "EEE, d MMM yyyy",
                      showShadow: Boolean = true,
                      topContent: @Composable () -> Unit = {
                          Box(
                              modifier = androidx.compose.ui.Modifier
                                  .padding(top = 8.dp)
                                  .size(32.dp)

                                  .background(
                                      color = androidx.compose.material.MaterialTheme.colors.background,
                                      shape = androidx.compose.foundation.shape.CircleShape
                                  )
                          )
                      }){


    val inputFormatter = DateTimeFormatter.ofPattern(inputDateFormat, Locale.ENGLISH)
    val outputFormatter = DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale.ENGLISH)

    val parsedDate = try {
        LocalDate.parse(date.trim(), inputFormatter)
    } catch (e: DateTimeParseException) {
        null
    }

    val dateString = parsedDate?.let {
        val formattedDate = outputFormatter.format(it)
        if (time.isNotEmpty()) {
            "$formattedDate, $time"
        } else {
            formattedDate
        }
    } ?: ""
    Log.d("DeleteDate","$dateString")

    var borderShadowColor = MaterialTheme.colors.primary
    var shadowColors = MaterialTheme.colors.secondary
    Box(
            modifier = Modifier
                .size(184.dp)
                .aspectRatio(1f)
                .clip(CircleShape)
                .background(MaterialTheme.colors.secondary, shape = CircleShape)
                .then(
                    if (showShadow) Modifier.drawBehind  {
                    val shadowColor = borderShadowColor.copy(alpha = 0.05f) // Soft inner shadow
                    val strokeWidth = 2.dp.toPx() // Shadow thickness
                    val topOffset = 1.5.dp.toPx() // Adjust to keep top shadow visible
                    val bottomOffset = 4.dp.toPx() // Slightly hide bottom shadow

                    drawCircle(
                        color = shadowColor,
                        radius = size.minDimension / 2 - strokeWidth, // Make shadow inner
                        center = Offset(
                            size.width / 2,
                            size.height / 2 - topOffset
                        ), // Shift shadow upward
                        style = Stroke(width = strokeWidth)
                    )

                    drawCircle(
                        color = shadowColors, // Hide bottom part of the shadow
                        radius = size.minDimension / 2 - strokeWidth - bottomOffset,
                        center = Offset(size.width / 2, size.height / 2 + bottomOffset)
                    )
                }else Modifier
                )
                ,
            contentAlignment = androidx.compose.ui.Alignment.Center,
        ) {
            Column(
                modifier = androidx.compose.ui.Modifier
                    .fillMaxSize()
                    .heightIn(max = 200.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
            ) {
                topContent()

                Text(
                    text = ("$message"),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    fontFamily = com.firstyogi.dothing.interDisplayFamily,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                    fontSize = 14.sp,
                    color = androidx.compose.material.MaterialTheme.colors.primary,
                    style = androidx.compose.ui.text.TextStyle(letterSpacing = 0.5.sp),
                    modifier = androidx.compose.ui.Modifier
                        .padding(top = 26.dp, start = 16.dp, end = 16.dp)
                    ,
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    /* .sharedElement(
                         state = rememberSharedContentState(key = "boundsMessage-$id"),
                         animatedVisibilityScope = animatedVisibilityScope,
                         boundsTransform = { _,_, ->
                             tween(300)
                         },
                         placeHolderSize = SharedTransitionScope.PlaceHolderSize.animatedSize
                     )*/

                )
                Text(
                    text =dateString.toUpperCase(),
                    fontFamily = com.firstyogi.dothing.interDisplayFamily,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Normal,
                    fontSize = 11.sp,
                    color = androidx.compose.material.MaterialTheme.colors.primary.copy(alpha = 0.5f),
                    style = androidx.compose.ui.text.TextStyle(letterSpacing = 1.sp),
                    modifier = androidx.compose.ui.Modifier
                        .padding(top = 4.dp, start = 16.dp, end = 16.dp)
                        .height(15.dp),
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    /* .sharedElement(
                         state = rememberSharedContentState(key = "boundsDateandTime-$id"),
                         animatedVisibilityScope = animatedVisibilityScope,
                         boundsTransform = { _,_, ->
                             tween(300)
                         }
                     )*/
                )
                if (repeatOption in listOf("Daily","Weekly","Monthly","Yearly") ){
                    ThemedRepeatedIconImage(
                        modifier = androidx.compose.ui.Modifier
                            .padding(top = 8.dp)
                            .alpha(0.3f))
                }

            }

        }

}
@Composable
fun DeleteCompletedTask(onDismiss: () -> Unit,
                        onDeleteClick:(String)->Unit,
                        navController: NavController,
                        id:String){

    onDeleteClick(id)
    navController.navigate(Screen.Home.route) {
        popUpTo(Screen.Update.route) { inclusive = true }
    }
    onDismiss.invoke()
}