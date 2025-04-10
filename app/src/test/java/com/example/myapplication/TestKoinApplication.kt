package com.example.myapplication

import android.app.Application
import com.example.myapplication.di.appModule
import com.google.firebase.FirebaseApp
import org.koin.android.ext.koin.androidContext
import org.koin.android.logger.AndroidLogger
import org.koin.core.context.GlobalContext.getOrNull
import org.koin.core.context.GlobalContext.startKoin

class TestApplication2 : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize Firebase to prevent the "not initialized" error.
        FirebaseApp.initializeApp(this)
        if (getOrNull() == null) {
            startKoin {
                // Optionally enable logging for debugging tests.
                AndroidLogger()
                // Provide the Application context.
                androidContext(this@TestApplication2)
                // Use test modules (you can reuse appModule if you want the real dependencies).
                modules(appModule)
            }
        }
    }
}
