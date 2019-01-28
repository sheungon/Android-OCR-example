package com.sotwtm.ocr.demo.camera;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import com.googlecode.leptonica.android.Pixa;
import com.googlecode.tesseract.android.TessBaseAPI;
import com.sotwtm.util.Log;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class TextRecognitionHelper {

    private static final String TESSERACT_TRAINED_DATA_FOLDER = "tessdata";

    private final Context context;
    private final TessBaseAPI tessBaseApi;

    /**
     * Constructor.
     *
     * @param context Application context.
     */
    public TextRecognitionHelper(final Context context) {
        this.context = context.getApplicationContext();
        this.tessBaseApi = new TessBaseAPI();
    }

    /**
     * Initialize tesseract engine.
     *
     * @param language Language code in ISO-639-3 format.
     */
    public void prepareTesseract(final String language) {
        try {
            prepareDirectory(tesseractPath() + TESSERACT_TRAINED_DATA_FOLDER);
        } catch (Exception e) {
            e.printStackTrace();
        }

        copyTessDataFiles();
        tessBaseApi.init(tesseractPath(), language);
        tessBaseApi.setVariable(TessBaseAPI.VAR_CHAR_WHITELIST, "0123456789");
    }

    private String tesseractPath() {
        return context.getCacheDir().getAbsolutePath() + "/ocr_demo/";
    }

    private void prepareDirectory(String path) {

        File dir = new File(path);
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                Log.e("ERROR: Creation of directory " + path + " failed, check does Android Manifest have permission to write to external storage.");
            }
        } else {
            Log.i("Created directory " + path);
        }
    }

    private void copyTessDataFiles() {
        try {
            String fileList[] = context.getAssets().list(TextRecognitionHelper.TESSERACT_TRAINED_DATA_FOLDER);
            if (fileList == null) {
                Log.e("");
                return;
            }

            for (String fileName : fileList) {
                String pathToDataFile = tesseractPath() + TextRecognitionHelper.TESSERACT_TRAINED_DATA_FOLDER + "/" + fileName;
                if (!(new File(pathToDataFile)).exists()) {
                    InputStream in = context.getAssets().open(TextRecognitionHelper.TESSERACT_TRAINED_DATA_FOLDER + "/" + fileName);
                    OutputStream out = new FileOutputStream(pathToDataFile);
                    byte[] buf = new byte[1024];
                    int length;
                    while ((length = in.read(buf)) > 0) {
                        out.write(buf, 0, length);
                    }
                    in.close();
                    out.close();
                    Log.d("Copied " + fileName + "to tessdata");
                }
            }
        } catch (IOException e) {
            Log.e("Unable to copy files to tessdata " + e.getMessage());
        }
    }

    /**
     * Set image for recognition.
     *
     * @param bitmap Image data.
     */
    public void setBitmap(final Bitmap bitmap) {
        tessBaseApi.setPageSegMode(TessBaseAPI.PageSegMode.PSM_SINGLE_BLOCK);
        tessBaseApi.setImage(bitmap);
    }

    /**
     * Get recognized words regions for image.
     *
     * @return List of words regions.
     */
    public List<Rect> getTextRegions() {
        Pixa regions = tessBaseApi.getWords();
        List<Rect> lineRects = new ArrayList<>(regions.getBoxRects());
        regions.recycle();
        return lineRects;
    }

    /**
     * Get recognized text for image.
     *
     * @return Recognized text string.
     */
    public String getText() {
        return tessBaseApi.getUTF8Text();
    }

    /**
     * Clear tesseract data.
     */
    public void stop() {
        tessBaseApi.clear();
    }
}
