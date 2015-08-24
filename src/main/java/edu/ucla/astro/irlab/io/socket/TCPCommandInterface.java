package edu.ucla.astro.irlab.io.socket;

import java.io.BufferedReader;
import java.io.Flushable;
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
public class TCPCommandInterface implements CommandInterface {
	// Socket informations
	private String host;
	private int port;
	private String commandTerminator;
	private String responseTerminator;
	private int timeout;
	
	// TCP socket and I/O. used Writer instead of PrintWriter 
	// because JSON gives error with PrintWriter
	private transient Socket tcpSocket;
	private transient Writer out;
	private transient BufferedReader in;
	
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

	public TCPCommandInterface(HashMap<String, String> settings) {
		this.host = settings.get("host");
		this.port = Integer.parseInt(settings.get("port"));
		this.commandTerminator = settings.get("commandTerminator");
		this.responseTerminator = settings.get("responseTerminator");
		this.timeout = Integer.parseInt(settings.get("timeout"));
	}
	public TCPCommandInterface(String host, int port, String commandTerminator, String responseTerminator, int timeout) {
		this.host = host;
		this.port = port;
		this.commandTerminator = commandTerminator;
		this.responseTerminator = responseTerminator;
		this.timeout = timeout;
	}

	/**
	 * For TCP, connects to the socket, and leave it connected until disconnect is called
	 * For UDP, construct udpSocket.
	 * uses socket information (host and port) read from JSON configuration
	 * @throws IOException 
	 */
	@Override
	public void connect() throws IOException  {
		if (!connected) {
			if (logger==null) {
				logger= LogManager.getLogger(TCPCommandInterface.class);
			}
			logger.debug("Opening TCP socket on {} port {}", host, port);
			tcpSocket = new Socket(host, port);

			logger.debug("Socket Timeout set to {} milliseconds.", timeout);
			tcpSocket.setSoTimeout(timeout);

			logger.debug("Opening I/O stream");
			out = new PrintWriter(tcpSocket.getOutputStream(), true);
			in = new BufferedReader(new InputStreamReader(tcpSocket.getInputStream()));
			connected = true;
		} else {
			logger.debug("Already connected to {}, port {}", host, port);
		}
	}
	/**
	 * Disconnects from the tcp socket and closes I/O stream.
	 * @throws IOException
	 */
	@Override
	public void disconnect() throws IOException{
		if (connected) {
			logger.debug("Closing socket Connection");
			
			out.flush();
			out.close();
			in.close();
			tcpSocket.close();
			connected = false;
		
		} else {
			logger.debug("Not connected to {}, port {}", host, port);
		}
	}
	/**
	 * Send commands through the socket with no response expected.
	 * @param commandString
	 * @throws UnknownHostException
	 * @throws IOException
	 */
	@Override
	public void sendCommand(String command, Parameter[] arguments)	throws IOException {
		
		String commandString = PositionalSyntaxCommandConstructor.constructCommandString(command, arguments);
		
		logger.debug("Sending command, {}", commandString);

		((PrintWriter) out).println(commandString + commandTerminator);
	}

	
	@Override
	public Object sendRequest(String command, Parameter[] arguments) throws IOException {
		
			String response = "";
			sendCommand(command,arguments);
			
			if (responseTerminator!=null && !responseTerminator.equals("")) {
				response = readEndbyTerm();
			} else {
				response = readEndbyTime();
			}
//			System.out.println(Arrays.toString(response.getBytes()));
//			System.out.println(response.trim());
			logger.debug("Response received : {}", response.trim());
			return response.trim();

	}
	
	private String readEndbyTime() throws IOException{
		StringBuffer line = new StringBuffer();
		logger.debug("Waiting for stream to be ready");
		long start = System.currentTimeMillis();
		long current = System.currentTimeMillis();
		while(!in.ready() && current-start<timeout) {
			try {
				Thread.sleep(5);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			current = System.currentTimeMillis();
		}
		if (in.ready()) {
			logger.debug("Reading Stream...");
			while(in.ready()) {
				line.append((char)in.read());
				if (!in.ready()) {
					try {
						Thread.sleep(timeout/100);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}	
			}
			return line.toString();
		} else {
			throw new SocketTimeoutException(String.format("Nothing was read from %s:%d for %d millisecond", host, port, timeout));
		}
	}

	private String readEndbyTerm() throws IOException{
		StringBuffer line = new StringBuffer();
		int temp;
		logger.debug("Reading until terminator is reached.");
		while (line.indexOf(responseTerminator) == -1 || line.indexOf(responseTerminator) == 0 ) {
			if (line.indexOf(responseTerminator)==0) {
				line.delete(0, responseTerminator.length());
			}
			temp = in.read();
			line.append((char) temp);
			int index = line.indexOf(responseTerminator);
			// There were some occasion that terminator was read multiple times
			// before actual response was read. to avoid this,
			// if the terminator is the first characters read, empty the buffer
			// so it won't cause the loop to exit.
		}
		return line.toString();
	}
	@Override
	public void constructFromConfigurationFile(String path) throws IOException {
		// TODO Auto-generated method stub
		
	}
	
}
