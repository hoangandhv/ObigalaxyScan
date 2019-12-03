package com.gvtechcom.obigalaxiscan

import android.Manifest
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import okhttp3.OkHttpClient


class MainActivity : AppCompatActivity(){


    private var resultFalse = 0 // đang quét QRcode mà tắt app thì =1
    private var VERSION = 0
    private val MY_CAMERA_REQUEST_CODE = 100
    private val MY_WRITE_EXTERNAL_STORAGE = 200

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
            openScanQrCode()

    }
    private fun checkPermission(): Boolean{
        when (PackageManager.PERMISSION_DENIED) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) -> {
                ActivityCompat.requestPermissions(this,arrayOf(Manifest.permission.CAMERA),MY_CAMERA_REQUEST_CODE)
                return false
            }
            ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) -> {
                ActivityCompat.requestPermissions(this,arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),MY_WRITE_EXTERNAL_STORAGE)
                return false
            }
            else -> return true
        }
    }
    private fun openScanQrCode(){
        if (checkPermission()){
            val intent = Intent(this,BarCodeViewActivity::class.java)
            startActivityForResult(intent,1)
        }

    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        //NAME_IMAGE = data!!.getStringExtra("valueScan")
        if (requestCode==1){
            resultFalse = 0
           // NAME_IMAGE = data!!.getStringExtra("resultNAMEIMAGE")

            val handler = Handler()
            handler.postDelayed({
                openScanQrCode()
            },1000)
        } else resultFalse =1

    }
    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            MY_CAMERA_REQUEST_CODE -> {
                // If request is cancelled, the result arrays are empty.
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    openScanQrCode()
                } else {
                    finish()
                }
                return
            }
            MY_WRITE_EXTERNAL_STORAGE -> {
                // If request is cancelled, the result arrays are empty.
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    openScanQrCode()
                } else {
                    finish()
                }
                return
            }
            // Add other 'when' lines to check for other
            // permissions this app might request.
            else -> {
                // Ignore all other requests.
            }
        }
    }

    companion object{
        var NAME_IMAGE: String = ""
        var chekScanQR = true
        val url = "http://10.10.10.43/api/upload"
    }



}
