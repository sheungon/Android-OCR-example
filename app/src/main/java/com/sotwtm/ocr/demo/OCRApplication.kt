package com.sotwtm.ocr.demo

import android.app.Application
import android.webkit.WebView
import com.sotwtm.util.Log

/**
 * Application class of OCR Demo app
 * @author sheungon
 * */
class OCRApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        Log.logLevel = if (BuildConfig.DEBUG) Log.VERBOSE else Log.NONE

        try {
            WebView.setWebContentsDebuggingEnabled(Log.isDebuggable)
        } catch (e: Exception) {
            Log.e("Error on enabling web debug", e)
        }
    }
}