package com.sotwtm.ocr.demo.main

import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.databinding.ObservableInt
import com.googlecode.tesseract.android.TessBaseAPI

class OcrOptions() {
    val showTextBounds = ObservableBoolean(true)
    val recognitionEnhancement = ObservableBoolean(true)
    val allowedDigitOnly = ObservableBoolean(true)
    val selectedTrainedData = ObservableField("digits")
    val segMode = ObservableInt(TessBaseAPI.PageSegMode.PSM_SINGLE_BLOCK)

    fun setSelectedTrainedData(selected: Any?) {
        (selected as? String)?.let {
            selectedTrainedData.set(it)
        }
    }

    fun setSegMode(mode: Any?) {
        (mode as? Int)?.let {
            segMode.set(it)
        } ?: (mode as? String)?.toIntOrNull()?.let {
            segMode.set(it)
        }
    }
}