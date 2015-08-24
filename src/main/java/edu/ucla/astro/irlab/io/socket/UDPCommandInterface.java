package edu.ucla.astro.irlab.io.socket;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Writer;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.HashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.ucla.astro.irlab.io.CommandInterface;
import edu.ucla.astro.irlab.io.InvalidConfigurationException;
import edu.ucla.astro.irlab.io.InvalidOutputException;
import edu.ucla.astro.irlab.io.Parameter;
import edu.ucla.astro.irlab.io.PositionalSyntaxCommandConstructor;
//TODO separate TCP and UDP
public class UDPCommandInterface implements CommandInterface {
	// Socket Information
	private String host;
	private int port;
	private String commandTerminator;
	private int maxBuffer;
	private int timeout;

	// UDP socket
	DatagramSocket udpSocket;
		
	// boolean monitoring socket connection status
	public transient boolean connected = false;
	// Log4J logger. (might change to custom logging) 
	private transient Logger logger;
	
	
	// setting logger.
	public void setLogger(Object loggerName) {
		logger= LogManager.getLogger(loggerName);
	}
	public void setLogger(Logger lgr) {
		logger = lgr;
	}

	public UDPCommandInterface(HashMap<String, String> settings) {
		this.host = settings.get("host");
		this.port = Integer.parseInt(settings.get("port"));
		this.commandTerminator = settings.get("commandTerminator");
		this.maxBuffer = Integer.parseInt(settings.get("maxBuffer"));
		this.timeout = Integer.parseInt(settings.get("timeout"));
	}
	public UDPCommandInterface(String host, int port, String terminator, int timeout, int buffersize) {
		this.host = host;
		this.port = port;
		this.commandTerminator = terminator;
		this.maxBuffer = buffersize;
		this.timeout = timeout;
	}

	@Override
	public void connect() throws IOException  {
		if (!connected) {
			if (logger==null) {
				logger= LogManager.getLogger(UDPCommandInterface.class);
			}
			udpSocket = new DatagramSocket();
			udpSocket.setSoTimeout(timeout);		
		} else {
			logger.debug("Already connected to {}, port {}", host, port);
		}
	}
	
	@Override
	public void disconnect() throws IOException{
		if (connected) {
			logger.debug("Closing socket Connection");
			udpSocket.close();
			connected = false;
		} else {
			logger.debug("Not connected to {}, port {}", host, port);
		}
	}

	@Override
	public void sendCommand(String command, Parameter[] arguments)	throws IOException {
		
		String commandString = PositionalSyntaxCommandConstructor.constructCommandString(command, arguments);
		
		logger.debug("Sending command, {}", commandString);
		String input = commandString + commandTerminator;
		byte[] bytecommand = input.getBytes();
		InetAddress address = InetAddress.getByName(host);
	    DatagramPacket packet = new DatagramPacket(bytecommand, bytecommand.length, address, port);
	    udpSocket.send(packet);
	} 

	public Object sendRequest(String command, Parameter[] arguments) throws IOException {
			String response = "";
			sendCommand(command,arguments);
			
			logger.debug("Receiving packet...");
			byte[] returned = new byte[maxBuffer];
		    DatagramPacket packet = new DatagramPacket(returned, returned.length);
		    udpSocket.receive(packet);		        
		    response = new String(packet.getData(), 0, packet.getLength());
			return response.trim();
	}
	
	
	@Override
	public void constructFromConfigurationFile(String path) throws IOException {
		// TODO Auto-generated method stub
		
	}
	


	}
