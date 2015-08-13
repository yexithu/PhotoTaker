package com.example.yuedayu.takephoto;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.List;

import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.example.yuedayu.takephoto.R;

public class MainActivity extends Activity {

    private static final String TAG = "MainActivity";
    private Camera mCamera;
    private CameraPreview mPreview;
    private Button startBtn;
    private MediaRecorder mMediaRecorder;
    private boolean isStarted = false;
    private FrameLayout preview;
    MyHandler mHandler = null;
    Thread thread;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (checkCameraHardware(this) == false) return;
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);
        mCamera = getCameraInstance();
        mPreview = new CameraPreview(this, mCamera);
        preview = (FrameLayout) findViewById(R.id.camera_preview);
        setSize();
        preview.addView(mPreview);
        startBtn = (Button) findViewById(R.id.btnStartStop);
        startBtn.setOnClickListener(onclick);
    }

    OnClickListener onclick = new OnClickListener(){
        @Override
        public void onClick(View arg0) {
//            if (isStarted) {
//                setVideo(true);
//            } else {
//                setVideo(false);
//            }
            thread = new MyThread();
            thread.start();
        }
    };

    @SuppressLint("SimpleDateFormat")
    private void setVideo(boolean flag) {
        if (isStarted && flag) {
            Log.i(TAG, "start");
            isStarted = false;
            startBtn.setText("Start");
            mMediaRecorder.stop();
            mMediaRecorder.reset();
            mMediaRecorder.release();
            mMediaRecorder = null;
            if(mCamera != null)
                mCamera.lock();
        } else {
            Log.i(TAG, "stop");
            isStarted = true;
            startBtn.setText("Stop");
            mCamera.unlock();
            mMediaRecorder = new MediaRecorder();
            CamcorderProfile mProfile = CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH);
            mMediaRecorder.reset();
            mMediaRecorder.setCamera(mCamera);
            mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
            mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
            mMediaRecorder.setProfile(mProfile);
            long dateTaken = System.currentTimeMillis();
            Date date = new Date(dateTaken);
            SimpleDateFormat dateFormat = new SimpleDateFormat("HH-mm-ss");
            String title = dateFormat.format(date);
            String filename = title + ".3gp"; // Used when emailing.
            String cameraDirPath = getSDPath() + File.separator + "pic" + File.separator;
            System.out.println(cameraDirPath);
            String filePath = cameraDirPath + filename;
            File cameraDir = new File(cameraDirPath);
            cameraDir.mkdirs();
            mMediaRecorder.setOutputFile(filePath);
            try {
                mMediaRecorder.prepare();
                mMediaRecorder.start(); // Recording is now started
            } catch (RuntimeException e) {
                Log.e(TAG, "Could not start media recorder. ", e);
                return;
            } catch (IOException e) {
                Log.e(TAG, "Could not start media recorder. ", e);
            }
        }
    }

    private class MyHandler extends Handler {
        public MyHandler(Looper looper){
            super (looper);
        }
        @Override
        public void handleMessage(Message msg) { // 处理消息
//            text .setText(msg. obj .toString());
            if (msg.obj.toString().equals("start")) {
                setVideo(true);
            } else if (msg.obj.toString().equals("open")) {
                setLight(true);
            } else {
                setVideo(false);
                setLight(false);
            }
        }
    }

    private class MyThread extends Thread{
        @Override
        public void run() {
            Looper curLooper = Looper.myLooper ();
            Looper mainLooper = Looper.getMainLooper ();
            if (curLooper== null ){
                mHandler = new MyHandler(mainLooper);
            } else {
                mHandler = new MyHandler(curLooper);
            }
            String msg = "start";
            mHandler.removeMessages(0);
            Message m = mHandler.obtainMessage(1, 1, 1, msg);
            mHandler.sendMessage(m);
            try {
                wait(100);
            } catch (Exception e) {

            }
            msg = "open";
            mHandler.removeMessages(0);
            m = mHandler.obtainMessage(1, 1, 1, msg);
            mHandler.sendMessage(m);
            try {
                wait(100);
            } catch (Exception e) {

            }
            msg = "close";
            mHandler.removeMessages(0);
            m = mHandler.obtainMessage(1, 1, 1, msg);
            mHandler.sendMessage(m);
        }
    }

    private void setLight(boolean flag) {
        Camera.Parameters mParameters = mCamera.getParameters();
        if (flag) {
            mParameters.setFlashMode(Camera.Parameters.FLASH_MODE_ON);
        } else {
            mParameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
        }
        mCamera.setParameters(mParameters);
    }

    public static String getSDPath() {
        File sdDir = null;
        boolean sdCardExist = Environment.getExternalStorageState()
                .equals(android.os.Environment.MEDIA_MOUNTED);
        if (sdCardExist) {
            sdDir = Environment.getExternalStorageDirectory();
            return sdDir.toString();
        }

        return null;
    }

    private boolean checkCameraHardware(Context context) {
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            return true;
        } else {
            return false;
        }
    }

    private void setSize() {
        Camera.Parameters mParameters = mCamera.getParameters();
        Camera.Size bestSize = null;
        List<Camera.Size> sizeList = mCamera.getParameters().getSupportedPreviewSizes();
        bestSize = sizeList.get(0);

        for (int i = 1; i < sizeList.size(); i++) {
            if ((sizeList.get(i).height * sizeList.get(i).width) >
                    (bestSize.height * bestSize.height)) {
                bestSize = sizeList.get(i);
            }
        }
        mParameters.setPreviewSize(sizeList.get(0).width, sizeList.get(0).height);
        mParameters.setPictureSize(bestSize.width, bestSize.height);
        mParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
        mCamera.setParameters(mParameters);
    }

    public static Camera getCameraInstance() {
        Camera c = null;
        try {
            c = Camera.open();
        } catch (Exception e) {

        }
        return c;
    }

    public void onDestroy() {
        mCamera.release();
        super.onDestroy();
    }
}