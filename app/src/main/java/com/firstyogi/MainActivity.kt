package com.firstyogi.dothing

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider
import com.firstyogi.ui.theme.AppJetpackComposeTheme
class MainActivity : ComponentActivity() {
    @SuppressLint("SuspiciousIndentation")
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppJetpackComposeTheme{
                val context = LocalContext.current
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    channelID,
                    "Tasks",
                    NotificationManager.IMPORTANCE_DEFAULT
                )
                val manager: NotificationManager =
                    context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                manager.createNotificationChannel(channel)}
                SetupNavGraph()

            }

        }
    }


}
@Composable
fun ThemedBackground() {
    val isDarkTheme = isSystemInDarkTheme()
    if (isDarkTheme) {
        (LocalView.current.parent as DialogWindowProvider)?.window?.setDimAmount(0.1f)
    } else {
        (LocalView.current.parent as DialogWindowProvider)?.window?.setDimAmount(0.1f)
    }
}



