package com.sotwtm.ocr.demo

import android.app.Application
import com.sotwtm.util.Log

class OCRApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        Log.logLevel = if (BuildConfig.DEBUG) Log.VERBOSE else Log.NONE
    }
}