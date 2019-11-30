package com.gvtechcom.obigalaxiscan;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class TakePhotoSurefaceviewActivity extends AppCompatActivity implements SurfaceHolder.Callback {
    Button btn_capture;
    Camera camera1;
    SurfaceView surfaceView;
    SurfaceHolder surfaceHolder;
    ImageView imageView;
    public static boolean previewing = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_take_photo_surefaceview);
        getWindow().setFormat(PixelFormat.UNKNOWN);
        surfaceView = findViewById(R.id.surfaceView);
        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(this);
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        btn_capture = (Button) findViewById(R.id.btn_capture);
        imageView = findViewById(R.id.imageView);
        if(!previewing){

            camera1 = Camera.open();
            if (camera1 != null){
                try {
                    camera1.setDisplayOrientation(90);
                    camera1.setPreviewDisplay(surfaceHolder);
                    camera1.startPreview();
                    previewing = true;
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }

        btn_capture.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                // TODO Auto-generated method stub

                if(camera1 != null)
                {
                    camera1.takePicture(myShutterCallback, myPictureCallback_RAW, myPictureCallback_JPG);

                }
            }
        });
    }

    android.hardware.Camera.ShutterCallback myShutterCallback = new android.hardware.Camera.ShutterCallback(){

        public void onShutter() {
            // TODO Auto-generated method stub
        }};

    android.hardware.Camera.PictureCallback myPictureCallback_RAW = new android.hardware.Camera.PictureCallback(){

        public void onPictureTaken(byte[] arg0, Camera arg1) {
            // TODO Auto-generated method stub
        }};

    android.hardware.Camera.PictureCallback myPictureCallback_JPG = new android.hardware.Camera.PictureCallback(){

        public void onPictureTaken(byte[] arg0, Camera arg1) {
            // TODO Auto-generated method stub
            Bitmap bitmapPicture = BitmapFactory.decodeByteArray(arg0, 0, arg0.length);

            Bitmap correctBmp = Bitmap.createBitmap(bitmapPicture, 0, 0, bitmapPicture.getWidth(), bitmapPicture.getHeight(), null, true);

            try {
                File newFile = savebitmap(correctBmp);
                System.out.println("------------------------save");
                Uri uri= Uri.fromFile(newFile);
                imageView.setImageURI(uri);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }};

    public static File savebitmap(Bitmap bmp) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.JPEG, 60, bytes);
        File f = new File(Environment.getExternalStorageDirectory()
                + File.separator + "testimage.jpg");
        f.createNewFile();
        FileOutputStream fo = new FileOutputStream(f);
        fo.write(bytes.toByteArray());
        fo.close();
        return f;
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int width,
                               int height) {
        // TODO Auto-generated method stub
        if(previewing){
            camera1.stopPreview();
            previewing = false;
        }

        if (camera1 != null){
            try {
                camera1.setPreviewDisplay(surfaceHolder);
                camera1.startPreview();
                previewing = true;
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    public void surfaceCreated(SurfaceHolder holder) {
        // TODO Auto-generated method stub

    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        // TODO Auto-generated method stub

        camera1.stopPreview();
        camera1.release();
        camera1 = null;
        previewing = false;

    }

}
