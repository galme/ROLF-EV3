package com.galuu.ev3videocontrol;

/**
 * Created by galme on 16. 08. 2016.
 */

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.media.Image;
import android.media.ImageReader;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Created by galuu on 12. 07. 2016.
 * This class is used to capture video from the camera and send JPEGs over either UDP or TCP.
 * References to UDP/TCPservices must be given. The class then picks the appropriate one, based on the selected MainActivity.TransferMode
 */
public class PCVideoStreamer {

    Activity activity;

    protected static final String TAG = "VideoProcessing";
    protected static final int CAMERACHOICE = CameraCharacteristics.LENS_FACING_BACK;
    protected CameraDevice cameraDevice;
    protected CameraCaptureSession session;
    protected ImageReader imageReader;
    private static int JPEGquality = 30;
    private int FPS = 10;
    int videoWidth = 320;
    int videoHeight = 240;

    long lastEpoch = 0;

    UDPservice myUDPservice = null;
    TCPservice myTCPservice = null;



    public PCVideoStreamer(Activity activity, UDPservice myUDPservice, TCPservice myTCPservice) {
        this.activity = activity;
        this.myUDPservice = myUDPservice;
        this.myTCPservice = myTCPservice;

        onStartCommand();
    }

    public void setFPS(int fps)
    {
        FPS = fps;
    }

    protected CameraDevice.StateCallback cameraStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            Log.i(TAG, "CameraDevice.StateCallback onOpened");
            cameraDevice = camera;
            actOnReadyCameraDevice();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            Log.w(TAG, "CameraDevice.StateCallback onDisconnected");
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            Log.e(TAG, "CameraDevice.StateCallback onError " + error);
        }
    };

    protected CameraCaptureSession.StateCallback sessionStateCallback = new CameraCaptureSession.StateCallback() {
        @Override
        public void onConfigured(@NonNull CameraCaptureSession session) {
            Log.i(TAG, "CameraCaptureSession.StateCallback onConfigured");
            PCVideoStreamer.this.session = session;
            try {
                session.setRepeatingRequest(createCaptureRequest(), null, null);
            } catch (CameraAccessException e) {
                Log.e(TAG, e.getMessage());
            }
        }

        @Override
        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
        }
    };

    protected ImageReader.OnImageAvailableListener onImageAvailableListener = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader reader) {
            //Log.i(TAG, "onImageAvailable");
            Image img = reader.acquireLatestImage();
            if (img != null) {
                processImage(img);
                img.close();
            }
        }
    };

    public void readyCamera() {
        CameraManager manager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
        try {
            String pickedCamera = getCamera(manager);
            if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            manager.openCamera(pickedCamera, cameraStateCallback, null);
            imageReader = ImageReader.newInstance(videoWidth, videoHeight, 0x00000001 /*ImageFormat.YUV_420_888*/, 2 /* images buffered */);
            imageReader.setOnImageAvailableListener(onImageAvailableListener, null);
            Log.i(TAG, "imageReader created");
        } catch (CameraAccessException e){
            Log.e(TAG, e.getMessage());
        }
    }

    public String getCamera(CameraManager manager){
        try {
            for (String cameraId : manager.getCameraIdList()) {
                CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
                int cOrientation = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (cOrientation == CAMERACHOICE) {
                    return cameraId;
                }
            }
        } catch (CameraAccessException e){
            e.printStackTrace();
        }
        return null;
    }

    public void onStartCommand() {
        Log.i(TAG, "onStartCommand");

        readyCamera();

    }

    public void actOnReadyCameraDevice()
    {
        try {
            cameraDevice.createCaptureSession(Arrays.asList(imageReader.getSurface()), sessionStateCallback, null);
        } catch (CameraAccessException e){
            Log.e(TAG, e.getMessage());
        }
    }

    public void onDestroy() {
        try {
            session.abortCaptures();
        } catch (CameraAccessException e){
            Log.e(TAG, e.getMessage());
        }
        session.close();
    }

    private int fpsToDelay(int fps)
    {
        return 1000/fps + 1;
    }

    private void processImage(Image image){

        long currentEpoch = System.currentTimeMillis();
        if (currentEpoch > lastEpoch + fpsToDelay(FPS))
            lastEpoch = currentEpoch;
        else
            return;


        final Image.Plane[] planes = image.getPlanes();
        final ByteBuffer buffer = planes[0].getBuffer();

        int pixelStride = planes[0].getPixelStride();
        int rowStride = planes[0].getRowStride();
        int rowPadding = rowStride - pixelStride * videoWidth;

        // create bitmap
        Bitmap bitmap = Bitmap.createBitmap(videoWidth + rowPadding/pixelStride, videoHeight, Bitmap.Config.ARGB_8888);
        bitmap.copyPixelsFromBuffer(buffer);
        image.close();

        ByteArrayOutputStream tmpStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, JPEGquality, tmpStream);

        // send
        if (MainActivity.transferMode == TransferMode.UDP)
            myUDPservice.imgStream = tmpStream;
        else if (MainActivity.transferMode == TransferMode.TCP)
            myTCPservice.imgStream = tmpStream;

    }

    protected CaptureRequest createCaptureRequest() {
        try {
            CaptureRequest.Builder builder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
            builder.addTarget(imageReader.getSurface());
            return builder.build();
        } catch (CameraAccessException e) {
            Log.e(TAG, e.getMessage());
            return null;
        }
    }

}
