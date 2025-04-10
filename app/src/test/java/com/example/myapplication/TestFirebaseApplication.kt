package com.example.myapplication

import android.app.Application
import com.google.firebase.FirebaseApp

class TestFirebaseApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize Firebase to prevent the "not initialized" error.
        FirebaseApp.initializeApp(this)
    }
}
