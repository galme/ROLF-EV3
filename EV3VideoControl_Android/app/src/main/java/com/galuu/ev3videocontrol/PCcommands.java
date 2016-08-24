package com.galuu.ev3videocontrol;

import android.app.Activity;
import android.os.AsyncTask;
import android.util.Log;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;


/**
 * Created by galme on 14. 08. 2016.
 * this server is used for receiving and forwarding commands from PC to the robot
 */
public class PCcommands extends AsyncTask {

    Activity activity;
    BTManager btManager;
    int PORT = 5778;
    int BACKUP_PORT = 49592;
    boolean portSwitch = false;
    ServerSocket server = null;
    Socket socket = null;
    public static String remoteAddress = "";


    public PCcommands(Activity activity)
    {
        this.activity = activity;

        btManager = new BTManager();
        btManager.setBluetooth(BTManager.BT_ON);

    }

    protected Void doInBackground(Object... params)
    {
        while(!btManager.connect()); // wait for EV3 to connect to the phone over BT

        networking();
        return null;
    }

    void createServer() // create TCP server
    {
        Log.d("PCcommsServer", "trying to start server (PC)");
        try {
            if (server == null || server.isClosed())
            {
                if (portSwitch)
                {
                    server = new ServerSocket(BACKUP_PORT);
                    portSwitch = false;
                }
                else
                {
                    server = new ServerSocket(PORT);
                    portSwitch = true;
                }

            }
            Log.d("PCcommsServer", "PCcommands Server running on port " + PORT);
        } catch (IOException e)
        {
            e.printStackTrace();
            Log.d("PCcommsServer", "server failed, changing port from " + PORT + " to " + (++PORT));
            createServer();
        }
    }

    void acceptClient() // accept client (PC)
    {
        Log.d("PCcommsServer", "trying to accept client...");
        try
        {
            if (socket == null || !socket.isConnected() || socket.isClosed() || socket.isInputShutdown())
            {
                socket = server.accept();
                String tmpAddr = socket.getRemoteSocketAddress().toString();
                Log.d("probeIP", tmpAddr);
                if (tmpAddr.length() < 8)
                    throw new IOException();
                int start = tmpAddr.indexOf('/');
                int end = tmpAddr.indexOf(':');
                remoteAddress = tmpAddr.substring(start + 1, end);


                Log.d("IP", "IP: " + remoteAddress);

                if (socket == null)
                    throw new IOException();

                Log.d("PCcommsServer", "client connected!");
            }

        }
        catch (IOException e)
        {
            e.printStackTrace();
            Log.d("PCcommsServer", "client connection failed");
            acceptClient();
        }
    }

    void networking()
    {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Log.d("PCcommsNetworking", "starting networking!");

        createServer();
        acceptClient();
        DataInputStream dataInputStream = null;

        try
        {
            dataInputStream = new DataInputStream(socket.getInputStream());
        } catch (IOException e)
        {
            e.printStackTrace();
            networking();
        }

        while(true)
        {
            int data = -1;
            try {
                if (dataInputStream != null)
                {
                    data = dataInputStream.readInt();
                }
                else
                {
                    Log.e("PCcommsStream", "PCcommands dataInputStream is NULL!");
                    networking();
                }
            } catch (IOException e) {
                e.printStackTrace();
                socket = null;
                networking();
            }

            if(processData(data) && !btManager.sendInt(data))
            {
                while(!btManager.connect());
                if (dataInputStream != null) {
                    try {
                        dataInputStream.skip(10000);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

        }
    }

    // try to process input data on the phone
    // data >= -1 is reserved for robot directions and should always be sent to it
    // data < -1 can be used as phone specific commands
    private boolean processData(int data) // return true if data should be forwarded to robot, false otherwise
    {
        if (data == -2)
        {
            RTSPservice.toggleFlash();
            return false;
        }
        else
        {
            return true;
        }
    }

    public void stopBT()
    {
        btManager.stop();
    }
}
