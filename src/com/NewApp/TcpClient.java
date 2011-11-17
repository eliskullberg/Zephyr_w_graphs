package com.NewApp;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;

public class TcpClient {
	
	Socket s = null;
	
	public TcpClient(String hostname, int port){
	
	String HOSTNAME = hostname;
	int PORT = port;
	InetAddress IPADDR;
	try {
		IPADDR = InetAddress.getByName(HOSTNAME);
		s = new Socket(IPADDR, PORT);
	} catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
		
	}
	
	
	public boolean send(byte [] data) {
		
		try {
			OutputStream output = s.getOutputStream();
			output.write(data);
			output.flush();
			System.err.println(data.toString());
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
		}
		
		
		return true;
	}

}
