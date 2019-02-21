package com.sotwtm.ocr.demo.camera

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.Rect
import com.googlecode.tesseract.android.TessBaseAPI
import com.sotwtm.ocr.demo.BuildConfig
import com.sotwtm.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.*

/**
 *
 */
class TextRecognitionHelper
/**
 * Constructor.
 *
 * @param context Application context.
 */
    (context: Context) {

    private val context: Context = context.applicationContext
    private val tessBaseApi: TessBaseAPI = TessBaseAPI()
    private val sharedPref: SharedPreferences = context.getSharedPreferences("OCR", Context.MODE_PRIVATE)

    /**
     * Get recognized words regions for image.
     *
     * @return List of words regions.
     */
    val textRegions: List<Rect>
        get() {
            val regions = tessBaseApi.words
            val lineRects = ArrayList(regions.boxRects)
            regions.recycle()
            return lineRects
        }

    /**
     * Get recognized text for image.
     *
     * @return Recognized text string.
     */
    val text: String
        get() = tessBaseApi.utF8Text

    /**
     * Initialize tesseract engine.
     *
     * @param language Language code in ISO-639-3 format.
     */
    fun prepareTesseract(
        language: String,
        allowDigitOnly: Boolean
    ) {
        try {
            prepareDirectory(tesseractPath() + TESSERACT_TRAINED_DATA_FOLDER)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        copyTessDataFiles()
        Log.d("AllowDigitOnly: $allowDigitOnly")
        if (allowDigitOnly) {
            tessBaseApi.setVariable(TessBaseAPI.VAR_CHAR_WHITELIST, "0123456789")
        }
        tessBaseApi.init(tesseractPath(), language)
    }

    private fun tesseractPath(): String {
        return context.cacheDir.absolutePath + "/ocr_demo/"
    }

    private fun prepareDirectory(path: String) {

        val dir = File(path)
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                Log.e("ERROR: Creation of directory $path failed, check does Android Manifest have permission to write to external storage.")
            }
        } else {
            Log.i("Created directory $path")
        }
    }

    @Synchronized
    private fun copyTessDataFiles() {
        try {
            val fileList = context.assets.list(TextRecognitionHelper.TESSERACT_TRAINED_DATA_FOLDER)
            if (fileList == null) {
                Log.e("")
                return
            }

            val lastVersion = sharedPref.getInt(KEY_LAST_VERSION_CODE, -1)
            val versionUpdated = lastVersion < BuildConfig.VERSION_CODE

            for (fileName in fileList) {
                val pathToDataFile =
                    tesseractPath() + TextRecognitionHelper.TESSERACT_TRAINED_DATA_FOLDER + "/" + fileName
                if (!File(pathToDataFile).exists() || versionUpdated) {
                    val inputStream =
                        context.assets.open(TextRecognitionHelper.TESSERACT_TRAINED_DATA_FOLDER + "/" + fileName)
                    val out = FileOutputStream(pathToDataFile, false)
                    val buf = ByteArray(1024)
                    var length: Int
                    while (true) {
                        length = inputStream.read(buf)
                        if (length <= 0) break
                        out.write(buf, 0, length)
                    }
                    inputStream.close()
                    out.close()
                    Log.d("Copied " + fileName + "to tessdata")
                }
            }

            if (versionUpdated) {
                sharedPref.edit()
                    .putInt(KEY_LAST_VERSION_CODE, BuildConfig.VERSION_CODE)
                    .apply()
                Log.d("Updated trained data to version : ${BuildConfig.VERSION_CODE}")
            }
        } catch (e: IOException) {
            Log.e("Unable to copy files to tessdata " + e.message)
        }

    }

    /**
     * Set image for recognition.
     *
     * @param bitmap Image data.
     */
    fun setBitmap(bitmap: Bitmap, segMode: Int) {
        tessBaseApi.pageSegMode = segMode
        tessBaseApi.setImage(bitmap)
    }

    /**
     * Clear tesseract data.
     */
    fun stop() {
        tessBaseApi.clear()
    }

    companion object {

        private const val TESSERACT_TRAINED_DATA_FOLDER = "tessdata"
        private const val KEY_LAST_VERSION_CODE = "LastVersionCode"
    }
}
