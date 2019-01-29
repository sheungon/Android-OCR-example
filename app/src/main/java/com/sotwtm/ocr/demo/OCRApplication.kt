package com.sotwtm.ocr.demo

import android.app.Application
import android.os.Build
import android.webkit.WebView
import com.crashlytics.android.Crashlytics
import com.crashlytics.android.core.CrashlyticsCore
import com.sotwtm.util.Log
import io.fabric.sdk.android.Fabric

/**
 * Application class of OCR Demo app
 * @author sheungon
 * */
class OCRApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        initCrashReporter()

        Log.logLevel = if (BuildConfig.DEBUG) Log.VERBOSE else Log.NONE

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            try {
                WebView.setWebContentsDebuggingEnabled(Log.isDebuggable)
            } catch (e: Exception) {
                Log.e("Error on enabling web debug", e)
            }
        }
    }

    private fun initCrashReporter() {
        val core = CrashlyticsCore.Builder()
            .disabled(!BuildConfig.USE_CRASHLYTICS)
            .build()
        val crashlytics = Crashlytics.Builder()
            .core(core)
            .build()
        Fabric.with(this, crashlytics)
    }
}