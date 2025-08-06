package com.firstyogi

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.core.net.toUri

class OpenAddTaskAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = "app://dothing/add_task/false".toUri() // Deep link to AddDaskScreen
            setPackage(context.packageName) // Target your app
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) // Required when starting from widget
        }
        context.startActivity(intent)
    }
}