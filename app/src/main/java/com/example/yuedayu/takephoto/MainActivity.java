package com.example.yuedayu.takephoto;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
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
    private Button flashBtn;
    private MediaRecorder mMediaRecorder;
    private FrameLayout preview;
    private boolean isRecording = false;

    public static final int MEDIA_TYPE_IMAGE = 1;
    public static final int MEDIA_TYPE_VIDEO = 2;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (checkCameraHardware(this) == false) return;
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);

        mCamera = getCameraInstance();
        mMediaRecorder = new MediaRecorder();

        mPreview = new CameraPreview(this, mCamera);
        preview = (FrameLayout) findViewById(R.id.camera_preview);
        setSize();
        preview.addView(mPreview);

        startBtn = (Button) findViewById(R.id.btnStartStop);
        startBtn.setOnClickListener(onclick);

    }

    OnClickListener onclick = new OnClickListener() {
        @Override
        public void onClick(View v) {
            recordingStart();

            TimeSlotHandler timeSlotHandler = new TimeSlotHandler();
            TimeSlotThread timeSlotThread = new TimeSlotThread(timeSlotHandler, 100);
            timeSlotThread.start();
        }
    };

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

    @Override
    protected void onPause() {
        super.onPause();
        releaseMediaRecorder();       // if you are using MediaRecorder, release it first
        releaseCamera();              // release the camera immediately on pause event
    }

    public Camera getCameraInstance() {
        Camera c = mCamera;

        try {
            c = Camera.open();
        } catch (Exception e) {
            Log.w("Get CameraInstance", "Open Fail");
        }
        return c;
    }

    public void onDestroy() {
        //mCamera.release();
        super.onDestroy();
    }

    private boolean prepareVideoRecorder(){

        mCamera.unlock();

        // Step 1: Unlock and set camera to MediaRecorder
        mMediaRecorder.setCamera(mCamera);

        // Step 2: Set sources
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);

        // Step 3: Set a CamcorderProfile (requires API Level 8 or higher)
        mMediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH));

        // Step 4: Set output file
        mMediaRecorder.setOutputFile(getOutputMediaFile(MEDIA_TYPE_VIDEO).toString());

        // Step 5: Set the preview output
        mMediaRecorder.setPreviewDisplay(mPreview.getHolder().getSurface());

        // Step 6: Prepare configured MediaRecorder
        try {
            mMediaRecorder.prepare();
        } catch (IllegalStateException e) {
            Log.d(TAG, "IllegalStateException preparing MediaRecorder: " + e.getMessage());
            releaseMediaRecorder();
            return false;
        } catch (IOException e) {
            Log.d(TAG, "IOException preparing MediaRecorder: " + e.getMessage());
            releaseMediaRecorder();
            return false;
        }
        return true;
    }

    private static File getOutputMediaFile(int type){
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.

        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "MyCameraApp");
        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        if (! mediaStorageDir.exists()){
            if (! mediaStorageDir.mkdirs()){
                Log.d("MyCameraApp", "failed to create directory");
                return null;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new java.util.Date());
        File mediaFile;
        if (type == MEDIA_TYPE_IMAGE){
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "IMG_"+ timeStamp + ".jpg");
        } else if(type == MEDIA_TYPE_VIDEO) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "VID_"+ timeStamp + ".mp4");
        } else {
            return null;
        }

        return mediaFile;
    }

    private void releaseMediaRecorder(){
        if (mMediaRecorder != null) {
            mMediaRecorder.reset();   // clear recorder configuration
            mMediaRecorder.release(); // release the recorder object
            mMediaRecorder = null;
            //mCamera.lock();           // lock camera for later use
        }
    }

    private void releaseCamera(){
        if (mCamera != null){
            mCamera.release();        // release the camera for other applications
            mCamera = null;
        }
    }

    private void setFlash() {
        if(!isRecording) {
            mCamera.lock();
        }

        Camera.Parameters parameters = mCamera.getParameters();
        parameters.setFlashMode(parameters.getFlashMode().equals(Camera.Parameters.FLASH_MODE_TORCH) ? Camera.Parameters.FLASH_MODE_OFF : Camera.Parameters.FLASH_MODE_TORCH);
        mCamera.setParameters(parameters);

        if(!isRecording) {
            mCamera.unlock();
        }
    }

    private class TimeSlotHandler extends Handler {
        public TimeSlotHandler() {
            super();
        }

        public TimeSlotHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 0:
                    setFlash();

                     break;
                case 1:
                    setFlash();
                    recordingEnd();
                    break;
            }

        }
    }

    private class TimeSlotThread extends Thread {


        private TimeSlotHandler mTimeSlotHandler;
        int mWaitTime = 250;
        int beforetime = 1;
        int afterTime = 500;
        public TimeSlotThread(TimeSlotHandler handler, int waitTime) {
            this.mWaitTime = waitTime;
            mTimeSlotHandler = handler;
        }
        @Override
        public void run() {
            try {
                sleep(beforetime);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            Message flashMessage = mTimeSlotHandler.obtainMessage(0);
            flashMessage.sendToTarget();
            try {
                sleep(afterTime);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            Message message = mTimeSlotHandler.obtainMessage(1);
            message.sendToTarget();
        }
    }

    public void recordingStart() {
        // initialize video camera
        if (prepareVideoRecorder()) {
            // Camera is available and unlocked, MediaRecorder is prepared,
            // now you can start recording
            mMediaRecorder.start();

            // inform the user that recording has started
            startBtn.setText("Stop");
            isRecording = true;
        } else {
            // prepare didn't work, release the camera
            releaseMediaRecorder();
            // inform user
        }
    }

    public void recordingEnd() {
        // stop recording and release camera
        mMediaRecorder.stop();  // stop the recording
        releaseMediaRecorder(); // release the MediaRecorder object

        // inform the user that recording has stopped
        startBtn.setText("Capture");
        isRecording = false;
    }
}