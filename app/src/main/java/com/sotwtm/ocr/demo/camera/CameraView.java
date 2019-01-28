package com.sotwtm.ocr.demo.camera;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.*;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import com.sotwtm.ocr.demo.R;
import com.sotwtm.ocr.demo.jni.JniBitmapUtil;
import com.sotwtm.util.Log;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 */
public class CameraView extends SurfaceView implements Camera.PreviewCallback, SurfaceHolder.Callback,
        OCRThread.TextRegionsListener {

    private static final int DEFAULT_RECOG_HEIGHT_DP = 0;
    private static final int DEFAULT_RECOG_INTERVAL_MS = 200;

    private boolean recognitionEnhancement;

    private int yOffset = 0;
    private int recongHeightPixel;
    private int recongInterval;
    private long lastRecongTime = 0;

    private final List<Rect> regions = new ArrayList<>();

    private Camera mCamera;
    private byte[] mVideoSource;
    private Bitmap cameraImgBuf;
    private OCRThread ocrThread;

    private Paint focusPaint;
    private Rect focusRect;

    private Paint textBoundPaint;
    private Paint nonRecognizablePaint;

    private float horizontalRectRation;
    private float verticalRectRation;

    static {
        System.loadLibrary("livecamera");

        if (!OpenCVLoader.initDebug()) {
            // Handle initialization error
            Log.wtf("Cannot load OpenCV");
        }
    }

    public CameraView(final Context context) {
        this(context, null);
    }

    public CameraView(final Context context, final AttributeSet attributes) {
        super(context, attributes);
        getHolder().addCallback(this);
        setWillNotDraw(false);
        focusPaint = new Paint();
        focusPaint.setColor(0xeed7d7d7);
        focusPaint.setStyle(Paint.Style.STROKE);
        focusPaint.setStrokeWidth(2);
        focusRect = new Rect(0, 0, 0, 0);

        textBoundPaint = new Paint();
        textBoundPaint.setColor(0xDDFF0000);
        textBoundPaint.setStyle(Paint.Style.STROKE);
        textBoundPaint.setStrokeWidth(4);

        nonRecognizablePaint = new Paint();
        nonRecognizablePaint.setColor(0x77000000);
        nonRecognizablePaint.setStyle(Paint.Style.FILL);

        ocrThread = new OCRThread(context);
        horizontalRectRation = 1.0f;
        verticalRectRation = 1.0f;

        TypedArray typedArray = context.getTheme()
                .obtainStyledAttributes(attributes, R.styleable.CameraView, 0, 0);
        try {
            recongHeightPixel = typedArray.getDimensionPixelSize(R.styleable.CameraView_recognizableHeight,
                    (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, DEFAULT_RECOG_HEIGHT_DP, context.getResources().getDisplayMetrics()));
            Log.d("recongHeightPixel: " + recongHeightPixel);
            recongInterval = typedArray.getInt(R.styleable.CameraView_recognizeInterval, DEFAULT_RECOG_INTERVAL_MS);
            if (recongInterval <= 0) {
                recongInterval = DEFAULT_RECOG_INTERVAL_MS;
            }
            Log.d("recongInterval: " + recongInterval);
            recognitionEnhancement = typedArray.getBoolean(R.styleable.CameraView_recognitionEnhancement, false);
        } finally {
            typedArray.recycle();
        }
    }

    public void setShowTextBounds(final boolean show) {
        regions.clear();
        ocrThread.setRegionsListener(show ? this : null);
        invalidate();
    }

    public void makeOCR(final OCRThread.TextRecognitionListener listener) {
        ocrThread.setTextRecognitionListener(listener);
    }


    @Override
    protected void onDraw(final Canvas canvas) {
        if (focusRect.width() > 0) {
            canvas.drawRect(focusRect, focusPaint);
        }
        if (mCamera != null) {
            mCamera.addCallbackBuffer(mVideoSource);
            drawTextBounds(canvas);
            drawNonRecognizableArea(canvas);
        }
    }

    private void drawTextBounds(final Canvas canvas) {
        for (Rect region : regions) {
            canvas.drawRect(region.left * horizontalRectRation, (yOffset + region.top) * verticalRectRation,
                    region.right * horizontalRectRation, (yOffset + region.bottom) * verticalRectRation, textBoundPaint);
        }
    }

    private void drawNonRecognizableArea(final Canvas canvas) {
        if (recongHeightPixel <= 0) return;
        canvas.drawRect(0,
                0,
                canvas.getWidth(),
                yOffset * verticalRectRation,
                nonRecognizablePaint);
        canvas.drawRect(0,
                (yOffset + recongHeightPixel) * verticalRectRation,
                canvas.getWidth(),
                canvas.getHeight(),
                nonRecognizablePaint);
    }

    @Override
    public void onPreviewFrame(final byte[] bytes, final Camera camera) {

        //if (System.currentTimeMillis() - lastRecongTime < recongInterval) return;

        JniBitmapUtil.decode(cameraImgBuf, bytes);

        Mat imageMat = new Mat();
        Utils.bitmapToMat(cameraImgBuf, imageMat);

        // Crop image
        org.opencv.core.Rect roi;
        if (recongHeightPixel > 0) {
            roi = new org.opencv.core.Rect(yOffset, 0, recongHeightPixel, imageMat.rows());
        } else {
            roi = new org.opencv.core.Rect(yOffset, 0, imageMat.cols(), imageMat.rows());
        }
        imageMat = new Mat(imageMat, roi);

        // Turn image to black and white
        if (recognitionEnhancement) {
            Imgproc.cvtColor(imageMat, imageMat, Imgproc.COLOR_BGR2GRAY);
            Imgproc.GaussianBlur(imageMat, imageMat, new org.opencv.core.Size(3, 3), 0);
            Imgproc.threshold(imageMat, imageMat, 0, 255, Imgproc.THRESH_OTSU);
        }

        Bitmap temp = Bitmap.createBitmap(imageMat.cols(), imageMat.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(imageMat, temp);

        ocrThread.updateBitmap(temp);
        lastRecongTime = System.currentTimeMillis();
    }

    @Override
    public void surfaceCreated(final SurfaceHolder surfaceHolder) {
        try {
            mCamera = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK);
            mCamera.setDisplayOrientation(90);
            mCamera.setPreviewDisplay(surfaceHolder);
            mCamera.setPreviewCallbackWithBuffer(this);
            startOcrThread();
        } catch (IOException eIOException) {
            mCamera.release();
            mCamera = null;
            throw new IllegalStateException();
        }
    }

    public int getXOffset() {
        return yOffset;
    }

    private void startOcrThread() {
        ocrThread.start();
    }

    @Override
    public void surfaceChanged(final SurfaceHolder surfaceHolder, final int format, final int width, final int height) {
        mCamera.stopPreview();
        Size lSize = findBestResolution();
        updateTextRectsRatio(width, height, lSize);
        PixelFormat lPixelFormat = new PixelFormat();
        PixelFormat.getPixelFormatInfo(mCamera.getParameters().getPreviewFormat(), lPixelFormat);
        int lSourceSize = lSize.width * lSize.height * lPixelFormat.bitsPerPixel / 8;
        mVideoSource = new byte[lSourceSize];
        if (lSize.height < recongHeightPixel) {
            recongHeightPixel = lSize.height;
        }
        yOffset = recongHeightPixel > 0 ? (lSize.width - recongHeightPixel) / 2 : 0;
        cameraImgBuf = Bitmap.createBitmap(lSize.width, lSize.height, Bitmap.Config.ARGB_8888);
        Camera.Parameters lParameters = mCamera.getParameters();
        lParameters.setPreviewSize(lSize.width, lSize.height);
        mCamera.setParameters(lParameters);
        mCamera.addCallbackBuffer(mVideoSource);
        mCamera.startPreview();
    }

    private Size findBestResolution() {
        List<Size> lSizes = mCamera.getParameters().getSupportedPreviewSizes();
        Size lSelectedSize = mCamera.new Size(0, 0);
        for (Size lSize : lSizes) {
            if ((lSize.width >= lSelectedSize.width) && (lSize.height >= lSelectedSize.height)) {
                lSelectedSize = lSize;
            }
        }
        if ((lSelectedSize.width == 0) || (lSelectedSize.height == 0)) {
            lSelectedSize = lSizes.get(0);
        }
        return lSelectedSize;
    }

    private void updateTextRectsRatio(final int width, final int height, final Size cameraSize) {
        verticalRectRation = ((float) height) / cameraSize.width;
        horizontalRectRation = ((float) width) / cameraSize.height;
    }

    @Override
    public void surfaceDestroyed(final SurfaceHolder surfaceHolder) {
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
            mVideoSource = null;
            cameraImgBuf = null;
            stopOcrThread();
        }
    }

    private void stopOcrThread() {
        boolean retry = true;
        ocrThread.cancel();
        ocrThread.setRegionsListener(null);
        while (retry) {
            try {
                ocrThread.join();
                retry = false;
            } catch (InterruptedException e) {
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            float x = event.getX();
            float y = event.getY();

            Rect touchRect = new Rect(
                    (int) (x - 100),
                    (int) (y - 100),
                    (int) (x + 100),
                    (int) (y + 100));

            final Rect targetFocusRect = new Rect(
                    touchRect.left * 2000 / this.getWidth() - 1000,
                    touchRect.top * 2000 / this.getHeight() - 1000,
                    touchRect.right * 2000 / this.getWidth() - 1000,
                    touchRect.bottom * 2000 / this.getHeight() - 1000);

            doTouchFocus(targetFocusRect);
            focusRect = touchRect;
            invalidate();

            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {

                @Override
                public void run() {
                    focusRect = new Rect(0, 0, 0, 0);
                    invalidate();
                }
            }, 1000);
        }
        return false;
    }

    private void doTouchFocus(final Rect tfocusRect) {
        try {
            final List<Camera.Area> focusList = new ArrayList<Camera.Area>();
            Camera.Area focusArea = new Camera.Area(tfocusRect, 1000);
            focusList.add(focusArea);

            Camera.Parameters para = mCamera.getParameters();
            para.setFocusAreas(focusList);
            para.setMeteringAreas(focusList);
            mCamera.setParameters(para);
            mCamera.autoFocus(myAutoFocusCallback);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * AutoFocus callback
     */
    Camera.AutoFocusCallback myAutoFocusCallback = new Camera.AutoFocusCallback() {

        @Override
        public void onAutoFocus(boolean arg0, Camera arg1) {
            if (arg0) {
                mCamera.cancelAutoFocus();
            }
        }
    };

    /**
     * {@inheritDoc}
     */
    @Override
    public void onTextRegionsRecognized(final List<Rect> textRegions) {
        regions.clear();
        regions.addAll(textRegions);
        invalidate();
    }

    public void setRecognitionEnhancement(boolean checked) {
        recognitionEnhancement = checked;
    }
}
