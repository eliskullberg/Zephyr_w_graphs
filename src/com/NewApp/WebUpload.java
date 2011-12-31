package com.NewApp;

import java.io.DataOutputStream;
import java.io.PrintWriter;
import java.net.Socket;

import org.json.simple.JSONObject;

public class WebUpload
{
	String ip = "mrs.carenet-se.se";
	Socket s = null;
	DataOutputStream out = null;
	PrintWriter outToServer;
	
	public WebUpload()
	{
		try {
			s = new Socket(ip, 9999);
			out = new DataOutputStream(s.getOutputStream());
			outToServer = new PrintWriter(out, true);
		} catch (Exception  e) {
			// TODO Auto-generated catch block
			System.out.println("Error initiating socket");
			e.printStackTrace();
		}
	}
	
	public void send(String hr, String patientId)
	{
		try {
			JSONObject obj=new JSONObject();
			obj.put("hr",hr);
			obj.put("spo",new Integer(99));
			obj.put("patientId",patientId);
			outToServer.println(obj);
			System.out.println(obj);
			//out.flush();
			
		}
		catch (Exception e){
			System.err.println("Error sending");
		}
		
		
	}
	 

}
