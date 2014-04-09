package hcd;

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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
	// a string(char sequence) that separates command with parameter 
	// (used in creating command string)
	private String commandParameterSeperator;
	// a string(char sequence) that separates parameters from each other 
	// (used in creating command string)
	private String parameterSeperator;
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
	
	/**
	 * Construct a command string as configured in Command object using parameters
	 * @param command
	 * @param parameters
	 * @return
	 * @throws InvalidParameterException
	 * @throws InvalidCommandException
	 */
	public String constructCommandString(String command, ArrayList<Object> parameters) throws InvalidParameterException, InvalidCommandException {
		// 1. 	grab command from the map. if not found, throw an exception
		Command c = commandMap.get(command);
		if (c == null) {
			throw new InvalidCommandException(command + " was not found in Command Map");
		}
		
		// 2. 	start the commandString with the commandName
		String cmdString = c.getCommand();

		// 3. 	if no parameters or empty parameters were passed in, ask commander for paramList.
		//		if the paramList is not null, throw an exception
		//		else, (paramList is null, nothing else to be added to commandString) return cmdString.
		if (parameters == null || parameters.size()==0) {
			if (c.getNumParameters() > 0) {
				throw new InvalidParameterException("No parameters passed in. " + command
						+ " expects the following parameters:"+ c.getParamList());
			} else {
				return cmdString;
			}
		}
		// 4.	if some parameters are passed in, it's time to check.
		else {
		// 4-1	When configuration expects some parameters,
		// 		but number of parameters passed in does not match with configuration,
		//		throw an exception
			if (parameters.size() != c.getNumParameters()) {
				throw new InvalidParameterException("Size of input parameter list does not match with command specification from config file: " + command +" requires "+c.getNumParameters()+"parameters");
			}
		// 4-2	Going through the parameters passed in.
			int ii=0;
			for (Object o : parameters) {
		// 4-2-1	grab the parameter KEY from command object 
		//			and also grab the parameter OBJECT from the map
				String paramName = c.getParameterName(ii);
				Parameter p = paramMap.get(paramName);
		// 4-2-2 	when the parameter of the given name was not found from configuration,
		//			p will be null, then throw an exception
				if (p == null) {
					//. this shouldn't happen if configuration file is written properly.
					//. TODO: validation configuration file (somewhere else, after loading) to ensure all parameters specified for all commands has a matching parameter in paramMap
					throw new InvalidParameterException("Parameter name from configuration is not found in parameter map.");
				}
				
		//4-2-3 parse the object to string, throws exception if type mismatch
				String parsed = p.objectToString(o);

		//4-2-4	if this parameter is first parameter, add commandParameterSeperator before
				//		else, add parameterSeperator before.
				if (ii == 0) {
					cmdString = cmdString + commandParameterSeperator	+ parsed;
				} else {
					cmdString = cmdString + parameterSeperator + parsed;
				}
				ii++;
			}
		}
		return cmdString;
	}
	
	/**
	 * Submit the command/request to the hardware and return the response 
	 * as an ArrayList formatted as configured in Command object.
	 * @param command
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
	public ArrayList<Object> submit(String command, ArrayList<Object> parameters) throws InvalidParameterException, InvalidCommandException, StringIndexOutOfBoundsException, SocketTimeoutException, UnknownHostException, IOException, InvalidOutputException{
		Command cmd = commandMap.get(command);
		String cmdString = constructCommandString(command,parameters);
		
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
	public ArrayList<Object> submit(String command) throws InvalidParameterException, InvalidCommandException, StringIndexOutOfBoundsException, SocketTimeoutException, UnknownHostException, IOException, InvalidOutputException{
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
	public ArrayList<Object> submit(String command, Object[] parameters) throws StringIndexOutOfBoundsException, SocketTimeoutException, UnknownHostException, InvalidParameterException, InvalidCommandException, IOException, InvalidOutputException{
		return submit(command,new ArrayList<Object>(Arrays.asList(parameters)));
	}
	/**
	 * Overloading of submit(String, ArrayList<Object>). 
	 * Converts the String[] as ArrayList<Object> as configured in Parameter
	 * @param command
	 * @param parameters
	 * @return
	 * @throws InvalidParameterException
	 * @throws StringIndexOutOfBoundsException
	 * @throws SocketTimeoutException
	 * @throws UnknownHostException
	 * @throws InvalidCommandException
	 * @throws IOException
	 * @throws InvalidOutputException
	 */
	public ArrayList<Object> submit(String command, String[] parameters) throws InvalidParameterException, StringIndexOutOfBoundsException, SocketTimeoutException, UnknownHostException, InvalidCommandException, IOException, InvalidOutputException{
		Command cmd = commandMap.get(command);
		ArrayList<Object> params = new ArrayList<Object>();
		for(int ii = 0; ii<parameters.length; ii++){
			System.out.println(cmd.getParameterName(ii));
			Parameter p = paramMap.get(cmd.getParameterName(ii));
			params.add(p.stringToObject(parameters[ii]));
		}
		
		return submit(command,params);
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

	public ArrayList<Object> formatOutputObject(Command cmd, String response) throws InvalidParameterException{
		String happyformat = null;
		String sadformat = null;
		String rf, es;
		int numItem;
		ArrayList<Object> output = new ArrayList<Object>();
		
		if((rf = cmd.constructResponseFormat(paramMap))!= null){happyformat = rf;}
		if((es = cmd.getErrorStatusRegex()) != null){sadformat = es;}
		else if(deviceErrorStatusRegex != null){sadformat = deviceErrorStatusRegex;} 
		
		if(happyformat!= null){
			Pattern hpattern = Pattern.compile(happyformat, Pattern.DOTALL);
			Matcher hmatcher = hpattern.matcher(response);	
			numItem = hmatcher.groupCount();
			while (hmatcher.find()){
				if(hmatcher.group(1).length()>0){
					ArrayList<Object> outArray = new ArrayList<Object>();
					// going through the match
					for(int i = 0; i<numItem; i++){
						// get the parameter object and get the corresponding string
						Parameter p = paramMap.get(cmd.getResponseList().get(i));
						String s = hmatcher.group(i+1);
						// convert the string into object, and add to Array
						outArray.add(p.stringToObject(s));
					}
					// add the object to ArrayList
					output.add(outArray);
				}	
			}
		}
		if(output.size()==0 && sadformat != null)
		{
			Pattern spattern = Pattern.compile(sadformat, Pattern.DOTALL);
			Matcher smatcher = spattern.matcher(response);
			numItem = smatcher.groupCount();
			while (smatcher.find()){
				ArrayList<String> outArray = new ArrayList<String>();
				outArray.add("*");
				for(int ii = 0; ii<numItem; ii++){
					outArray.add(smatcher.group(ii+1));
				}
				output.add(outArray);
			}
		}
		// ArrayList that will be output
								// number of items will be in Array
		
		
		// For ever match of the response format found,
		
		
		
		//TODO errorcase(whileloop condition)
		//TODO multiline support(newline parameter, regex dotall)
		//TODO one line okay, other bad?????
		//TODO server for hardware simulator?
		return output;
	}
}
