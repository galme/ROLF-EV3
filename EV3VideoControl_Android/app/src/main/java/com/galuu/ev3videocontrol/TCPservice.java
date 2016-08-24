package com.galuu.ev3videocontrol;

import android.os.AsyncTask;
import android.util.Log;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;


/**
 * Created by galme on 14. 08. 2016.
 * uses TCP sockets to send JPEGs (MJPEG video)
 */
public class TCPservice extends AsyncTask {

    int PORT = 6778;
    int BACKUP_PORT = 59592;
    boolean portSwitch = false;
    ServerSocket server = null;
    Socket socket = null;
    public ByteArrayOutputStream imgStream = null;


    public TCPservice() { }

    protected Void doInBackground(Object... params)
    {
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

        Log.d("PCcommsNetworking", "starting networking!");

        createServer();
        acceptClient();
        DataOutputStream dataOutputStream = null;

        try
        {
            dataOutputStream = new DataOutputStream(socket.getOutputStream());
        } catch (IOException e)
        {
            e.printStackTrace();
            networking();
        }

        while(true)
        {
            if (socket != null && imgStream != null)
            {
                try {
                    byte[] data = imgStream.toByteArray();

                    if (dataOutputStream != null)
                    {
                        dataOutputStream.write(ByteBuffer.allocate(4).putInt(data.length).array()); // image's data length
                        Log.d("LENGTH", data.length + " ");
                        dataOutputStream.write(data); // image's data
                        Log.d("LENGTH?", data.length + " ");
                        dataOutputStream.flush();
                    }
                    else
                    {
                        imgStream = null;
                        Log.e("TCPservice", "dataOutputStream is null when trying to write to it!");
                        networking();
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }
                imgStream = null;
            }
        }
    }
}
