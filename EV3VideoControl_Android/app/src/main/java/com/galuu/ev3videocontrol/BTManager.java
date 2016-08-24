package com.galuu.ev3videocontrol;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.ParcelUuid;
import android.util.Log;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.UUID;

/**
 * Created by galuu on 16. 07. 2016.
 * NOTE : EV3 MUST BE PAIRED with the phone already !!!
 */

public class BTManager {

    public static final String TAG = "Connector";
    private static UUID EV3_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    public static final boolean BT_ON = true;
    public static final boolean BT_OFF = false;
    boolean connected = false;

    public BluetoothAdapter bluetoothAdapter;
    public BluetoothSocket bluetoothSocket;
    public String address = null;

    public BTManager(String address) {
        this.address = address;
        this.bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    public BTManager()
    {
        this.bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    public void setBluetooth(boolean state) {
        if(state == BTManager.BT_ON) {
            // Check if bluetooth is off
            if(!this.bluetoothAdapter.isEnabled())
            {
                this.bluetoothAdapter.enable();
                while(!this.bluetoothAdapter.isEnabled());
                Log.d(BTManager.TAG, "Bluetooth turned on");
            }

        }

        // Check if bluetooth is enabled
        else if(state == BTManager.BT_OFF) {
            // Check if bluetooth is enabled
            if(this.bluetoothAdapter.isEnabled())
            {
                this.bluetoothAdapter.disable();
                while(this.bluetoothAdapter.isEnabled());
                Log.d(BTManager.TAG, "Bluetooth turned off");
            }

        }

    }

    public boolean connect() {

        BluetoothDevice ev3 = null;

        // was the address manually given ?
        if (address != null)
        {
            ev3 = this.bluetoothAdapter.getRemoteDevice(this.address);
        }
        else
        {
            for (BluetoothDevice btDevice : bluetoothAdapter.getBondedDevices())
            {
                ParcelUuid[] uuids = btDevice.getUuids();
                for (int i = 0; i != uuids.length; i++)
                {
                    if (uuids[i].getUuid().compareTo(EV3_UUID) == 0) // UUID matches ?
                    {
                        ev3 = btDevice;
                    }
                }
            }
        }

        if (ev3 == null)
        {
            Log.e("BlueTooth", "no EV3 paired!");
            return false;
        }

        try {
            this.bluetoothSocket = ev3.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")); // EV3 UUID
            this.bluetoothSocket.connect();
            connected = true;
            Log.d("BTManager", "BT connection established");

        }
        catch (IOException e) {
            connected = false;

        }

        return connected;

    }

    public boolean isConnected()
    {
        return connected;
    }

    public boolean sendInt(int msg)
    {
        if (connected)
        {
            if(this.bluetoothSocket != null)
            {
                try {
                    DataOutputStream output = new DataOutputStream(this.bluetoothSocket.getOutputStream());
                    output.writeInt(msg);
                    output.flush();
                    Log.d(BTManager.TAG, "Successfully sent message");

                }
                catch (IOException e) {
                    Log.d(BTManager.TAG, "Couldn't send message");
                    connected = false;

                }
            }
            else
            {
                Log.d(BTManager.TAG, "Couldn't send message");
                connected = false;
            }
        }

        return connected;
    }

    public Integer readInt() {
        Integer message;

        if(this.bluetoothSocket!= null) {
            try {
                DataInputStream input = new DataInputStream(this.bluetoothSocket.getInputStream());
                message = input.readInt();
                Log.d(BTManager.TAG, "Successfully read message");

            }
            catch (IOException e) {
                message = null;
                Log.d(BTManager.TAG, "Couldn't read message");

            }
        }
        else {
            message = null;
            Log.d(BTManager.TAG, "Couldn't read message");

        }

        return message;

    }

    public void stop()
    {
        try
        {
            bluetoothSocket.close();
            bluetoothSocket.getInputStream().close();
            bluetoothSocket.getOutputStream().close();
            bluetoothAdapter.disable();
        }
        catch (Exception exc)
        {
            bluetoothAdapter.disable();
        }

    }
}