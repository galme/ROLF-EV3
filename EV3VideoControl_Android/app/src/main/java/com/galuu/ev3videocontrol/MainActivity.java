package com.galuu.ev3videocontrol;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.WindowManager;

/**
 * copyright Gal M.
 * This application is used to connect to the PC sending commands over a local TCP socket connection and the EV3 robot over BT which receives and interprets those commands.
 * It also captures video (w/libstreaming) or a series of images (MJPEG) and sends it to the PC.
 * Libstreaming library (https://github.com/fyhertz/libstreaming @fyhertz) is used in this project.
 *
 *
 * Orignal GitHub source: https://github.com/galme/ROLF-EV3
 */

public class MainActivity extends Activity {

    private final static String TAG = "MainActivity";
    public static TransferMode transferMode = TransferMode.RTSP; // set this to the mode you want (RTSP uses h.264 encoding, TCP/UDP is MJPEG) ... it must match the PC client's setting!
    public static boolean forceMediaCodec = true;
    PCcommands pCcommands = null;
    UDPservice myUDPservice = null;
    TCPservice myTCPservice = null;
    RTSPservice rtspService = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);

        // start server used for receiving and forwarding commands from PC to the robot
        pCcommands = new PCcommands(this);
        pCcommands.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

        // start proper service (TCP, UDP, RTSP)
        if (MainActivity.transferMode == TransferMode.UDP)
        {
            myUDPservice = new UDPservice();
            myUDPservice.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
        else if (MainActivity.transferMode == TransferMode.TCP)
        {
            myTCPservice = new TCPservice();
            myTCPservice.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
        else if (MainActivity.transferMode == TransferMode.RTSP)
        {
            rtspService = new RTSPservice(this);
            rtspService.startRTSP();
        }

        // start VideoStreamer server for MJPEG, which uses TCP/UDP
        if (MainActivity.transferMode == TransferMode.UDP || MainActivity.transferMode == TransferMode.TCP)
        {
            PCVideoStreamer pcVideoStreamer = new PCVideoStreamer(this, myUDPservice, myTCPservice);
            pcVideoStreamer.setFPS(10);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

    }
}
