package com.firstyogi.dothing

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.toUpperCase
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.firstyogi.ui.theme.AppJetpackComposeTheme
import com.google.accompanist.systemuicontroller.rememberSystemUiController

class NotificationPermissionActivity : ComponentActivity() {
    private val preferenceKey = "notification_permission_choice"

    @SuppressLint("SuspiciousIndentation", "UnusedBoxWithConstraintsScope",
        "UnusedMaterialScaffoldPaddingParameter"
    )
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppJetpackComposeTheme {
                val context = LocalContext.current
                val userChoice = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
                    .getBoolean(preferenceKey, false)
                        if (userChoice) {
                            // User has already made a choice, navigate to MainActivity directly
                            val intent = Intent(context, MainActivity::class.java)
                            context.startActivity(intent)
                            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)

                            finish()
                        }else{
                            var hasNotificationPermission by remember {
                                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
                                    mutableStateOf(
                                        ContextCompat.checkSelfPermission(
                                            context,
                                            Manifest.permission.POST_NOTIFICATIONS
                                        )== PackageManager.PERMISSION_GRANTED
                                    )
                                }else mutableStateOf(true)

                            }
                            val launcher =
                                rememberLauncherForActivityResult(
                                    contract = ActivityResultContracts.RequestPermission()
                                ) { isGranted ->
                                    hasNotificationPermission = isGranted
                                    onPermissionResult(isGranted)
                                }
                            val scaffoldState = rememberScaffoldState()

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
                                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {

                                                // Background Box (Card)
                                                Box(
                                                    modifier = Modifier
                                                        .size(height = 244.dp, width = 272.dp)
                                                        .padding(top = 32.dp)
                                                        .background(
                                                            color = androidx.compose.material.MaterialTheme.colors.secondary,
                                                            shape = RoundedCornerShape(32.dp)
                                                        )
                                                )

                                                // ThemedNotificationImage on top of the Box
                                                ThemedNotificationImage(
                                                    modifier = Modifier
                                                        .offset(y = 80.dp) // Move it down to sit on top of the Box
                                                        .align(Alignment.TopCenter) // Keep it centered horizontally
                                                )
                                            }
                                            Spacer(modifier = Modifier.weight(1f))
                                            Box(modifier = Modifier
                                                .wrapContentSize(),
                                                contentAlignment = Alignment.Center
                                            ){
                                                Column(modifier = Modifier,
                                                    horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Text(text = "Allow notifications",
                                                        color = androidx.compose.material.MaterialTheme.colors.primary,
                                                        modifier = Modifier.padding(),
                                                        fontFamily = interDisplayFamily,
                                                        fontSize = 24.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        textAlign = TextAlign.Center,
                                                        letterSpacing = 0.5.sp
                                                    )
                                                    Spacer(modifier = Modifier.padding(top = 16.dp))
                                                    Text(text = "Get task notifications only—\nno unnecessary alerts, just\n what you need.",
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
                                                            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                                        },
                                                        contentAlignment = Alignment.Center
                                                    ){
                                                        ButtonTextWhiteTheme(text = ("Allow notifications"),
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
                                                            val intent =
                                                                Intent(
                                                                    context,
                                                                    MainActivity::class.java
                                                                )
                                                            context.startActivity(intent)
                                                            overridePendingTransition(
                                                                android.R.anim.fade_in,
                                                                android.R.anim.fade_out
                                                            )

                                                            finish()
                                                        },
                                                        contentAlignment = Alignment.Center
                                                    ){
                                                        ButtonTextWhiteTheme(text = ("Not Now"),
                                                            color = androidx.compose.material.MaterialTheme.colors.primary,

                                                            modifier = Modifier)
                                                    }
                                                }
                                               
                                            }

                                            Text(text = "You can change this later in settings.".toUpperCase(),
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
    }
    private fun onPermissionResult(isGranted: Boolean) {
        // Save the user's choice in SharedPreferences
        getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
            .edit()
            .putBoolean(preferenceKey, true)
            .apply()

        // Handle the permission result
        if (isGranted) {
            // Permission granted, navigate to MainActivity
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)

        } else {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)

            // Permission denied, you may want to handle this case
        }

        finish()
    }

}
@Composable
fun ThemedNotificationImage(modifier: Modifier) {
    val isDarkTheme = isSystemInDarkTheme()
    val imageRes = if (isDarkTheme) {
       R.drawable.notification_dark_theme
    } else {
        R.drawable.notification_light_theme
    }

    Image(
        painter = painterResource(id = imageRes),
        contentDescription = null,
        modifier = modifier
    )
}
