package com.sotwtm.ocr.demo.main


import android.Manifest
import android.app.ProgressDialog
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.sotwtm.ocr.demo.R
import com.sotwtm.ocr.demo.camera.CameraView
import com.sotwtm.ocr.demo.camera.OCRThread

/**
 */
class RecognitionActivity : AppCompatActivity(), OCRThread.TextRecognitionListener {

    private var cameraView: CameraView? = null
    private var progressDialog: ProgressDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_CALENDAR
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Permission is not granted
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                REQUEST_PERMISSION
            )
        } else {
            init()
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        cameraView = null
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_show_bounds -> {
                item.isChecked = !item.isChecked
                cameraView!!.setShowTextBounds(item.isChecked)
                true
            }
            R.id.action_recognition_enhancement -> {
                item.isChecked = !item.isChecked
                cameraView!!.setRecognitionEnhancement(item.isChecked)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            REQUEST_PERMISSION -> {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                    init()
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Toast.makeText(this, R.string.error_no_permission, Toast.LENGTH_LONG).show()
                    finish()
                }
            }
        }
    }

    private fun init() {
        setContentView(R.layout.activity_recognition)
        val actionBar = supportActionBar
        actionBar!!.setDisplayShowHomeEnabled(false)

        cameraView = findViewById(R.id.camera_surface)
        cameraView!!.setShowTextBounds(true)
        cameraView!!.setRecognitionEnhancement(true)
        progressDialog = ProgressDialog(this)
        progressDialog!!.setMessage(getString(R.string.progress_message_ocr))
    }

    /**
     * Perform OCR for camera frame.
     *
     * @param view Button instance.
     */
    fun makeOCR(view: View) {
        progressDialog!!.show()
        cameraView!!.makeOCR(this)
    }

    private fun showOCRDialog(recognizedText: String) {
        val dialogBuilder = AlertDialog.Builder(this)
        dialogBuilder.setTitle(R.string.dialog_ocr_title)
        dialogBuilder.setPositiveButton(R.string.dialog_ocr_button) { dialog, which ->
            // empty
        }
        dialogBuilder.setMessage(recognizedText)
        dialogBuilder.create().show()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.recognition, menu)
        return true
    }

    override fun onTextRecognized(text: String) {
        progressDialog!!.dismiss()
        showOCRDialog(text)
    }

    companion object {
        private val REQUEST_PERMISSION = 1000
    }
}
