package com.galuu.ev3videocontrol;

import android.os.AsyncTask;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

/**
 * Created by galme on 16. 08. 2016.
 * uses UDP sockets to send JPEGs (MJPEG video)
 */
public class UDPservice extends AsyncTask<Void, Void, Void> {

    static int DATAGRAM_PORT = 54029;
    InetAddress IP = null;

    DatagramSocket datagramSocket = null;

    public ByteArrayOutputStream imgStream = null;

    public UDPservice()
    {
        try {
            datagramSocket = new DatagramSocket();
            datagramSocket.setSendBufferSize(65000);
            datagramSocket.setTrafficClass(0x04); // hint: try others
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    void getRemoteIP()
    {
        Log.d("getRemoteIP", "called");
        if (IP != null)
            return;

        while(PCcommands.remoteAddress.equals(""));

        try {
            IP = InetAddress.getByName(PCcommands.remoteAddress);
        } catch (UnknownHostException e) {
            getRemoteIP();
        }
    }

    @Override
    protected Void doInBackground(Void... params) {

        while(true)
        {
            if (imgStream != null)
            {
                getRemoteIP();
                Log.d("UDP", "sending");
                byte[] img = imgStream.toByteArray();
                byte[] num = ByteBuffer.allocate(4).putInt(img.length).array();

                byte[] buffer = new byte[img.length + num.length];
                System.arraycopy(num, 0, buffer, 0, num.length);
                System.arraycopy(img, 0, buffer, num.length, img.length);

                DatagramPacket datagramPacket = new DatagramPacket(img, img.length, IP, DATAGRAM_PORT);

                try {
                    datagramSocket.send(datagramPacket);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                imgStream = null;

                Log.d("UDP", "sent");
            }
        }
    }
}
