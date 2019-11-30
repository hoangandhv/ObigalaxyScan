package com.gvtechcom.obigalaxiscan

import android.content.Intent
import android.os.*
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import android.widget.Toast
import com.google.zxing.integration.android.IntentIntegrator


class MainActivity : AppCompatActivity(){


    private var resultFalse = 0 // đang quét QRcode mà tắt app thì =1


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        startScanQrcode()

    }
    override fun onResume() {
        if (chekScanQR){
//            val intent = Intent(this,TakePhotoSurefaceviewActivity::class.java)
//            startActivity(intent)
            startScanQrcode()
            chekScanQR = false
        }
        super.onResume()
    }

    private fun startScanQrcode() {
        var integrator = IntentIntegrator(this)
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
        integrator.setPrompt("Scan QR CODE")
        integrator.setCameraId(0)
        integrator.setOrientationLocked(false)
        integrator.setBeepEnabled(false)
        integrator.initiateScan()
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val intentResult = IntentIntegrator.parseActivityResult(requestCode,resultCode,data)
        super.onActivityResult(requestCode, resultCode, data)
        //NAME_IMAGE = data!!.getStringExtra("valueScan")
        if (intentResult.contents!=null){
            resultFalse = 0
            NAME_IMAGE = intentResult.contents
            Toast.makeText(this, NAME_IMAGE+". Vui lòng đợi",Toast.LENGTH_LONG).show()
            val handler = Handler()
            handler.postDelayed({
                val intent = Intent(this,TakePhotoSurefaceviewActivity::class.java)
                startActivity(intent)
            },100)
        } else resultFalse =1

    }
    companion object{
        var NAME_IMAGE: String = ""
        var chekScanQR = true
    }



}
