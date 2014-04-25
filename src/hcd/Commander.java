package hcd;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.Gson;

public class Commander {
	
	// name of the hardware
	private String name;
	// type of Connection udp or tcp
	private String connection;
	// host and port information of the terminal server
	private String host;
	private int port;
	// socket timeout
	private int timeout;
	// terminator for command/request 
	private String commandTerminator;
	// terminator for the response from the hardware, 
	// in case of being different than command terminator
	private String responseTerminator;
	//a string that indicates if the response has error or has status info.
	private String deviceErrorStatusRegex;
	private String errorStatusNotifier;
	// Maps of Command Object and Parameter Objects that will hold references.
	private HashMap<String, Command> commandMap;
	private HashMap<String, Parameter> paramMap;
	
	// TCP socket and I/O. used Writer instead of PrintWriter 
	// because JSON gives error with PrintWriter
	private Socket tcpSocket;
	private Writer out;
	private BufferedReader in;
	
	// UDP socket and I/O.
	private DatagramSocket udpSocket;
	
	// boolean monitoring socket connection status
	public boolean connected = false;
	// Log4J logger. (might change to custom logging) 
	private Logger logger;
	// setting logger.
	
	public void setLogger(String loggerName){
		logger= LogManager.getLogger(loggerName);
	}
	public static Commander getCommanderObject(String path) throws FileNotFoundException{
		String jsonFilePath = path; 
		Gson gson = new Gson();
		FileReader fr = new FileReader(jsonFilePath);
		Commander cmdr = gson.fromJson(fr, Commander.class);
		cmdr.setLogger(cmdr.getName());
	
		return cmdr;
	}
	
	/**
	 * For TCP, connects to the socket, and leave it connected until disconnect is called
	 * For UDP, construct udpSocket.
	 * uses socket information (host and port) read from JSON configuration
	 * @throws UnknownHostException
	 * @throws IOException
	 */
	public void connect() throws UnknownHostException, IOException{
		if(!connected){
			if(connection.equalsIgnoreCase("tcp")){
				logger.debug("Opening TCP socket on {} port {}", host, port);
				tcpSocket = new Socket(host, port);

				logger.debug("Socket Timeout set to {} milliseconds.", timeout);
				tcpSocket.setSoTimeout(timeout);

				logger.debug("Opening I/O stream");
				out = new PrintWriter(tcpSocket.getOutputStream(), true);
				in = new BufferedReader(new InputStreamReader(tcpSocket.getInputStream()));
				connected = true;
			}else if (connection.equalsIgnoreCase("udp")){
				udpSocket = new DatagramSocket();
				connected = true;
			}else {
				logger.debug("No proper connection type is set");
			}
		} else {
			System.out.format("Already connected to %s, port %d", host, port);
		}
	}
	/**
	 * Disconnects from the tcp socket and closes I/O stream.
	 * @throws IOException
	 */
	public void disconnect() throws IOException{
		if(connected){
			logger.debug("Closing I/O stream");
			out.flush();
			out.close();
			in.close();
			logger.debug("Closing socket on {} port {}", host, port);
			tcpSocket.close();
			connected = false;
		} else {
			System.out.format("Not connected to %s, port %d", host, port);
		}
	}
	
	
	/**
	 * Send commands through the socket with no response expected.
	 * @param commandString
	 * @throws UnknownHostException
	 * @throws IOException
	 */
	public void sendCommand(String commandString) throws UnknownHostException, IOException{
		logger.debug("Sending command, {}", commandString);
		if(connection.equalsIgnoreCase("tcp")){
			((PrintWriter) out).print(commandString + commandTerminator);
		}else if(connection.equalsIgnoreCase("udp")){
			byte[] data = commandString.getBytes();
			DatagramPacket packSent = new DatagramPacket(data, data.length, InetAddress.getByName(host), port);
			udpSocket.send(packSent);
		}
	}
	/**
	 * Send request through the socket and return the response as String.
	 * case 1 : if number of characters of the response is known, read that many characters
	 * case 2 : if terminator of response is known, read until the terminator is reached.
	 * case 3 : if neither, wait until socket times out.
	 * 
	 * @param commandString
	 * @param outlength
	 * @return
	 * @throws UnknownHostException
	 * @throws IOException
	 * @throws StringIndexOutOfBoundsException
	 * @throws SocketTimeoutException
	 * @throws InvalidOutputException
	 */
	public String sendRequest(String commandString, int outlength)
			throws UnknownHostException, IOException,
			StringIndexOutOfBoundsException, SocketTimeoutException, InvalidOutputException {
		String response = "";
		if(connection.equalsIgnoreCase("tcp")){
			StringBuffer line = new StringBuffer();
			
			logger.debug("Sending command, {}", commandString);
			((PrintWriter) out).println(commandString + commandTerminator);

			// When the terminator of the output is known, read until the terminator has reached
			if (responseTerminator != null) {
				int temp;
				logger.debug("Reading until terminator is reached.");
				while (line.indexOf(responseTerminator) == -1) {
					temp = in.read();
					line.append((char) temp);
					int index = line.indexOf(responseTerminator);
			// There were some occasion that terminator was read multiple times
			// before actual response was read. to avoid this,
			// if the terminator is the first characters read, empty the buffer
			// so it won't cause the loop to exit.
					if (index == 0){
						line.setLength(0);
					}
					if (index != -1) {
						response = line.toString().substring(0, index);
					}
				}
			}
			// When number of characters for the output is known, only read that many characters
			else if (outlength > 0) {
				logger.debug("Reading {} characters of response.", outlength);
				int temp;
				for (int i = 0; i < outlength; i++) {
					temp = in.read();
					line.append((char) temp);
				}
				response = line.toString();
			// empty the buffer string
				line.delete(0, line.length() - 1);
			}
			// Read until socket times out, then loop will exit by the SocketTimeoutException
			// Exception is caught here since it is what we expect to happen.
			else {
				logger.debug("Reading response");
				try {
					int temp;
					while (true) {
						temp = in.read();
						if(temp!=0){
							line.append((char) temp);	
						}
					}
				} catch (SocketTimeoutException e) {
					logger.debug("Socket Timeout");
				} finally {
			// when the socket times out, try to put together the response.
					logger.debug("Retriveing output");
			// if line is length of 0, it means no response, 
			// hence throw invalidOutputException after closing.
					response = line.toString();
					if (response.length() == 0) {
						throw new InvalidOutputException("No output was read for command " + commandString);
					}
				}
			}
		}else if(connection.equalsIgnoreCase("udp")){
			DatagramPacket packet = new DatagramPacket(new byte[256], 256);
			sendCommand(commandString);
			udpSocket.receive(packet);
			response = new String(packet.getData(),0,packet.getLength());
		}
		return response;

	}
	/**
	 * Overloading of String sendRequest. considering number of characters are not known
	 * @param commandString
	 * @return
	 * @throws UnknownHostException
	 * @throws IOException
	 * @throws StringIndexOutOfBoundsException
	 * @throws InvalidOutputException
	 */
	public String sendRequest(String commandString)
			throws UnknownHostException, IOException, StringIndexOutOfBoundsException, InvalidOutputException {
		return sendRequest(commandString, -1);
	}	
	
	
	public String constructCommandString(String cmdKey, ArrayList<Object> parameters) throws InvalidParameterException{
		Command c = getCommand(cmdKey);
		String cmdstr = c.getCommand();
		Pattern pattern = Pattern.compile("(\\(\\w*?\\))");
		Matcher matcher = pattern.matcher(cmdstr);

		int counter = 0;
		while(matcher.find()){
			counter ++;
			if(counter <= parameters.size()){
				String paramKey = matcher.group().replace("(","").replace(")","");
				Parameter p = paramMap.get(paramKey);
				String paramVal = p.objectToString(parameters.get(counter-1));
				cmdstr = matcher.replaceFirst(paramVal);
				matcher = pattern.matcher(cmdstr);
			}	
		}
		if(counter != parameters.size()){
			throw new InvalidParameterException("Number of parameters does not match. " + counter + " required, " + parameters.size() + " entered.");
		}
		
				
		return cmdstr;
	}
	
	
	/**
	 * Submit the command/request to the hardware and return the response 
	 * as an ArrayList formatted as configured in Command object.
	 * @param cmdKey
	 * @param parameters
	 * @return
	 * @throws InvalidParameterException
	 * @throws InvalidCommandException
	 * @throws StringIndexOutOfBoundsException
	 * @throws SocketTimeoutException
	 * @throws UnknownHostException
	 * @throws IOException
	 * @throws InvalidOutputException
	 */
	public ArrayList<ArrayList<Object>> submit(String cmdKey, ArrayList<Object> parameters) throws InvalidParameterException, InvalidCommandException, StringIndexOutOfBoundsException, SocketTimeoutException, UnknownHostException, IOException, InvalidOutputException{
		Command cmd = getCommand(cmdKey);
		String cmdString = constructCommandString(cmdKey,parameters);
		
		String response = null;
		
		if (cmd.isRequest()||deviceErrorStatusRegex != null || cmd.getErrorStatusRegex()!= null) {
			response = sendRequest(cmdString);
		}else {
			sendCommand(cmdString);
			return null;
		}
		logger.debug("Response to " + cmdString + "\n" + response);

		return formatOutputObject(cmd, response);
	}
	/**
	 * Overloading of submit(String, ArrayList<Object>). 
	 * Enters an empty ArrayList for command with no response.
	 * @param command
	 * @return
	 * @throws InvalidParameterException
	 * @throws InvalidCommandException
	 * @throws StringIndexOutOfBoundsException
	 * @throws SocketTimeoutException
	 * @throws UnknownHostException
	 * @throws IOException
	 * @throws InvalidOutputException
	 */
	public ArrayList<ArrayList<Object>> submit(String command) throws InvalidParameterException, InvalidCommandException, StringIndexOutOfBoundsException, SocketTimeoutException, UnknownHostException, IOException, InvalidOutputException{
		return submit(command, new ArrayList<Object>());
	}
	/**
	 * Overloading of submit(String, ArrayList<Object>).
	 * converts the Array as ArrayList.
	 * @param command
	 * @param parameters
	 * @return
	 * @throws StringIndexOutOfBoundsException
	 * @throws SocketTimeoutException
	 * @throws UnknownHostException
	 * @throws InvalidParameterException
	 * @throws InvalidCommandException
	 * @throws IOException
	 * @throws InvalidOutputException
	 */
	public ArrayList<ArrayList<Object>> submit(String command, Object[] parameters) throws StringIndexOutOfBoundsException, SocketTimeoutException, UnknownHostException, InvalidParameterException, InvalidCommandException, IOException, InvalidOutputException{
		return submit(command,new ArrayList<Object>(Arrays.asList(parameters)));
	}
	public ArrayList<ArrayList<Object>> submit(String command, String[] parameters) throws InvalidParameterException, StringIndexOutOfBoundsException, SocketTimeoutException, UnknownHostException, InvalidCommandException, IOException, InvalidOutputException{
		ArrayList<String> paramList = getCommand(command).getParamList();
		Object[] objectArray = new Object[parameters.length];
		if(paramList.size() != parameters.length){
			throw new InvalidParameterException("Number of parameters does not match. " + paramList.size() + " needed, " + parameters.length + " entered.");
		} else {
			for(int ii = 0; ii<parameters.length; ii++){
				String p = paramList.get(ii);
				objectArray[ii] = getParam(p).stringToObject(parameters[ii]);
			}
		}
		return submit(command,objectArray);
	}

	
	/**
	 * Returns the HashMap of Command objects
	 * @return
	 */
	public HashMap<String, Command> getCommandMap() { return commandMap;} 
	/**
	 * Returns the HashMap of Parameter objects
	 * @return
	 */
	public HashMap<String, Parameter> getParamMap() {return paramMap;}
	/** 
	 * Returns the name of the Hardware
	 * @return
	 */
	public String getName(){return name;}
	/**
	 * Returns the command object using the key
	 * @param key
	 * @return
	 */
	public Command getCommand(String key){
		return commandMap.get(key);
	}
	/**
	 * returns the parameter object using the key
	 * @param key
	 * @return
	 */
	public Parameter getParam(String key){
		return paramMap.get(key);
	}

	// TODO unknown number of outputs?!
	// TODO format to regex method
	// TODO way to configure grouping symbol?
	public ArrayList<ArrayList<Object>> formatOutputObject(Command cmd, String response) throws InvalidParameterException{
		ArrayList<ArrayList<Object>> output = new ArrayList<ArrayList<Object>>();
		ArrayList<Parameter> responseList = new ArrayList<Parameter>();
		String happyformat = null;
		String sadformat = null;
		// use responseFormat from json to create actual regex.
		if(response!= null){
			happyformat = cmd.getResponseFormat();
			Pattern pattern = Pattern.compile("(\\(\\w*?\\))");
			Matcher matcher = pattern.matcher(happyformat);
			
			while(matcher.find()){
				String found = matcher.group();
				String paramKey = found.replace("(","").replace(")","");
				Parameter p = paramMap.get(paramKey);
				responseList.add(p);
				String paramRegex = p.getDataFormat();
				int open = matcher.start();
				int close = matcher.end();
				happyformat = happyformat.substring(0, open) + paramRegex + happyformat.substring(close);
				matcher = pattern.matcher(happyformat);
			}
		}
		// creating actual errorStatusRegex
		if(cmd.getErrorStatusRegex() != null){sadformat = cmd.getErrorStatusRegex();}
		else if(deviceErrorStatusRegex != null){sadformat = deviceErrorStatusRegex;} 
		
		// if happy format is expected, check happy response first
		if(happyformat!= null){
			Pattern hpattern = Pattern.compile(happyformat, Pattern.DOTALL);
			Matcher hmatcher = hpattern.matcher(response);	
			int numItem = hmatcher.groupCount();
			while (hmatcher.find()){
				ArrayList<Object> outArray = new ArrayList<Object>();
				// going through the match
				for(int i = 0; i<numItem; i++){
					String s = hmatcher.group(i+1);
					// convert the string into object, and add to Array
					outArray.add(responseList.get(i).stringToObject(s));
				}
				// add the object to ArrayList
				output.add(outArray);
			}
		}
		// if happyformat was not matched, check if it matches sadformat
		if(output.size()==0 && sadformat != null)
		{
			Pattern spattern = Pattern.compile(sadformat, Pattern.DOTALL);
			Matcher smatcher = spattern.matcher(response);
			int numItem = smatcher.groupCount();
			while (smatcher.find()){
				ArrayList<Object> outArray = new ArrayList<Object>();
				outArray.add(errorStatusNotifier);
				for(int ii = 0; ii<numItem; ii++){
					outArray.add(smatcher.group(ii+1));
				}
				output.add(outArray);
			}
		}
		return output;
	}
	public ArrayList<ArrayList<Object>> formatOutputObject(String cmdKey, String response) throws InvalidParameterException{
		return formatOutputObject(getCommand(cmdKey),response);
	}
}
