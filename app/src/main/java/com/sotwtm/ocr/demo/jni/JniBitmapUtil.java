package com.sotwtm.ocr.demo.jni;

import android.graphics.Bitmap;

/**
 * JNI of com_sotwtm_ocr_demo_jni_JniBitmapUtil.cpp
 * */
public class JniBitmapUtil {
    public static native void decode(final Bitmap target, final byte[] source);
}
