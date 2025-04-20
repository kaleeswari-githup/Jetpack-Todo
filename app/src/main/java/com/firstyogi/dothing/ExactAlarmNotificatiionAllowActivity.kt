package com.firstyogi.dothing

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Scaffold
import androidx.compose.material.rememberScaffoldState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.firstyogi.ui.theme.AppJetpackComposeTheme

class ExactAlarmNotificatiionAllowActivity : ComponentActivity(){

        private val preferenceKey = "exact_alarm_permission_choice"

        @RequiresApi(Build.VERSION_CODES.S)
        @SuppressLint("SuspiciousIndentation", "UnusedBoxWithConstraintsScope",
            "UnusedMaterialScaffoldPaddingParameter"
        )
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setContent {
                AppJetpackComposeTheme {
                    val context = LocalContext.current
                    val sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)

                    // Check if user has already made a choice
                    val userChoice = sharedPreferences.getBoolean(preferenceKey, false)

                        val scaffoldState = rememberScaffoldState()
                    if (userChoice && (getSystemService(Context.ALARM_SERVICE) as AlarmManager).canScheduleExactAlarms()) {
                        // Permission granted and choice made, go to MainActivity
                        val intent = Intent(context, MainActivity::class.java)
                        context.startActivity(intent)
                        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                        finish()
                        return@AppJetpackComposeTheme
                    }

                        // Hide status and navigation bar when this screen is shown

                        Scaffold(
                            scaffoldState = scaffoldState,
                            modifier = Modifier.fillMaxSize(),
                            backgroundColor = Color.Transparent
                        ){
                            BoxWithConstraints (modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    color = androidx.compose.material.MaterialTheme.colors.background
                                ),

                                ){
                                // ThemedGridImage(modifier = Modifier)


                                Column(modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState()),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {


                                        // Background Box (Card)
                                        Box(
                                            modifier = Modifier
                                                .height(244.dp)
                                                .padding(top = 32.dp, start = 24.dp,end = 24.dp)
                                                .background(
                                                    color = androidx.compose.material.MaterialTheme.colors.primary,
                                                    shape = RoundedCornerShape(32.dp)
                                                )
                                        ){

                                        ThemedExactNotificationImage(modifier = Modifier)
                                        }

                                        // ThemedNotificationImage on top of the Box

                                    Spacer(modifier = Modifier.weight(1f))
                                    Box(modifier = Modifier
                                        .wrapContentSize(),
                                        contentAlignment = Alignment.Center
                                    ){
                                        Column(modifier = Modifier,
                                            horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(text = "Allow precise reminders",
                                                color = androidx.compose.material.MaterialTheme.colors.primary,
                                                modifier = Modifier.padding(),
                                                fontFamily = interDisplayFamily,
                                                fontSize = 24.sp,
                                                fontWeight = FontWeight.Medium,
                                                textAlign = TextAlign.Center,
                                                letterSpacing = 0.5.sp
                                            )
                                            Spacer(modifier = Modifier.padding(top = 16.dp))
                                            Text(text = "Get notified at the exact time \nyou set—right on time, every \ntime.",
                                                color = androidx.compose.material.MaterialTheme.colors.primary.copy(alpha = 0.5f),
                                                modifier = Modifier.padding( start = 24.dp,end = 24.dp),
                                                fontFamily = interDisplayFamily,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Medium,
                                                textAlign = TextAlign.Center,
                                                lineHeight = 20.sp,
                                                letterSpacing = 0.5.sp)
                                        }

                                    }
                                    Spacer(modifier = Modifier.weight(1f))
                                    Box(modifier = Modifier
                                        .wrapContentSize()
                                        .padding(bottom = 48.dp)){
                                        Column {
                                            Box(modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(start = 48.dp, end = 48.dp)
                                                .height(48.dp)
                                                .background(
                                                    color = androidx.compose.material.MaterialTheme.colors.primary,
                                                    shape = RoundedCornerShape(64.dp)
                                                )
                                                .clickable(indication = null,
                                                    interactionSource = remember { MutableInteractionSource() }) {
                                                    sharedPreferences.edit()
                                                        .putBoolean(preferenceKey, true)
                                                        .apply()
                                                    val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                                        data = Uri.parse("package:${context.packageName}")
                                                    }
                                                    context.startActivity(intent)
                                                },
                                                contentAlignment = Alignment.Center
                                            ){
                                                ButtonTextWhiteTheme(text = ("Allow precise reminders"),
                                                    color = androidx.compose.material.MaterialTheme.colors.secondary,modifier = Modifier
                                                )
                                            }
                                            Spacer(modifier = Modifier.padding(top = 24.dp))
                                            Box(modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(start = 48.dp, end = 48.dp)
                                                .height(48.dp)
                                                .background(
                                                    color = androidx.compose.material.MaterialTheme.colors.secondary,
                                                    shape = RoundedCornerShape(64.dp)
                                                )
                                                .clickable(indication = null,
                                                    interactionSource = remember { MutableInteractionSource() }) {

                                                        sharedPreferences.edit()
                                                            .putBoolean(preferenceKey, false)
                                                            .apply()
                                                    // Go to MainActivity
                                                    val intent = Intent(context, MainActivity::class.java)
                                                    context.startActivity(intent)
                                                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                                                    finish()
                                                    overridePendingTransition(
                                                        android.R.anim.fade_in,
                                                        android.R.anim.fade_out
                                                    )

                                                    finish()
                                                },
                                                contentAlignment = Alignment.Center
                                            ){
                                                ButtonTextWhiteTheme(text = ("Skip for now"),
                                                    color = androidx.compose.material.MaterialTheme.colors.primary,

                                                    modifier = Modifier
                                                )
                                            }
                                        }

                                    }

                                    Text(text = "NEEDED FOR EXACTLY-TIMED NOTIFICATIONS.".toUpperCase(),
                                        fontFamily = interDisplayFamily,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = androidx.compose.material.MaterialTheme.colors.primary.copy(alpha = 0.5f),
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(bottom = 60.dp),
                                        letterSpacing = 1.sp
                                    )
                                }



                            }
                        }

                    }


                }
            




}
    override fun onResume() {
        super.onResume()

        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                // Permission granted → go to MainActivity
                getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
                    .edit()
                    .putBoolean(preferenceKey, true)
                    .apply()
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                finish()
            }
            // else stay on the permission screen
        } else {
            // For Android 11 and below, no permission needed → go to MainActivity
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}

@Composable
fun ThemedExactNotificationImage(modifier: Modifier) {
    val isDarkTheme = isSystemInDarkTheme()
    val imageRes = if (isDarkTheme) {
        R.drawable.exact_screen_image_dark
    } else {
        R.drawable.exact_image_light
    }

    Image(
        painter = painterResource(id = imageRes),
        contentDescription = null,
        modifier = modifier
    )
}