import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import lejos.hardware.Bluetooth;
import lejos.hardware.Button;
import lejos.hardware.Key;
import lejos.hardware.KeyListener;
import lejos.remote.nxt.NXTConnection;


public class MainClass {
	
	static private DataInputStream inputDataStream = null;
	static private DataOutputStream outputDataStream = null;
	
	static void startBlueToothConnection() // establish BT connection
	{
		NXTConnection connection = null;
		try
		{
			// listen for BT connections
			System.out.println("init BT connection ...");
			connection = Bluetooth.getNXTCommConnector().waitForConnection(10000, NXTConnection.RAW);
			inputDataStream = connection.openDataInputStream();
			outputDataStream = connection.openDataOutputStream();
			System.out.println("Connected");
		} 
		catch (Exception a)
		{
			// TODO : properly reestablish BT connection on error
			System.out.println("ERROR: shit hit the fan");
			Bluetooth.getNXTCommConnector().cancel();
			
			try {
				inputDataStream.close();
				outputDataStream.close();
				connection.close();
			} catch (IOException e) {
				System.out.println("ERROR: closing failed");
			}
			startBlueToothConnection();
		}
		
		getData();
	}
	
	public static void main(String[] args) 
	{
		
		// emergency exit :)
		Button.ESCAPE.addKeyListener(new KeyListener() {

			@Override
			public void keyPressed(Key k) {
				System.exit(0);
				
			}

			@Override
			public void keyReleased(Key k) {
				// TODO Auto-generated method stub
			}
		});

		startBlueToothConnection();
	}
	
	static void getData()
	{
		new CommandListener(inputDataStream).run();
	}

}
