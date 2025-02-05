package com.google.mediapipe.examples.facelandmarker

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.util.Log

class FLApplication : Application() {

    companion object {
        @SuppressLint("StaticFieldLeak")
        lateinit var instance: FLApplication
            private set
        @SuppressLint("StaticFieldLeak")
        lateinit var context: Context
            private set
    }

    override fun onCreate() {
        super.onCreate()

        instance = this
        context = applicationContext

        Log.d("FLApplication","$context")
    }


}