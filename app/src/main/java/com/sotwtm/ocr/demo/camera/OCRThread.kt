package com.sotwtm.ocr.demo.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Rect
import android.os.Handler
import com.sotwtm.util.Log
import java.util.concurrent.atomic.AtomicBoolean

/**
 */
class OCRThread
/**
 * Constructor.
 *
 * @param context Application context.
 */
constructor(
    val context: Context,
    _lang: String,
    _allowDigitOnly: Boolean,
    var segMode: Int
) : Thread() {

    private val handler: Handler
    private var textRecognitionHelper: TextRecognitionHelper = TextRecognitionHelper(context)
    private val bitmapChanged: AtomicBoolean

    private var runFlag: Boolean = false
    private var bitmap: Bitmap? = null
    private var regionsListener: TextRegionsListener? = null
    private var textRecognitionListener: TextRecognitionListener? = null
    private var reInitialize: Boolean = false

    init {
        this.bitmapChanged = AtomicBoolean()
        this.handler = Handler()
    }

    var lang: String = _lang
        @Synchronized
        set(value) {
            field = value
            reInitialize = true
        }

    var allowDigitOnly: Boolean = _allowDigitOnly
        @Synchronized
        set(value) {
            field = value
            reInitialize = true
        }

    /**
     * Update image data for recognition.
     *
     * @param bitmap camera frame data.
     */
    fun updateBitmap(bitmap: Bitmap) {
        this.bitmap = bitmap
        bitmapChanged.set(true)
    }

    @Synchronized
    override fun start() {
        this.runFlag = true
        super.start()
    }

    /**
     * Stop thread execution.
     */
    fun cancel() {
        runFlag = false
        this.regionsListener = null
        this.textRecognitionListener = null
    }

    /**
     * Setter for recognized text region updates listener.
     *
     * @param regionsListener Listener for recognized text regions updates.
     */
    fun setRegionsListener(regionsListener: TextRegionsListener?) {
        this.regionsListener = regionsListener
    }

    /**
     * Setter for recognized text updates listener.
     *
     * @param textRecognitionListener Listener for recognized text updates.
     */
    fun setTextRecognitionListener(textRecognitionListener: TextRecognitionListener) {
        this.textRecognitionListener = textRecognitionListener
    }

    /**
     * Perform text recognition.
     */
    override fun run() {
        textRecognitionHelper.prepareTesseract(lang, allowDigitOnly)
        while (runFlag) {
            if (reInitialize) {
                textRecognitionHelper = TextRecognitionHelper(context)
                textRecognitionHelper.prepareTesseract(lang, allowDigitOnly)
                reInitialize = false
                Log.d("Re-initialized OCR")
            }
            if (bitmapChanged.compareAndSet(true, false)) {
                val matrix = Matrix()
                matrix.postRotate(90f)
                val rotatedBitmap = Bitmap
                    .createBitmap(bitmap!!, 0, 0, bitmap!!.width, bitmap!!.height, matrix, true)
                textRecognitionHelper.setBitmap(rotatedBitmap, segMode)
                updateTextRegions()
                updateOCRText()
                rotatedBitmap.recycle()
            }
        }
        textRecognitionHelper.stop()
    }

    private fun updateTextRegions() {
        val listener = this.regionsListener
        if (listener != null) {
            val regions = textRecognitionHelper.textRegions
            handler.post { listener.onTextRegionsRecognized(regions) }

        }
    }

    @Synchronized
    private fun updateOCRText() {
        val listener = this.textRecognitionListener
        if (listener != null) {
            val text = textRecognitionHelper.text
            handler.post { listener.onTextRecognized(text) }
            this@OCRThread.textRecognitionListener = null
        }
    }

    /**
     * Listener for recognized text regions updates.
     */
    interface TextRegionsListener {

        /**
         * Notify about recognized text regions update.
         *
         * @param textRegions list of recognized text regions.
         */
        fun onTextRegionsRecognized(textRegions: List<Rect>)
    }

    /**
     * Listener for recognized text updates.
     */
    interface TextRecognitionListener {

        /**
         * Notify text recognized.
         *
         * @param text Recognized text.
         */
        fun onTextRecognized(text: String)
    }
}
