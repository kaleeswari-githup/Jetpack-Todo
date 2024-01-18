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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.firstyogi.ui.theme.AppJetpackComposeTheme

class NotificationPermissionActivity : ComponentActivity() {
    private val preferenceKey = "notification_permission_choice"

    @SuppressLint("SuspiciousIndentation")
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
                            Box (modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    color = androidx.compose.material.MaterialTheme.colors.background
                                ),

                                ){
                                ThemedGridImage()

                                Box(modifier = Modifier.fillMaxSize()) {
                                    Column(modifier = Modifier.fillMaxSize(),
                                        horizontalAlignment = Alignment.CenterHorizontally) {
                                        ThemedNotificationImage(modifier = Modifier.padding(top = 110.dp))

                                        Text(text = "ALLOW NOTIFICATIONS",
                                            color = androidx.compose.material.MaterialTheme.colors.secondary,
                                            modifier = Modifier.padding(top = 80.dp),
                                            fontFamily = interDisplayFamily,
                                            fontSize = 24.sp,
                                            fontWeight = FontWeight.Medium,
                                            textAlign = TextAlign.Center
                                        )
                                        Text(text = "Get task notifications only—no unnecessary alerts, just what you need.",
                                            color = androidx.compose.material.MaterialTheme.colors.secondary.copy(alpha = 0.75f),
                                            modifier = Modifier.padding(top = 16.dp, start = 24.dp,end = 24.dp),
                                            fontFamily = interDisplayFamily,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Medium,
                                            textAlign = TextAlign.Center,
                                            lineHeight = 24.sp)
                                        Box(modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = 24.dp, end = 24.dp, top = 129.dp)
                                            .height(72.dp)
                                            .background(
                                                color = androidx.compose.material.MaterialTheme.colors.secondary,
                                                shape = RoundedCornerShape(64.dp)
                                            )
                                            .clickable(indication = null,
                                                interactionSource = remember { MutableInteractionSource() }) {
                                                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                            },
                                            contentAlignment = Alignment.Center
                                        ){
                                            ButtonTextWhiteTheme(text = ("Allow Notifications").uppercase(),
                                                color = androidx.compose.material.MaterialTheme.colors.primary
                                            )
                                        }
                                        Box(modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = 24.dp, end = 24.dp, top = 24.dp)
                                            .height(72.dp)
                                            .background(
                                                color = androidx.compose.material.MaterialTheme.colors.primary,
                                                shape = RoundedCornerShape(64.dp)
                                            )
                                            .clickable(indication = null,
                                                interactionSource = remember { MutableInteractionSource() }) {
                                                val intent = Intent(context,MainActivity::class.java)
                                                context.startActivity(intent)
                                                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)

                                                finish()
                                            },
                                            contentAlignment = Alignment.Center
                                        ){
                                            ButtonTextWhiteTheme(text = ("Not Now").uppercase(),
                                                color = androidx.compose.material.MaterialTheme.colors.secondary,)
                                        }
                                        Text(text = "You can update in settings anytime.",
                                            fontFamily = interDisplayFamily,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = androidx.compose.material.MaterialTheme.colors.secondary.copy(alpha = 0.75f),
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.padding(top = 41.dp)
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
       R.drawable.notification_image_dark
    } else {
        R.drawable.notification_image_light
    }

    Image(
        painter = painterResource(id = imageRes),
        contentDescription = null,
        modifier = modifier
    )
}
