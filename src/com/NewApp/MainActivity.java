package com.NewApp;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.Set;

import zephyr.android.BioHarnessBT.BTClient;
import zephyr.android.BioHarnessBT.ZephyrProtocol;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {
    /** Called when the activity is first created. */
	//lots of placeholders
	BluetoothAdapter adapter = null;
	BTClient _bt;
	ZephyrProtocol _protocol;
	NewConnectedListener _NConnListener;
	private final int HEART_RATE = 0x100;
	private final int RESPIRATION_RATE = 0x101;
	private final int SKIN_TEMPERATURE = 0x102;
	private final int POSTURE = 0x103;
	private final int PEAK_ACCLERATION = 0x104;
	LinkedList<Float> values = new LinkedList<Float>();
	String[] verlabels = new String[] { "", "", "" };
	String[] horlabels = new String[] { "1", "2",  "3", "4", "5", "6", "7", "8", "9", "10"};
	float[] floatarray = new float[] {80.0f, 100.0f, 80.0f, 100.0f, 80.0f, 100.0f, 80.0f, 100.0f, 80.0f,100.0f};
	GraphView gw;
	MediaPlayer alarmPlayer;
	MediaPlayer warningPlayer;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        /*Sending a message to android that we are going to initiate a pairing request*/
        IntentFilter filter = new IntentFilter("android.bluetooth.device.action.PAIRING_REQUEST");
        /*Registering a new BTBroadcast receiver from the Main Activity context with pairing request event*/
       this.getApplicationContext().registerReceiver(new BTBroadcastReceiver(), filter);
        // Registering the BTBondReceiver in the application that the status of the receiver has changed to Paired
        IntentFilter filter2 = new IntentFilter("android.bluetooth.device.action.BOND_STATE_CHANGED");
       this.getApplicationContext().registerReceiver(new BTBondReceiver(), filter2);
        
      //Obtaining the handle to act on the CONNECT button
        EditText et = (EditText) findViewById(R.id.labelStatusMsg);
		String ErrorText  = "Enter server IPs";
		 et.setText(ErrorText);
		 
		 // Initiate media players
		 alarmPlayer = MediaPlayer.create(this, R.raw.alarm);
		 warningPlayer = MediaPlayer.create(this, R.raw.warning);
		 alarmPlayer.setLooping(true);
		 
		 //Fill value list with some data
		 for(int i = 0; i<10; i++){
			 values.add(80.0f);
		 }
		 
		gw = new GraphView(this, floatarray , "Pulse", horlabels, verlabels, GraphView.LINE);
			LinearLayout ll = (LinearLayout) findViewById(R.id.ll);
			ll.addView(gw, 400, 400);
			
			gw.refreshDrawableState();

        Button btnConnect = (Button) findViewById(R.id.ButtonConnect);
        if (btnConnect != null)
        {
        	btnConnect.setOnClickListener(new OnClickListener() {
        		public void onClick(View v) {
        			EditText ettmp = (EditText) findViewById(R.id.labelStatusMsg);
        			String boxContent = ettmp.getText().toString();
        			String BhMacID = "00:07:80:9D:8A:E8";
        			//String BhMacID = "00:07:80:88:F6:BF";
        			adapter = BluetoothAdapter.getDefaultAdapter();
        			
        			Set<BluetoothDevice> pairedDevices = adapter.getBondedDevices();
        			
        			if (pairedDevices.size() > 0) 
        			{
                        for (BluetoothDevice device : pairedDevices) 
                        {
                        	if (device.getName().startsWith("BH")) 
                        	{
                        		BluetoothDevice btDevice = device;
                        		BhMacID = btDevice.getAddress();
                                break;

                        	}
                        }
                        
                        
        			}
        			
        			//BhMacID = btDevice.getAddress();
        			BluetoothDevice Device = adapter.getRemoteDevice(BhMacID);
        			String DeviceName = Device.getName();
        			_bt = new BTClient(adapter, BhMacID);
        			_NConnListener = new NewConnectedListener(Newhandler,Newhandler);
        			_bt.addConnectedEventListener(_NConnListener);
        			NewConnectedListener.ip = boxContent;
        			System.out.println("IP IS " + NewConnectedListener.ip);
        			
        			TextView et1;
        			TextView tv1 = (TextView)findViewById(R.id.labelHeartRate);
        			tv1.setText("000");
        			
        			 
        			if(_bt.IsConnected())
        			{
        				_bt.start();
        				EditText et = (EditText) findViewById(R.id.labelStatusMsg);
        				String ErrorText  = "Connected to BioHarness "+DeviceName;
						 et.setText(ErrorText);
						 
						 //Reset all the values to 0s

        			}
        			else
        			{
        				EditText et = (EditText) findViewById(R.id.labelStatusMsg);
        				String ErrorText  = "Unable to Connect !";
						 et.setText(ErrorText);
        			}
        		}
        	});
        }
        /*Obtaining the handle to act on the DISCONNECT button*/
        Button btnDisconnect = (Button) findViewById(R.id.ButtonDisconnect);
        if (btnDisconnect != null)
        {
        	btnDisconnect.setOnClickListener(new OnClickListener() {
				@Override
				/*Functionality to act if the button DISCONNECT is touched*/
				public void onClick(View v) {
					alarmPlayer.pause();
					// TODO Auto-generated method stub
					/*Reset the global variables*/
					EditText et = (EditText) findViewById(R.id.labelStatusMsg);
    				String ErrorText  = "Disconnected from BioHarness!";
					 et.setText(ErrorText);

					/*This disconnects listener from acting on received messages*/	
					_bt.removeConnectedEventListener(_NConnListener);
					/*Close the communication with the device & throw an exception if failure*/
					_bt.Close();
				
				}
        	});
        }
    }
    private class BTBondReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			Bundle b = intent.getExtras();
			BluetoothDevice device = adapter.getRemoteDevice(b.get("android.bluetooth.device.extra.DEVICE").toString());
			Log.d("Bond state", "BOND_STATED = " + device.getBondState());
		}
    }
    private class BTBroadcastReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			Log.d("BTIntent", intent.getAction());
			Bundle b = intent.getExtras();
			Log.d("BTIntent", b.get("android.bluetooth.device.extra.DEVICE").toString());
			Log.d("BTIntent", b.get("android.bluetooth.device.extra.PAIRING_VARIANT").toString());
			try {
				BluetoothDevice device = adapter.getRemoteDevice(b.get("android.bluetooth.device.extra.DEVICE").toString());
				Method m = BluetoothDevice.class.getMethod("convertPinToBytes", new Class[] {String.class} );
				byte[] pin = (byte[])m.invoke(device, "1234");
				m = device.getClass().getMethod("setPin", new Class [] {pin.getClass()});
				Object result = m.invoke(device, pin);
				Log.d("BTTest", result.toString());
			} catch (SecurityException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (NoSuchMethodException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (IllegalArgumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
    }
    

    final  Handler Newhandler = new Handler(){
    	public void handleMessage(Message msg)
    	{
    		TextView tv;
    		TextView et;
    		switch (msg.what)
    		{
    		case HEART_RATE:
    			// Convert to Int from String
    			String HeartRatetext = msg.getData().getString("HeartRate");
    			System.out.println("Heart Rate Info is "+ HeartRatetext);
    			// Grab GUI element
    			tv = (TextView)findViewById(R.id.labelHeartRate);
    			if (tv != null)tv.setText(HeartRatetext);
    			Integer hr = Integer.parseInt(HeartRatetext);
    			//Push to graph
    			float hrf = hr.floatValue();
    			values.addFirst(hrf);
    			values.removeLast();
    			float[] farray = new float[10];
    			for (int b = 0; b<10; b++){
    				farray[b] = 240 - values.get(b);
    				System.err.print(values.get(b));
    			}
    			gw.values = farray;
    			gw.invalidate();
    			if (values.get(0) > 200.0f && values.get(1) <= 200.0f ){
    				alarmPlayer.start(); 
    				System.err.print("Start");
    			}
    			else if (values.get(0) < 60.0f && values.get(1) >= 60.0f ){
    				alarmPlayer.start();
    				System.err.print("Start");
    			}
    			else if (values.get(0) < 200.0f && values.get(1) >= 200.0f ){
    				alarmPlayer.pause();
    				System.err.print("Stop");
    			}
    			else if (values.get(0) > 60.0f && values.get(1) <= 60.0f ){
    				alarmPlayer.pause();
    				System.err.print("Stop");
    			}
    			if (Math.abs(values.get(1) - values.get(0)) > 5.0f){
    				warningPlayer.start();
    			}
    			
    			
    			
    		break;
    		
    		case RESPIRATION_RATE:
    			System.err.println("skipping resp rate display");
    		
    		break;
    		
    		case SKIN_TEMPERATURE:
    			System.err.println("skipping skin display");
    		break;
    		
    		case POSTURE:
    			System.err.println("skipping posture display");
    		
    		break;
    		
    		case PEAK_ACCLERATION:
    			System.err.println("skipping posture display");
    		break;	
    		
    		
    		}
    	}

    };
    
}


