package com.sotwtm.ocr.demo.camera

import android.content.Context
import android.graphics.*
import android.hardware.Camera
import android.hardware.Camera.Size
import android.os.Handler
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.sotwtm.ocr.demo.R
import com.sotwtm.ocr.demo.jni.JniBitmapUtil
import com.sotwtm.util.Log
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc
import java.io.IOException
import java.util.*

/**
 */
class CameraView @JvmOverloads constructor(context: Context, attributes: AttributeSet? = null) :
    SurfaceView(context, attributes), Camera.PreviewCallback, SurfaceHolder.Callback, OCRThread.TextRegionsListener {

    private var recognitionEnhancement: Boolean = false

    var xOffset = 0
        private set
    private var recongHeightPixel: Int = 0
    private var recongInterval: Int = 0
    private var lastRecongTime: Long = 0

    private val regions = ArrayList<Rect>()

    private var mCamera: Camera? = null
    private var mVideoSource: ByteArray? = null
    private var cameraImgBuf: Bitmap? = null
    private val ocrThread: OCRThread

    private val focusPaint: Paint
    private var focusRect: Rect? = null

    private val textBoundPaint: Paint
    private val nonRecognizablePaint: Paint

    private var horizontalRectRation: Float = 0.toFloat()
    private var verticalRectRation: Float = 0.toFloat()

    /**
     * AutoFocus callback
     */
    internal var myAutoFocusCallback: Camera.AutoFocusCallback = Camera.AutoFocusCallback { arg0, arg1 ->
        if (arg0) {
            mCamera!!.cancelAutoFocus()
        }
    }

    init {
        holder.addCallback(this)
        setWillNotDraw(false)

        focusPaint = Paint()
        focusPaint.color = -0x11282829
        focusPaint.style = Paint.Style.STROKE
        focusPaint.strokeWidth = 2f
        focusRect = Rect(0, 0, 0, 0)

        textBoundPaint = Paint()
        textBoundPaint.color = -0x22010000
        textBoundPaint.style = Paint.Style.STROKE
        textBoundPaint.strokeWidth = 4f

        nonRecognizablePaint = Paint()
        nonRecognizablePaint.color = 0x77000000
        nonRecognizablePaint.style = Paint.Style.FILL

        ocrThread = OCRThread(context)
        horizontalRectRation = 1.0f
        verticalRectRation = 1.0f

        val typedArray = context.theme
            .obtainStyledAttributes(attributes, R.styleable.CameraView, 0, 0)
        try {
            recongHeightPixel = typedArray.getDimensionPixelSize(
                R.styleable.CameraView_recognizableHeight,
                TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    DEFAULT_RECOG_HEIGHT_DP.toFloat(),
                    context.resources.displayMetrics
                ).toInt()
            )
            Log.d("recongHeightPixel: $recongHeightPixel")
            recongInterval = typedArray.getInt(R.styleable.CameraView_recognizeInterval, DEFAULT_RECOG_INTERVAL_MS)
            if (recongInterval <= 0) {
                recongInterval = DEFAULT_RECOG_INTERVAL_MS
            }
            Log.d("recongInterval: $recongInterval")
            recognitionEnhancement = typedArray.getBoolean(R.styleable.CameraView_recognitionEnhancement, false)
        } finally {
            typedArray.recycle()
        }
    }

    fun setShowTextBounds(show: Boolean) {
        regions.clear()
        ocrThread.setRegionsListener(if (show) this else null)
        invalidate()
    }

    fun makeOCR(listener: OCRThread.TextRecognitionListener) {
        ocrThread.setTextRecognitionListener(listener)
    }


    override fun onDraw(canvas: Canvas) {
        if (focusRect!!.width() > 0) {
            canvas.drawRect(focusRect!!, focusPaint)
        }
        if (mCamera != null) {
            mCamera!!.addCallbackBuffer(mVideoSource)
            drawTextBounds(canvas)
            drawNonRecognizableArea(canvas)
        }
    }

    private fun drawTextBounds(canvas: Canvas) {
        for (region in regions) {
            canvas.drawRect(
                region.left * horizontalRectRation, (xOffset + region.top) * verticalRectRation,
                region.right * horizontalRectRation, (xOffset + region.bottom) * verticalRectRation, textBoundPaint
            )
        }
    }

    private fun drawNonRecognizableArea(canvas: Canvas) {
        if (recongHeightPixel <= 0) return
        canvas.drawRect(
            0f,
            0f,
            canvas.width.toFloat(),
            xOffset * verticalRectRation,
            nonRecognizablePaint
        )
        canvas.drawRect(
            0f,
            (xOffset + recongHeightPixel) * verticalRectRation,
            canvas.width.toFloat(),
            canvas.height.toFloat(),
            nonRecognizablePaint
        )
    }

    override fun onPreviewFrame(bytes: ByteArray, camera: Camera) {

        //if (System.currentTimeMillis() - lastRecongTime < recongInterval) return;

        JniBitmapUtil.decode(cameraImgBuf, bytes)
        val imgBuf = cameraImgBuf ?: return

        var imageMat = Mat()
        Utils.bitmapToMat(imgBuf, imageMat)

        // Crop image
        val roi: org.opencv.core.Rect
        if (recongHeightPixel > 0) {
            roi = org.opencv.core.Rect(xOffset, 0, recongHeightPixel, imageMat.rows())
        } else {
            roi = org.opencv.core.Rect(xOffset, 0, imageMat.cols(), imageMat.rows())
        }
        imageMat = Mat(imageMat, roi)

        // Turn image to black and white
        if (recognitionEnhancement) {
            Imgproc.cvtColor(imageMat, imageMat, Imgproc.COLOR_BGR2GRAY)
            Imgproc.GaussianBlur(imageMat, imageMat, org.opencv.core.Size(3.0, 3.0), 0.0)
            Imgproc.threshold(imageMat, imageMat, 0.0, 255.0, Imgproc.THRESH_OTSU)
        }

        val temp = Bitmap.createBitmap(imageMat.cols(), imageMat.rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(imageMat, temp)

        ocrThread.updateBitmap(temp)
        lastRecongTime = System.currentTimeMillis()
    }

    override fun surfaceCreated(surfaceHolder: SurfaceHolder) {
        try {
            mCamera = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK)
            mCamera!!.setDisplayOrientation(90)
            mCamera!!.setPreviewDisplay(surfaceHolder)
            mCamera!!.setPreviewCallbackWithBuffer(this)
            startOcrThread()
        } catch (eIOException: IOException) {
            mCamera!!.release()
            mCamera = null
            throw IllegalStateException()
        }

    }

    private fun startOcrThread() {
        ocrThread.start()
    }

    override fun surfaceChanged(surfaceHolder: SurfaceHolder, format: Int, width: Int, height: Int) {
        mCamera!!.stopPreview()
        val lSize = findBestResolution()
        updateTextRectsRatio(width, height, lSize)
        val lPixelFormat = PixelFormat()
        PixelFormat.getPixelFormatInfo(mCamera!!.parameters.previewFormat, lPixelFormat)
        val lSourceSize = lSize.width * lSize.height * lPixelFormat.bitsPerPixel / 8
        mVideoSource = ByteArray(lSourceSize)
        if (lSize.height < recongHeightPixel) {
            recongHeightPixel = lSize.height
        }
        xOffset = if (recongHeightPixel > 0) (lSize.width - recongHeightPixel) / 2 else 0
        cameraImgBuf = Bitmap.createBitmap(lSize.width, lSize.height, Bitmap.Config.ARGB_8888)
        val lParameters = mCamera!!.parameters
        lParameters.setPreviewSize(lSize.width, lSize.height)
        mCamera!!.parameters = lParameters
        mCamera!!.addCallbackBuffer(mVideoSource)
        mCamera!!.startPreview()
    }

    private fun findBestResolution(): Size {
        val lSizes = mCamera!!.parameters.supportedPreviewSizes
        var lSelectedSize = mCamera!!.Size(0, 0)
        for (lSize in lSizes) {
            if (lSize.width >= lSelectedSize.width && lSize.height >= lSelectedSize.height) {
                lSelectedSize = lSize
            }
        }
        if (lSelectedSize.width == 0 || lSelectedSize.height == 0) {
            lSelectedSize = lSizes[0]
        }
        return lSelectedSize
    }

    private fun updateTextRectsRatio(width: Int, height: Int, cameraSize: Size) {
        verticalRectRation = height.toFloat() / cameraSize.width
        horizontalRectRation = width.toFloat() / cameraSize.height
    }

    override fun surfaceDestroyed(surfaceHolder: SurfaceHolder) {
        if (mCamera != null) {
            mCamera!!.stopPreview()
            mCamera!!.release()
            mCamera = null
            mVideoSource = null
            cameraImgBuf = null
            stopOcrThread()
        }
    }

    private fun stopOcrThread() {
        var retry = true
        ocrThread.cancel()
        ocrThread.setRegionsListener(null)
        while (retry) {
            try {
                ocrThread.join()
                retry = false
            } catch (e: InterruptedException) {
            }

        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            val x = event.x
            val y = event.y

            val touchRect = Rect(
                (x - 100).toInt(),
                (y - 100).toInt(),
                (x + 100).toInt(),
                (y + 100).toInt()
            )

            val targetFocusRect = Rect(
                touchRect.left * 2000 / this.width - 1000,
                touchRect.top * 2000 / this.height - 1000,
                touchRect.right * 2000 / this.width - 1000,
                touchRect.bottom * 2000 / this.height - 1000
            )

            doTouchFocus(targetFocusRect)
            focusRect = touchRect
            invalidate()

            val handler = Handler()
            handler.postDelayed({
                focusRect = Rect(0, 0, 0, 0)
                invalidate()
            }, 1000)
        }
        return false
    }

    private fun doTouchFocus(tfocusRect: Rect) {
        try {
            val focusList = ArrayList<Camera.Area>()
            val focusArea = Camera.Area(tfocusRect, 1000)
            focusList.add(focusArea)

            val para = mCamera!!.parameters
            para.focusAreas = focusList
            para.meteringAreas = focusList
            mCamera!!.parameters = para
            mCamera!!.autoFocus(myAutoFocusCallback)
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    /**
     * {@inheritDoc}
     */
    override fun onTextRegionsRecognized(textRegions: List<Rect>) {
        regions.clear()
        regions.addAll(textRegions)
        invalidate()
    }

    fun setRecognitionEnhancement(checked: Boolean) {
        recognitionEnhancement = checked
    }

    companion object {

        private val DEFAULT_RECOG_HEIGHT_DP = 0
        private val DEFAULT_RECOG_INTERVAL_MS = 200

        init {
            System.loadLibrary("livecamera")

            if (!OpenCVLoader.initDebug()) {
                // Handle initialization error
                Log.wtf("Cannot load OpenCV")
            }
        }
    }
}
