package com.example.myapplication

import android.app.Application
import com.example.myapplication.di.appModule
import org.koin.core.context.startKoin
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger

class Index : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger()
            androidContext(this@Index)
            modules(appModule)
        }
    }
}
