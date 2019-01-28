package com.sotwtm.ocr.demo;


import android.Manifest;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.sotwtm.ocr.demo.camera.CameraView;
import com.sotwtm.ocr.demo.camera.OCRThread;
import org.jetbrains.annotations.NotNull;

/**
 */
public class RecognitionActivity extends AppCompatActivity implements OCRThread.TextRecognitionListener {

    private static final int REQUEST_PERMISSION = 1000;

    private CameraView cameraView;
    private ProgressDialog progressDialog;

    @SuppressWarnings("deprecation")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    REQUEST_PERMISSION);
        } else {
            init();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        cameraView = null;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_show_bounds:
                item.setChecked(!item.isChecked());
                cameraView.setShowTextBounds(item.isChecked());
                return true;
            case R.id.action_recognition_enhancement:
                item.setChecked(!item.isChecked());
                cameraView.setRecognitionEnhancement(item.isChecked());
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NotNull String permissions[],
                                           @NotNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_PERMISSION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                    init();
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Toast.makeText(this, R.string.error_no_permission, Toast.LENGTH_LONG).show();
                    finish();
                }
            }
        }
    }

    private void init() {
        setContentView(R.layout.activity_recognition);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowHomeEnabled(false);

        cameraView = findViewById(R.id.camera_surface);
        cameraView.setShowTextBounds(true);
        cameraView.setRecognitionEnhancement(true);
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage(getString(R.string.progress_message_ocr));
    }

    /**
     * Perform OCR for camera frame.
     *
     * @param view Button instance.
     */
    public void makeOCR(final View view) {
        progressDialog.show();
        cameraView.makeOCR(this);
    }

    private void showOCRDialog(final String recognizedText) {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        dialogBuilder.setTitle(R.string.dialog_ocr_title);
        dialogBuilder.setPositiveButton(R.string.dialog_ocr_button, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(final DialogInterface dialog, final int which) {
                // empty
            }
        });
        dialogBuilder.setMessage(recognizedText);
        dialogBuilder.create().show();
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.recognition, menu);
        return true;
    }

    @Override
    public void onTextRecognized(final String text) {
        progressDialog.dismiss();
        showOCRDialog(text);
    }
}
