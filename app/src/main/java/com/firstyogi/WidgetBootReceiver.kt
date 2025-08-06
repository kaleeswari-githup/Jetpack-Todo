package com.firstyogi

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class WidgetBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Reinitialize Firebase listener on boot or app update
        TodoWidget.initializeFirebaseListener(context)
    }
}