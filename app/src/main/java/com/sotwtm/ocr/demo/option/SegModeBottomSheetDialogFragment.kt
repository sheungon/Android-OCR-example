package com.sotwtm.ocr.demo.option

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.sotwtm.ocr.demo.R
import com.sotwtm.ocr.demo.databinding.ListSegModeBinding
import com.sotwtm.ocr.demo.main.OcrOptions

class SegModeBottomSheetDialogFragment : BottomSheetDialogFragment() {

    var dataBinding: ListSegModeBinding? = null
    var ocrOptions: OcrOptions? = null
        set(value) {
            field = value
            dataBinding?.options = ocrOptions
        }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        dataBinding = DataBindingUtil.inflate(inflater, R.layout.list_seg_mode, container, false)
        dataBinding?.options = ocrOptions
        dataBinding?.fragment = this
        return dataBinding?.root
    }

    override fun onDestroyView() {
        super.onDestroyView()

        dataBinding?.unbind()
        dataBinding = null
    }

    fun onClick(view: View) {
        ocrOptions?.setSegMode(view.tag)
        dismiss()
    }
}