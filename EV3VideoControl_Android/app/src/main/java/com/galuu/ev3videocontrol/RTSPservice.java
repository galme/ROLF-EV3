package com.galuu.ev3videocontrol;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.hardware.Camera;
import android.preference.PreferenceManager;

import com.galuu.ev3videocontrol.streaming.Session;
import com.galuu.ev3videocontrol.streaming.SessionBuilder;
import com.galuu.ev3videocontrol.streaming.audio.AudioQuality;
import com.galuu.ev3videocontrol.streaming.gl.SurfaceView;
import com.galuu.ev3videocontrol.streaming.rtsp.RtspServer;
import com.galuu.ev3videocontrol.streaming.video.VideoQuality;
import com.galuu.ev3videocontrol.streaming.video.VideoStream;

/**
 * Created by galme on 18. 08. 2016.
 * uses RTSP to send video
 */
public class RTSPservice
{
    Activity activity = null;
    SurfaceView mSurfaceView = null;
    public static Session session = null;
    private static int RTSP_PORT = 5678;

    public RTSPservice(Activity activity)
    {
        this.activity = activity;
    }

    public void startRTSP()
    {
        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        mSurfaceView = (SurfaceView) activity.findViewById(R.id.surface);

        // Sets the port of the RTSP server to RTSP_PORT
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(activity).edit();
        editor.putString(RtspServer.KEY_PORT, String.valueOf(RTSP_PORT));
        editor.commit();

        // Configures the SessionBuilder
        session = SessionBuilder.getInstance()
                .setSurfaceView(mSurfaceView)
                .setPreviewOrientation(90)
                .setContext(activity.getApplicationContext())
                .setAudioEncoder(SessionBuilder.AUDIO_NONE)
                .setVideoQuality(new VideoQuality(640, 480, 15, 300000))
                .setVideoEncoder(SessionBuilder.VIDEO_H264)
                .build();


        // Starts the RTSP server
        activity.startService(new Intent(activity, RtspServer.class));
    }

    public static void toggleFlash()
    {
        Camera cam = VideoStream.mCamera;
        Camera.Parameters p = cam.getParameters();
        if (VideoStream.mFlashEnabled)
        {
            p.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
            VideoStream.mFlashEnabled = false;
        }
        else
        {
            p.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
            VideoStream.mFlashEnabled = true;
        }

        cam.setParameters(p);
    }
}
