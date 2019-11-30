package com.gvtechcom.obigalaxiscan

import android.Manifest
import android.app.Dialog
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.RectF
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.media.Image
import android.media.ImageReader
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Size
import android.util.SparseIntArray
import android.view.Surface
import android.view.TextureView
import android.view.Window
import android.view.WindowManager
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.gvtechcom.obigalaxiscan.MainActivity.Companion.NAME_IMAGE
import kotlinx.android.synthetic.main.activity_take_photo.*
import kotlinx.android.synthetic.main.dialog_image_scan.view.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.*
import java.util.*

class TakePhotoActivity : AppCompatActivity() {
    val url = "http://10.10.10.43/api/upload"
    private val neededPermissions = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )
    var client = OkHttpClient()
    var dialog: Dialog? = null
    private var ORIENTATIONS = SparseIntArray().apply {
    }
    private var cameraId: String? = null
    protected var cameraDevice: CameraDevice? = null
    protected var cameraCaptureSessions: CameraCaptureSession? = null
    protected var captureRequestBuilder: CaptureRequest.Builder? = null
    private var imageDimension: Size? = null
    private var imageReader: ImageReader? = null
    private var callTransform = true // chỉ quét lần đầu tiên
    var mviewHeight = 0
    var mviewWidth = 0
    // LƯU RA FILE
    private val file: File? = null
    private val REQUEST_CAMERA_PERMISSION = 200
    private var mBackgroundHandler: Handler? = null
    private var mBackgroundThread: HandlerThread? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_take_photo)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

    }

    private var textureListener: TextureView.SurfaceTextureListener =
        object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(surface: SurfaceTexture,width: Int,height: Int
            ) {
                openCamera(width,height)
            }

            override fun onSurfaceTextureSizeChanged(
                surface: SurfaceTexture,
                width: Int,
                height: Int

            ) {
                //configureTransform(width,height)
            }

            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                return false
            }

            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
            }
        }

    private val stateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) {
            // Camera opened
            Log.e("AAA", "onOpened")
            cameraDevice = camera
            createCameraPreview()
        }

        override fun onDisconnected(camera: CameraDevice) {
            Log.e("AAA", "onDisconnected")
            cameraDevice?.close()
        }

        override fun onError(camera: CameraDevice, error: Int) {
            Log.e("AAA", "onError----: $error")
            cameraDevice?.close()
            cameraDevice = null
        }
    }

    protected fun startBackgroundThread() {
        mBackgroundThread = HandlerThread("Camera Background")
        mBackgroundThread!!.start()
        mBackgroundHandler = Handler(mBackgroundThread!!.looper)
    }

    protected fun stopBackgroundThread() {
        mBackgroundThread?.quitSafely()
        try {
            mBackgroundThread?.join()
            mBackgroundThread = null
            mBackgroundHandler = null
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }

    }
    private fun takePicture() {
        if (null == cameraDevice) {
            Log.e("AAA", "cameraDevice is null")
            return
        }
        val manager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            val characteristics = manager.getCameraCharacteristics(cameraDevice!!.id)
            var jpegSizes: Array<Size>? = null
            if (characteristics != null) {
                jpegSizes = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)!!.getOutputSizes(
                    ImageFormat.JPEG
                )
            }

            // CAPTURE IMAGE với tuỳ chỉnh kích thước
            var width = 1280
            var height = 720
            /*if (jpegSizes != null && jpegSizes.isNotEmpty()) {
                width = jpegSizes[0].width
                height = jpegSizes[0].height
            }*/
            val reader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 1)
            val outputSurfaces = ArrayList<Surface>(2)
            outputSurfaces.add(reader.surface)
            outputSurfaces.add(Surface(textureView.surfaceTexture))
            val captureBuilder =
                cameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
            captureBuilder.addTarget(reader.surface)
            captureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO)
            // kiểm tra orientation tuỳ thuộc vào mỗi device khác nhau như có nói bên trên
            val rotation = windowManager.defaultDisplay.rotation
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATIONS.get(rotation))
            val fileName = "Obi_"+System.currentTimeMillis() + ".jpg"
            val file = File(Environment.getExternalStorageDirectory(),fileName)
            var uri: Uri = Uri.fromFile(file)

            val readerListener = object : ImageReader.OnImageAvailableListener {
                override fun onImageAvailable(reader: ImageReader) {
                    var image: Image? = null
                    try {
                        image = reader.acquireLatestImage()
                        val buffer = image!!.planes[0].buffer
                        val bytes = ByteArray(buffer.capacity())
                        buffer.get(bytes)
                        save(bytes)
                    } catch (e: FileNotFoundException) {
                        e.printStackTrace()
                    } catch (e: IOException) {
                        e.printStackTrace()
                    } finally {
                        image?.close()
                    }
                }

                // Lưu ảnh
                @Throws(IOException::class)
                private fun save(bytes: ByteArray) {
                    var output: OutputStream? = null
                    try {
                        output = FileOutputStream(file)
                        output.write(bytes)
                    } finally {
                        output?.close()
                    }


                }
            }
            reader.setOnImageAvailableListener(readerListener, mBackgroundHandler)
            val captureListener = object : CameraCaptureSession.CaptureCallback() {
                override fun onCaptureCompleted(
                    session: CameraCaptureSession,
                    request: CaptureRequest,
                    result: TotalCaptureResult
                ) {
                    super.onCaptureCompleted(session, request, result)
                    showDialogImage(uri)
                    POST(url, file, fileName, object : Callback {
                        override fun onResponse(call: Call, response: Response) {
                            val responseData = response.body?.string()
                            runOnUiThread {
                                //println("----------------->>$responseData")
                                Toast.makeText(this@TakePhotoActivity, "Thành công!", Toast.LENGTH_SHORT).show()
                                val handles = Handler()
                                handles.postDelayed({
                                    finish()
                                    MainActivity.chekScanQR = true
                                    dialog!!.dismiss()
                                }, 5000)
                            }
                        }

                        override fun onFailure(call: Call, e: IOException) {
                            runOnUiThread {
                                Toast.makeText(this@TakePhotoActivity,"Lỗi. Vui lòng kiểm tra lại hệ thống! $e",
                                    Toast.LENGTH_LONG).show()
                                dialog!!.dismiss()
                                val handles = Handler()
                                handles.postDelayed({
                                    finish()
                                    MainActivity.chekScanQR = true
                                }, 1000)
                            }

                        }
                    })
                    //createCameraPreview()
                }
            }
            cameraDevice!!.createCaptureSession(
                outputSurfaces,
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        try {
                            session.capture(
                                captureBuilder.build(),
                                captureListener,
                                mBackgroundHandler
                            )
                        } catch (e: CameraAccessException) {
                            e.printStackTrace()
                        }

                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {}
                },
                mBackgroundHandler
            )
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }

    }

    // Khởi tạo camera để preview trong textureview
    private fun createCameraPreview() {
        try {
            val texture = textureView.surfaceTexture!!
            texture.setDefaultBufferSize(imageDimension!!.width, imageDimension!!.height)
            val surface = Surface(texture)
            captureRequestBuilder = cameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            captureRequestBuilder?.addTarget(surface)
            cameraDevice?.createCaptureSession(
                Arrays.asList(surface),
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(cameraCaptureSession: CameraCaptureSession) {
                        //The camera is already closed
                        if (null == cameraDevice) {
                            return
                        }
                        // When the session is ready, we start displaying the preview.
                        cameraCaptureSessions = cameraCaptureSession
                        updatePreview()
                    }

                    override fun onConfigureFailed(cameraCaptureSession: CameraCaptureSession) {
                        Toast.makeText(
                            this@TakePhotoActivity,
                            "Configuration change",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                },
                null
            )
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }

    }

    private fun openCamera(width: Int,height: Int) {
        val manager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        Log.e("AAA", "is camera open")
        try {
            cameraId = "0"//manager.cameraIdList[0]
            val characteristics = manager.getCameraCharacteristics(cameraId.toString())
            val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)!!
            imageDimension = map.getOutputSizes(SurfaceTexture::class.java)[0]
            // Add permission for camera and let user grant the permission
            // Kiểm tra permission với android sdk >= 23
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.CAMERA
                ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this@TakePhotoActivity,
                    arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    REQUEST_CAMERA_PERMISSION
                )
                return
            }

            manager.openCamera(cameraId.toString(), stateCallback, null)
            configureTransform(width, height)

        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }

        Log.e("AAA", "openCamera X")
        //takePicture()
    }

    private fun updatePreview() {
        if (null == cameraDevice) {
            Log.e("AAA", "updatePreview error, return")
        }
        captureRequestBuilder?.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO)
        try {
            cameraCaptureSessions?.setRepeatingRequest(
                captureRequestBuilder!!.build(),
                null,
                mBackgroundHandler
            )
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
        takePicture()

    }

    private fun configureTransform(viewWidth: Int, viewHeight: Int) {
        if (null == textureView || null == imageDimension) {
            return
        }
        val rotation = windowManager.defaultDisplay.rotation
        val matrix = Matrix()
        if (callTransform){
            mviewWidth = viewWidth
            mviewHeight = viewHeight
            callTransform = false
        }
        val viewRect = RectF(0f, 0f, viewWidth.toFloat(), viewHeight.toFloat())
        val bufferRect = RectF(0f, 0f, imageDimension?.height!!.toFloat(), imageDimension?.width!!.toFloat())
        val centerX = viewRect.centerX()
        val centerY = viewRect.centerY()
        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY())
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL)
            val scale = Math.max(
                viewHeight / imageDimension!!.height.toFloat(),
                viewWidth / imageDimension!!.width.toFloat()
            )
            matrix.postScale(scale,scale, centerX, centerY)
            matrix.postRotate((90 * (rotation - 2)).toFloat(), centerX, centerY)
            println("---------------$scale $centerX $centerY")
        } else if (Surface.ROTATION_180 == rotation) {
            matrix.postRotate(180f, centerX, centerY)
        }
        textureView.setTransform(matrix)

    }

    private fun closeCamera() {
        if (null != cameraDevice) {
            cameraDevice?.close()
            cameraDevice = null
        }
        if (null != imageReader) {
            imageReader?.close()
            imageReader = null
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                // close the app
                Toast.makeText(
                    this@TakePhotoActivity,
                    "Sorry!!!, you can't use this app without granting permission",
                    Toast.LENGTH_LONG
                ).show()
                finish()
            }
        }
    }


    override fun onResume() {
        super.onResume()
        Log.e("AAA", "onResume")
        //startBackgroundThread()
        textureView.surfaceTextureListener = textureListener
//        if (textureView.isAvailable) {
//            openCamera(mviewWidth,mviewHeight)
//        } else {
//            textureView.surfaceTextureListener = textureListener
//        }
    }

    override fun onPause() {
        Log.e("AAA", "onPause")
        //closeCamera();
        stopBackgroundThread()
        super.onPause()
    }



    private fun POST(url: String, file: File, fileName: String, callback: Callback): Call {
        Toast.makeText(this@TakePhotoActivity, "Gửi lên hệ thống", Toast.LENGTH_LONG).show()
        val formBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("content",
                NAME_IMAGE
            )
            .addFormDataPart("image", fileName, file.asRequestBody("image/jpg".toMediaTypeOrNull()))
            .build()

        val request = Request.Builder()
            .url(url)
            .post(formBody)
            .build()


        val call = client.newCall(request)
        call.enqueue(callback)
        return call
    }

    private fun showDialogImage(uri: Uri) {
        dialog = Dialog(this)
        val dialogLayout = layoutInflater.inflate(R.layout.dialog_image_scan, null)
        dialog!!.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog!!.setContentView(dialogLayout)
        dialogLayout.img_View.setImageURI(uri)
        dialog!!.show()

    }

}
