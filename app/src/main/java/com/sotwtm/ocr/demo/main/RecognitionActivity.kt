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
import androidx.core.view.size
import androidx.databinding.DataBindingUtil
import com.sotwtm.ocr.demo.R
import com.sotwtm.ocr.demo.camera.CameraView
import com.sotwtm.ocr.demo.camera.OCRThread
import com.sotwtm.ocr.demo.databinding.ActivityRecognitionBinding
import com.sotwtm.ocr.demo.option.SegModeBottomSheetDialogFragment
import com.sotwtm.ocr.demo.option.TrainedDataBottomSheetDialogFragment
import com.sotwtm.util.Log

/**
 */
class RecognitionActivity : AppCompatActivity(), OCRThread.TextRecognitionListener {

    private val ocrOptions = OcrOptions()
    private var cameraView: CameraView? = null
    private var progressDialog: ProgressDialog? = null
    private var dataBinding: ActivityRecognitionBinding? = null

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
        dataBinding?.unbind()
        dataBinding = null
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        for (i in 0 until (menu?.size ?: 0)) {
            menu?.getItem(i)?.let {
                when (it.itemId) {
                    R.id.action_show_bounds -> it.isChecked = ocrOptions.showTextBounds.get()
                    R.id.action_recognition_enhancement -> it.isChecked = ocrOptions.recognitionEnhancement.get()
                    R.id.action_allow_digit_only -> it.isChecked = ocrOptions.allowedDigitOnly.get()
                }
            }
        }
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        Log.d("onOptionsItemSelected : $item")
        return when (item.itemId) {
            R.id.action_show_bounds -> {
                item.isChecked = !item.isChecked
                ocrOptions.showTextBounds.set(item.isChecked)
                true
            }
            R.id.action_recognition_enhancement -> {
                item.isChecked = !item.isChecked
                ocrOptions.recognitionEnhancement.set(item.isChecked)
                true
            }
            R.id.action_allow_digit_only -> {
                item.isChecked = !item.isChecked
                ocrOptions.allowedDigitOnly.set(item.isChecked)
                true
            }
            R.id.action_recognition_mode -> {
                // show recognition mode selector
                val fragment = SegModeBottomSheetDialogFragment()
                fragment.ocrOptions = ocrOptions
                fragment.show(supportFragmentManager, null)
                true
            }
            R.id.action_trained_data -> {
                // show trained data selector
                val fragment = TrainedDataBottomSheetDialogFragment()
                fragment.ocrOptions = ocrOptions
                fragment.show(supportFragmentManager, null)
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
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
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
        dataBinding = DataBindingUtil.setContentView(this, R.layout.activity_recognition)
        dataBinding?.options = ocrOptions
        val actionBar = supportActionBar
        actionBar!!.setDisplayShowHomeEnabled(false)

        cameraView = findViewById(R.id.camera_surface)
        cameraView!!.setShowTextBounds(true)
        cameraView!!.enableRecognitionEnhancement(true)
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
        dialogBuilder.setPositiveButton(R.string.dialog_ocr_button) { _, _ ->
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
