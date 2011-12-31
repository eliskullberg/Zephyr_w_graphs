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
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

/* 
 * CareNet Zephyr Android application
 * Code an extension on BioHarness BT Application by Zephyr 
 * Intended for academic/educational purposes. 
 * Contact: elisk@kth.se
 * 
 */

public class MainActivity extends Activity {
    /** Called when the activity is first created. */
	public BluetoothAdapter adapter = null;
	public BTClient _bt;
	public ZephyrProtocol _protocol;
	public NewConnectedListener _NConnListener;
	private final int HEART_RATE = 0x100;
	private final int RESPIRATION_RATE = 0x101;
	private final int SKIN_TEMPERATURE = 0x102;
	private final int POSTURE = 0x103;
	private final int PEAK_ACCLERATION = 0x104;
	private LinkedList<Float> values = new LinkedList<Float>();
	private String[] verlabels = new String[] { "220", "140", "60" };
	private String[] horlabels = new String[] { "", "",  "", "", "", "", "", "", "", ""};
	float[] floatarray = new float[] {80.0f, 100.0f, 80.0f, 100.0f, 80.0f, 100.0f, 80.0f, 100.0f, 80.0f,80.0f};
	private GraphView gw;
	private MediaPlayer alarmPlayer;
	static WebUpload w = new WebUpload();
	private MediaPlayer warningPlayer;
	static String patientId;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        //Set intent filters so we are notified for pairing changes
        IntentFilter filter = new IntentFilter("android.bluetooth.device.action.PAIRING_REQUEST");
        this.getApplicationContext().registerReceiver(new BTBroadcastReceiver(), filter);
        IntentFilter filter2 = new IntentFilter("android.bluetooth.device.action.BOND_STATE_CHANGED");
        this.getApplicationContext().registerReceiver(new BTBondReceiver(), filter2);
         
        // Initiate media players
		 alarmPlayer = MediaPlayer.create(this, R.raw.alarm);
		 warningPlayer = MediaPlayer.create(this, R.raw.warning);
		 alarmPlayer.setLooping(true);
		 
		 //Fill value list with some initial-data
		 for(int i = 0; i<10; i++){
			 values.add(80.0f);
		 }
		 
	
		//Logic for what happens when we press the connect button
		 Button btnConnect = (Button) findViewById(R.id.ButtonConnect);
	        if (btnConnect != null)
	        {
	        	btnConnect.setOnClickListener(new OnClickListener() {
	        		public void onClick(View v) {
	        			EditText user = (EditText) findViewById(R.id.patientIdBox);
	        			MainActivity.patientId = user.getText().toString();
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
        
        			
        			//Set heart rate label to zero, in case this is a reconnect
        			TextView et1;
        			TextView tv1 = (TextView)findViewById(R.id.labelHeartRate);
        			tv1.setText("00");
        			
        			//If we manage to connect, tell user with a toast, otherwise, present a different toast
        			if(_bt.IsConnected())
        			{
        				_bt.start();
        				String text  = "Connected to BioHarness "+DeviceName;
        				Toast toast = Toast.makeText(getApplicationContext(), text, 4);
        				toast.show();
						 
        			}
        			else
        			{
        				String text  = "Unable to Connect !";
        				Toast toast = Toast.makeText(getApplicationContext(), text, 4);
        				toast.show();
        			}
        		}
        	});
        }
        
        //Logic for disconnect button
        Button btnDisconnect = (Button) findViewById(R.id.ButtonDisconnect);
        if (btnDisconnect != null)
        {
        	btnDisconnect.setOnClickListener(new OnClickListener() {
				@Override
				
				public void onClick(View v) {
					//Stop any alarm that may be annoying the user
					alarmPlayer.pause();
					//Present a toast 
    				String text  = "Disconnected from BioHarness!";
					// et.setText(ErrorText);
					Toast toast = Toast.makeText(getApplicationContext(), text, 4);
    				toast.show();
					//Be well behaved and disconnect graciously	
					_bt.removeConnectedEventListener(_NConnListener);
					_bt.Close();
				
				}
        	});
        }
        //Initiate the graph and attatch it to the linearlayout
    	gw = new GraphView(this, floatarray , "Pulse", horlabels, verlabels, GraphView.LINE);
		LayoutParams lparams = new LayoutParams(LayoutParams.WRAP_CONTENT, 200);
		LinearLayout ll = (LinearLayout) findViewById(R.id.ll);
		gw.setLayoutParams(lparams);
		ll.addView(gw, 2);
		//Redraw, or you wont see it on startup
		gw.refreshDrawableState();
    }
    
    // Logic for what happens when we are interrupted by a successful bond found. Unchanged from zephyr initial app. 
    private class BTBondReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			Bundle b = intent.getExtras();
			BluetoothDevice device = adapter.getRemoteDevice(b.get("android.bluetooth.device.extra.DEVICE").toString());
			Log.d("Bond state", "BOND_STATED = " + device.getBondState());
		}
    }
    
    // Logic for how to initially connect with the device (general for any bt device). Unchanged from initial Zephyr app. 
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
    
    // This handler takes care of events sent by the NewConnectedListener class
    final  Handler Newhandler = new Handler(){
    	public void handleMessage(Message msg)
    	{
    		TextView tv;
    		TextView et;
    		switch (msg.what)
    		{
    		//We only care about heart rate in this app
    		case HEART_RATE:
    			//Update GUI with new heart rate
    			String HeartRatetext = msg.getData().getString("HeartRate");
    			System.out.println("Heart Rate Info is "+ HeartRatetext);
    			tv = (TextView)findViewById(R.id.labelHeartRate);
    			MainActivity.w.send(HeartRatetext, "elis");
    			if (tv != null)tv.setText(HeartRatetext);
    			Integer hr = Integer.parseInt(HeartRatetext);
    			// Also push it to the graph, by updating its underlyng datastructure
    			// Two arrays are used, since I never figured out how to typecast Float[] to float[]
    			float hrf = hr.floatValue();
    			values.addFirst(hrf);
    			values.removeLast();
    			float[] farray = new float[10];
    			for (int b = 0; b<10; b++){
    				farray[b] = values.get(b);
    				System.err.print(values.get(b));
    			}
    			// This is where we change the data of graph and redraw it
    			gw.values = farray;
    			gw.invalidate();

    			// This is logic for it we need to start or stop an audio alarm
    			if (values.get(0) > 200.0f && values.get(1) <= 200.0f ){
    				alarmPlayer.start(); 
    				System.err.print("Start");
    				tv.setTextColor(Color.RED);
    			}
    			else if (values.get(0) < 60.0f && values.get(1) >= 60.0f ){
    				alarmPlayer.start();
    				System.err.print("Start");
    				tv.setTextColor(Color.RED);
    			}
    			else if (values.get(0) < 200.0f && values.get(1) >= 200.0f ){
    				alarmPlayer.pause();
    				System.err.print("Stop");
    				tv.setTextColor(Color.LTGRAY);
    			}
    			else if (values.get(0) > 60.0f && values.get(1) <= 60.0f ){
    				alarmPlayer.pause();
    				System.err.print("Stop");
    				tv.setTextColor(Color.LTGRAY);
    			}
    			if (Math.abs(values.get(1) - values.get(0)) > 5.0f){
    				warningPlayer.start();
    			}
    			
    			
    			
    		break;
    		
    		// We dont care about these packets in this app
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


