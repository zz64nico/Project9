package com.example.note_kotlin

import android.app.Application
import androidx.room.Room.databaseBuilder
import com.google.firebase.FirebaseApp

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this

        FirebaseApp.initializeApp(this);
    }

    companion object {
        var instance: MainApplication? = null
            private set
    }
}