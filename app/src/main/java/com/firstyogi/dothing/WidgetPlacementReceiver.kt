package com.firstyogi.dothing

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.firstyogi.TodoWidget

class WidgetPlacementReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when(intent.action) {
            AppWidgetManager.ACTION_APPWIDGET_ENABLED,
            AppWidgetManager.ACTION_APPWIDGET_UPDATE -> {
                TodoWidget.initializeFirebaseListener(context)
            }
            AppWidgetManager.ACTION_APPWIDGET_DELETED -> {
                // Optional: handle cleanup if needed
            }
        }
    }
}