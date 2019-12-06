package com.gvtechcom.obigalaxiscan;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.ResultPoint;
import com.google.zxing.client.android.BeepManager;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;
import com.journeyapps.barcodescanner.DefaultDecoderFactory;

import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class BarCodeViewActivity extends AppCompatActivity {
    private DecoratedBarcodeView barcodeView;
    private BeepManager beepManager;
    private String lastText;
    Dialog dialog;
    private static String fileNametTime;
    SharedPreferences sharedPreferences;
    Switch aSwitch;
    Button btnScan;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bar_code_view);
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
        );
        barcodeView = (DecoratedBarcodeView) findViewById(R.id.barcode_scanner);
        aSwitch = findViewById(R.id.btn_switch);
        btnScan = findViewById(R.id.btn_scan);
        sharedPreferences = getSharedPreferences("ObigalaxyScan",MODE_PRIVATE);
        String checkSwitch = sharedPreferences.getString("aSwitch","true");
        btnScan.setVisibility(View.GONE);
        if (checkSwitch.equals("false")){
            aSwitch.setChecked(false);
        } else aSwitch.setChecked(true);
        //callScan();

    }

    @Override
    protected void onStart() {
        aSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                sharedPreferences.edit ().putString("aSwitch",aSwitch.isChecked()?"true":"false").commit();
                barcodeView.setStatusText("Lựa chọn áp dụng cho các lượt tiếp theo.");
                finish();
            }
        });
        btnScan.setOnClickListener(new OnClickListener(){
            @Override
            public void onClick(View view) {
                btnScan.setVisibility(View.GONE);
                barcodeView.setStatusText("Đang quét ảnh...");
                callScan();
            }
        });
        super.onStart();
    }

    private void callScan(){
        Collection<BarcodeFormat> formats = Arrays.asList(BarcodeFormat.QR_CODE, BarcodeFormat.CODE_39);
        barcodeView.getBarcodeView().setDecoderFactory(new DefaultDecoderFactory(formats));
        barcodeView.initializeFromIntent(getIntent());
        barcodeView.decodeContinuous(callback);
        beepManager = new BeepManager(this);
        if (aSwitch.isChecked()){
            barcodeView.setStatusText("Quét mã code và chụp ảnh.");
        } else barcodeView.setStatusText("Quét mã code.");
    }

    private BarcodeCallback callback = new BarcodeCallback() {
        @Override
        public void barcodeResult(BarcodeResult result) {
            if(result.getText() == null || result.getText().equals(lastText)) {
                // Prevent duplicate scans
                return;
            }

            lastText = result.getText();
            barcodeView.setStatusText("Đang gửi lên hệ thống");

            beepManager.playBeepSoundAndVibrate();

            //Added preview of scanned barcode
//            ImageView imageView = (ImageView) findViewById(R.id.barcodePreview);
//            imageView.setImageBitmap(result.getBitmapWithResultPoints(Color.YELLOW));
            Bitmap bitmap = result.getBitmapWithResultPoints(Color.YELLOW);
            File newFile = null;
            try {
                newFile = savebitmap(bitmap);
            } catch (IOException e) {
                e.printStackTrace();
            }
            Uri uri= Uri.fromFile(newFile);
            showDialogImage(uri);
            String fileName = fileNametTime;
            MainActivity.Companion.setNAME_IMAGE(lastText);
            POST(MainActivity.Companion.getUrl(), newFile, fileName, new Callback() {
                @Override
                public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            barcodeView.setStatusText("Xem kết quả trên màn hình!");
                            Toast.makeText(getBaseContext(), "Gửi thành công!", Toast.LENGTH_SHORT).show();
                            Handler handles =new Handler();
                            handles.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    finish();
                                    MainActivity.Companion.setChekScanQR(true);
                                    dialog.dismiss();
                                }
                            },5000);
                        }
                    });


                }
                @Override
                public void onFailure(@NotNull Call call, @NotNull final IOException e) {
                    runOnUiThread(new Runnable(){

                        @Override
                        public void run() {
                            barcodeView.setStatusText("Không kết nối được!");
                            Toast.makeText(getBaseContext(),"Lỗi: "+e,Toast.LENGTH_LONG).show();
                            dialog.dismiss();
                            Handler handles =new Handler();
                            handles.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    finish();
                                    MainActivity.Companion.setChekScanQR(true);
                                }
                            },1000);
                        }
                    });

                }


            });


        }

        @Override
        public void possibleResultPoints(List<ResultPoint> resultPoints) {
        }
    };

    public static File savebitmap(Bitmap bmp) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        fileNametTime = "Obi_"+System.currentTimeMillis()+".jpg";
        File f = new File(Environment.getExternalStorageDirectory()
                + File.separator + fileNametTime);
        f.createNewFile();
        FileOutputStream fo = new FileOutputStream(f);
        fo.write(bytes.toByteArray());
        fo.close();
        return f;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (aSwitch.isChecked()){
            callScan();
            btnScan.setVisibility(View.GONE);
            barcodeView.setStatusText("Đang quét ảnh...");
        } else {
            barcodeView.setStatusText("Bấm vào SCAN để quét ảnh.");
            btnScan.setVisibility(View.VISIBLE);
        }
        barcodeView.resume();
    }

    @Override
    protected void onPause() {
        super.onPause();

        barcodeView.pause();
    }

    public void pause(View view) {
        barcodeView.pause();
    }

    public void resume(View view) {
        barcodeView.resume();
    }

    public void triggerScan(View view) {
        barcodeView.decodeSingle(callback);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return barcodeView.onKeyDown(keyCode, event) || super.onKeyDown(keyCode, event);
    }
    public Call POST(String url, File file, String fileName, Callback callback){
        Toast.makeText(this, "Gửi lên hệ thống", Toast.LENGTH_LONG).show();
        final MediaType MEDIA_TYPE_JPG = MediaType.parse("image/jpg");
        RequestBody formBody =new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("content",
                        MainActivity.Companion.getNAME_IMAGE()
                )
                .addFormDataPart("image", fileName, RequestBody.create (MEDIA_TYPE_JPG, file))
                .build();

        Request request =new Request.Builder()
                .url(url)
                .post(formBody)
                .build();

        OkHttpClient client = new OkHttpClient();
        Call call = client.newCall(request);
        call.enqueue(callback);
        return call;
    }

    private void showDialogImage(Uri uri) {
        int width = (int)(getResources().getDisplayMetrics().widthPixels*0.70);
        int height = (int)(getResources().getDisplayMetrics().heightPixels*0.70);
        dialog =new Dialog(this);
        View dialogLayout = getLayoutInflater().inflate(R.layout.dialog_image_scan, null);
        ImageView imageView = dialogLayout.findViewById(R.id.img_View);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(dialogLayout);
        dialog.getWindow().setLayout(width,height);
        dialog.getWindow().setBackgroundDrawableResource(R.color.zxing_transparent);
        imageView.setImageURI(uri);
        dialog.create();
        dialog.show();

    }
}
