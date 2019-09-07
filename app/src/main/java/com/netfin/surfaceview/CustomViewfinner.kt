package com.netfin.surfaceview

import android.content.Context
import android.graphics.Rect
import com.journeyapps.barcodescanner.CameraPreview


class CustomCameraPreview(context: Context?) : CameraPreview(context) {

    public override fun calculateFramingRect(container: Rect?, surface: Rect?): Rect {
        return container!!
    }


}