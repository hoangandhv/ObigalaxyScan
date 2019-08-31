package com.netfin.surfaceview

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import me.dm7.barcodescanner.zxing.ZXingScannerView
import com.google.zxing.*

class ScanActivity: AppCompatActivity(), ZXingScannerView.ResultHandler {
    private var mScannerView: ZXingScannerView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mScannerView = ZXingScannerView(this)
        setContentView(mScannerView)

    }

    override fun handleResult(p0: Result?) {
        MainActivity.NAME_IMAGE = p0?.text.toString()
        finish()
    }


    public override fun onResume() {
        super.onResume()
        mScannerView!!.setResultHandler(this) // Register ourselves as a handler for scan results.
        mScannerView!!.startCamera()
        // Start camera on resume
    }

    public override fun onPause() {
        super.onPause()
        mScannerView!!.stopCamera()           // Stop camera on pause
    }
}