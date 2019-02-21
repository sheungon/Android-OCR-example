package com.sotwtm.ocr.demo.option

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.sotwtm.ocr.demo.R
import com.sotwtm.ocr.demo.databinding.ListTrainedDataBinding
import com.sotwtm.ocr.demo.main.OcrOptions

class TrainedDataBottomSheetDialogFragment : BottomSheetDialogFragment() {

    var dataBinding: ListTrainedDataBinding? = null
    var ocrOptions: OcrOptions? = null
        set(value) {
            field = value
            dataBinding?.options = ocrOptions
        }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        dataBinding = DataBindingUtil.inflate(inflater, R.layout.list_trained_data, container, false)
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
        ocrOptions?.setSelectedTrainedData(view.tag)
        dismiss()
    }
}