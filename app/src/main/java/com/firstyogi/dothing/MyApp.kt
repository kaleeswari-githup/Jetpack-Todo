package com.firstyogi.dothing

import android.app.Application
import androidx.work.Configuration
import androidx.work.WorkManager
import com.firstyogi.TodoWidget
import com.google.firebase.FirebaseApp
import com.google.firebase.database.FirebaseDatabase

class MyApp: Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseDatabase.getInstance().setPersistenceEnabled(true)
        FirebaseApp.initializeApp(this)
        TodoWidget.initializeFirebaseListener(this)
       PeriodicTaskUpdater.enqueue(this)
//       WorkManager.initialize(this, Configuration.Builder().build())
    }
}