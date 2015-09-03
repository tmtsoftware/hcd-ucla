package edu.ucla.astro.irlab.io;


import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import edu.ucla.astro.irlab.io.ice.ICECommandInterface;
import edu.ucla.astro.irlab.io.serial.SerialCommandInterface;
import edu.ucla.astro.irlab.io.socket.TCPCommandInterface;
import edu.ucla.astro.irlab.io.socket.UDPCommandInterface;
/**
 *	@author Ji Man Sohn @ UCLA Infrared Laboratory
 *	Commander class holds informations of a hardware and is responsible to communicate with the hardware 
 */
public class Commander {
	/** Name of the Commander, usually the name of the hardware */
	private String name;
	/** If it exist, regular expression of the device-specific error indicator*/
	private String deviceSadRegex;
	/** User-defined error Notifier that will be first element of output*/
	private String errorNotifier;
	/** HashMap of properties for connecting to the hardware. Generated from JSON*/
	private HashMap<String, String> connectionProperties;
	/** HashMap of Command class objects with their name as keys. Generated from JSON*/
	private HashMap<String, Command> commandMap;
	/** HashMap of ParameterDefinition class objects with their name as keys. Generated from JSON*/
	private HashMap<String, ParameterDefinition> paramDefMap;
	/** Boolean token that indicates connection status*/
	public transient boolean connected = false;
	/** Log4J logger. (might change to custom logging)*/ 
	public transient Logger logger;
	/** CommandInterface augo-generated using connectionProperties */
	private transient CommandInterface interfaceConnection;
	/** 
	 * HashMap of ParameterDefinition class objects with their name as keys. 
	 * Primitive data types of JAVA are predefined as default ParameterDefinitions
	 */
	public static final transient HashMap<String, ParameterDefinition> defaultParamDefMap;
	/**
	 * Map of regular expressions for each data type. Will be used setting each Commands' happyRegex.
	 */
	public static final transient HashMap<String, String> happyRegularExpressions;
	// Initializing default HashMaps
	static
	{
		defaultParamDefMap = new HashMap<String, ParameterDefinition>();
		defaultParamDefMap.put("byte",new ParameterDefinition("byte", "byte", "", "%d", "Default byte parameter"));
		defaultParamDefMap.put("int",new ParameterDefinition("int", "int", "", "%d", "Default int parameter"));
		defaultParamDefMap.put("long",new ParameterDefinition("long", "long", "", "%d", "Default long parameter"));
		defaultParamDefMap.put("double",new ParameterDefinition("double", "double", "", "%f","Default double parameter"));
		defaultParamDefMap.put("float",new ParameterDefinition("float", "float", "", "%f","Default float parameter"));
		defaultParamDefMap.put("char", new ParameterDefinition("char", "char", "",  "%1s", "Default char parameter"));
		defaultParamDefMap.put("boolean", new ParameterDefinition("boolean","boolean","","%s","Default boolean parameter"));
		defaultParamDefMap.put("string", new ParameterDefinition("string", "string", "", "%s", "Default string parameter"));
		
		happyRegularExpressions = new HashMap<String, String>();
		happyRegularExpressions.put("byte","([\\+\\-]?\\d+)");
		happyRegularExpressions.put("int", "([\\+\\-]?\\d+)" );
		happyRegularExpressions.put("long","([\\+\\-]?\\d+)");
		happyRegularExpressions.put("double", "([\\-\\+]?[0-9]+\\.[0-9]+[eE][\\-\\+]?[0-9]+|[\\-\\+]?[0-9]+\\.[0-9]+)");
		happyRegularExpressions.put("float", "([\\-\\+]?[0-9]+\\.[0-9]+[eE][\\-\\+]?[0-9]+|[\\-\\+]?[0-9]+\\.[0-9]+)");
		happyRegularExpressions.put("char", "(\\S{1})");
		happyRegularExpressions.put("boolean","([tT]rue|[fF]alse)");
		happyRegularExpressions.put("token","(\\S+)");
		happyRegularExpressions.put("string", "(.+)");
	}
	
	/**
	 * Alias for the same-named method with two parameters. Log level is "ALL" by default.
	 * @param path Path to the JSON configuration file of this commander
	 * @return Commander Object created from given JSON configuration.
	 * @throws IOException
	 * @throws InvalidConfigurationException
	 */
	public static Commander getCommanderObject(String path) throws IOException, InvalidConfigurationException{
		return getCommanderObject(path, "all");
	}
	/**
	 * Factory method of Commander. returns Commander created using JSON configuration at given path and with given log level.
	 * @param Path to the JSON configuration file of this commander
	 * @param Default log level of this Commander
	 * @return Commander Object created from given JSON configuration.
	 * @throws IOException
	 * @throws InvalidConfigurationException
	 */
	public static Commander getCommanderObject(String path, String loglevel) throws IOException, InvalidConfigurationException{
		String jsonFilePath = path; 
		Gson gson = new Gson();
		FileReader fr = new FileReader(jsonFilePath);
		Commander cmdr = gson.fromJson(fr, Commander.class);
		cmdr.setLogger(cmdr.getName(),loglevel);
		cmdr.validate();		
		cmdr.createCommandInterface();
		return cmdr;
	}
	/**
	 * Connect to the device using CommandInterface
	 * @throws IOException
	 */
	public void connect() throws IOException {
		//. TODO set connected to false on certain errors (e.g. timeouts)
		logger.debug("Connecting to {}", name);
		if (!connected) {
			interfaceConnection.connect();
			connected = true;
		}
	}
	/**
	 * Constructs CommandInterface using connectionProperties from JSON
	 * @throws IOException
	 */
	private void createCommandInterface() throws IOException {  //. TODO private?
		//. a separate configuration file for the interface might not be necessary
		//. but the interface needs to be set up with param/command maps, etc.
		String connectiontype = connectionProperties.get("type");
		logger.debug("Creating {} type command interface for {}", connectiontype, name);
		if (connectiontype.equalsIgnoreCase("Ice")) {
			ICECommandInterface iceCI = new ICECommandInterface(connectionProperties);
			iceCI.constructFromConfigurationFile("");  //. TODO
			setCommandInterface(iceCI);
		} else if (connectiontype.equalsIgnoreCase("tcp")) {
			//. Socket
			//. TODO if socket
//			String socketConfigJsonFilePath = ""; //. TODO get string from new cmdr
			TCPCommandInterface tcpCI = new TCPCommandInterface(connectionProperties);
//			socketCI.constructFromConfigurationFile(socketConfigJsonFilePath);
			setCommandInterface(tcpCI);
		} else if (connectiontype.equalsIgnoreCase("udp")) {
			UDPCommandInterface udpCI = new UDPCommandInterface(connectionProperties);
			setCommandInterface(udpCI);
		} else if (connectiontype.equalsIgnoreCase("Serial")) {
			SerialCommandInterface serialCI = new SerialCommandInterface(connectionProperties);
			setCommandInterface(serialCI);
		}
	}
	/**
	 * Internal method to convert Java Formatter format for Date/Calendar to regular expression  
	 * @param Java formatter style String for Date/Calendar
	 * @return Regular expression style String for Date/Calendar
	 */
	private String dateformat2regex(String format) {
		String dateformat = format;
		dateformat = dateformat.replaceAll("%tY","\\\\d{4}");	// 4-digit year
		dateformat = dateformat.replaceAll("%ty","\\\\d{2}");		// 2-digit year
		dateformat = dateformat.replaceAll("%tB", "\\\\S+");	// Month in Full String
		dateformat = dateformat.replaceAll("%tb","\\\\S+");	// Month in abbreviated String
		dateformat = dateformat.replaceAll("%tm","\\\\d{2}");		// 2-digit Month
		dateformat = dateformat.replaceAll("%td","\\\\d{2}");		// 2-digit day with leading zero
		dateformat = dateformat.replaceAll("%te","\\\\d{1,2}");		// 1 or 2-digit day
		dateformat = dateformat.replaceAll("%tH","\\\\d{2}");		// 2-digit hour with leading zero 00-23
		dateformat = dateformat.replaceAll("%tk","\\\\d{1,2}");		// 1 or 2-digit hour 0-23
		dateformat = dateformat.replaceAll("%tI","\\\\d{2}");		// 2-digit hour with leading zero 01-12
		dateformat = dateformat.replaceAll("%tl","\\\\d{1,2}");		// 1 or 2 digit hour 1-12
		dateformat = dateformat.replaceAll("%tM","\\\\d{2}");		// 2-digit minutes
		dateformat = dateformat.replaceAll("%tS","\\\\d{2}");		// 2-digit seconds
		dateformat = dateformat.replaceAll("%tL","\\\\d{3}");	// 3-digit milliseconds
		dateformat = dateformat.replaceAll("%tZ","\\\\S+");		// TimeZoneName
		dateformat = dateformat.replaceAll("%tz","\\\\S+");		// TimeZone Offset
		dateformat = dateformat.replaceAll("%tp", "\\\\S+");     // am/pm indicator
		System.out.println("dateformat" + dateformat);
		return "(" + dateformat + ")";
	}
	/**
	 * Disconnect from the device
	 * @throws IOException
	 */
	public void disconnect() throws IOException {
		if (connected) {
			logger.debug("Disconnecting from {}", name);
			interfaceConnection.disconnect();
			connected = false;
		}
	}
	/**
	 * Parse the response(assumed to be in String) from the hardware, and convert them into Java Objects.
	 * In case of response with multiple values, It will return ArrayList of Java Objects
	 * In case of response with multi-line response, It will return ArrayList of ArrayList of Java Objects.
	 * @param Command to be used as reference while parsing output
	 * @param Raw String response from the hardware
	 * @return Parsed response
	 * @throws InvalidParameterException
	 * @throws InvalidOutputException
	 * @throws InvalidCommandException
	 * @throws InvalidConfigurationException
	 */
	private Object formatOutputObject(Command cmd, String response) throws InvalidParameterException, InvalidOutputException, InvalidCommandException, InvalidConfigurationException{
		logger.debug("Parsing output");
		ArrayList<Object> outArray = new ArrayList<Object>();
		ArrayList<ArrayList<Object>> multiArray = new ArrayList<ArrayList<Object>>();
		
		ArrayList<String> rList = cmd.getResponseList();
		
		String happyformat = getHappyRegex(cmd);
		String sadformat = getSadRegex(cmd);
//		System.out.println("happy : " + happyformat);
//		System.out.println("sad : " + sadformat);
		Pattern pat;
		Matcher mat;
		int numItem;
		int caseNum;
		// different cases 0:both format configured	1:happyformat only	2:sadformat only
		if (happyformat!=null && sadformat!=null) {
			caseNum = 0;
		} else if (happyformat!=null) {
			caseNum = 1;
		} else if (sadformat!=null) {
			caseNum = 2;
		} else {
			throw new InvalidOutputException(String.format("Command \"%s\" does not have any response configured", cmd.getName()));
		}

		if (caseNum == 0 || caseNum ==1) {
			pat = Pattern.compile(happyformat,Pattern.DOTALL);
			mat = pat.matcher(response);
			numItem = mat.groupCount();
			while (mat.find()) {
//				System.out.println(mat.group(0));
				outArray = new ArrayList<Object>();
				for (int ii = 0; ii<numItem; ii++) {
					String s = mat.group(ii+1);
//					System.out.println(s);
					// convert the string into object, and add to Array
					if (caseNum == 0 || caseNum ==1) {
						ParameterDefinition pDef = getParamDef(rList.get(ii));
						Object out = pDef.getParameterFromString(s).getValue();
						outArray.add(out);
					} else {
						outArray.add(s);
					}
				}
				multiArray.add(outArray);
			}
			if (multiArray.size() < 1 && sadformat != null){
				pat = Pattern.compile(sadformat,Pattern.DOTALL);
				mat = pat.matcher(response);
				numItem = mat.groupCount();
				while (mat.find()) {
//					System.out.println(mat.group(0));
					outArray = new ArrayList<Object>();
					outArray.add(errorNotifier);
					// going through the match
					for (int ii = 0; ii<numItem; ii++) {
						String s = mat.group(ii+1);
						// convert the string into object, and add to Array
						if (caseNum == 0 || caseNum ==1) {
							ParameterDefinition pDef = getParamDef(rList.get(ii));
							Object out = pDef.getParameterFromString(s).getValue();
							outArray.add(out);
						} else {
							outArray.add(s);
						}
					}
					multiArray.add(outArray);
				}
			}
			
		} else {
			pat = Pattern.compile(sadformat,Pattern.DOTALL);
			mat = pat.matcher(response);
			numItem = mat.groupCount();
			while (mat.find()) {
//				System.out.println(mat.group(0));
				outArray = new ArrayList<Object>();
				outArray.add(errorNotifier);
				// going through the match
				for (int ii = 0; ii<numItem; ii++) {
					String s = mat.group(ii+1);
					// convert the string into object, and add to Array
					if (caseNum == 0 || caseNum ==1) {
						ParameterDefinition pDef = getParamDef(rList.get(ii));
						Object out = pDef.getParameterFromString(s).getValue();
						outArray.add(out);
					} else {
						outArray.add(s);
					}
				}
				multiArray.add(outArray);
			}
		}
		if (multiArray.size()==0) {
			throw new InvalidOutputException("Was not able to parse output for " + cmd.getName() + ".");
		}if (multiArray.size()==1) {
			ArrayList<Object> onlyArray = multiArray.get(0);
			if (onlyArray.size()==1) {
				return onlyArray.get(0);
			} else {
				return onlyArray;
			}
		} else {
			return multiArray;
		}
	}
	/**
	 * Returns CommandInterface of the Commander
	 * @return CommandInterface of the Commander
	 */
	public CommandInterface getCommandInterface() {
		return interfaceConnection;
	}
	/**
	 * Returns HashMap of Commands with their name as the key
	 * @return HashMap of Commands
	 */
	public HashMap<String,Command> getCommandMap() {
		return commandMap;
	}		
	/**
	 * Returns the Command object using the key
	 * @param name(key) of the Command
	 * @return Command with the name passed in
	 */
	public Command getCommandObject(String key) throws InvalidCommandException {
		Command cmd = commandMap.get(key);
		if (cmd == null) {
			throw new InvalidCommandException("Command \"" + key + "\" was not found in configuration.");		
		}
		return cmd;
	}
	/**
	 * Acquire successful regular expression for the Command passed in as Parameter
	 * For the first call, Command itself wouldn't have happyRegex ready. 
	 * In this case, construct happyRegex using responseFormat of the same Command.
	 * Reason for this is that Command does not know of the paramDefMap.
	 * @param Command which the happyRegex is requested of
	 * @return Regular expression of successful response.
	 * @throws InvalidCommandException
	 * @throws InvalidParameterException
	 */
	private String getHappyRegex(Command cmd) throws InvalidCommandException, InvalidParameterException{
		if(cmd.getHappyRegex()== null || cmd.getHappyRegex().equals("")) {
			String happy = cmd.getResponseFormat();
			ArrayList<String> rlist = cmd.getResponseList();
			for(int ii = 0; ii<rlist.size(); ii++){
				ParameterDefinition p;
				try {
					p = getParamDef(rlist.get(ii));
				} catch (InvalidParameterException e) {
					throw new InvalidParameterException("For " + cmd.getName() +": "+ e.getMessage());
				}
				String regex;
				if (p.getType().equalsIgnoreCase("date")) {
					regex = dateformat2regex(p.getParamFormat());
				} else {
					regex = happyRegularExpressions.get(p.getType());
				}
				happy = happy.replace("("+rlist.get(ii)+")", regex);
			}
			cmd.setHappyRegex(happy);
		}
		return cmd.getHappyRegex();
	}
	/** 
	 * Returns the name of the Hardware
	 * @return
	 */
	public String getName() {return name;}
	/**
	 * returns the ParameterDefinition object using the key
	 * @param Name(key) of the ParameterDefinition
	 * @return ParameterDefinition
	 * @throws InvalidParameterException 
	 */
	public ParameterDefinition getParamDef(String key) throws InvalidParameterException{
		if (paramDefMap.containsKey(key)) {
			return paramDefMap.get(key);
		} else if (defaultParamDefMap.containsKey(key)) {
			return defaultParamDefMap.get(key);
		} else {
			throw new InvalidParameterException(String.format("Parameter with name of %s was not found from maps.",key));
		}
	}
	/**
	 * Returns HashMap of ParameterDefinitions with their name as the key
	 * @return HashMap of ParameterDefinitions
	 */
	public HashMap<String,ParameterDefinition> getParamDefMap() {
		return paramDefMap;
	}
	/**
	 * Acquire failure regular expression for the Command passed in as Parameter
	 * In a case that command does not have command-specific sadRegex, set it to be same as the deviceSadRegex
	 * @param Command which the sadRegex is requested of
	 * @return Regular expression of failure response.
	 * @throws InvalidCommandException
	 * @throws InvalidParameterException
	 */
	private String getSadRegex(Command cmd) throws InvalidCommandException{
		if (cmd.getSadRegex() == null || cmd.getSadRegex().equals("")) {
			cmd.setSadRegex(deviceSadRegex);
		} 
		return cmd.getSadRegex();
	}
	/**
	 * In case some changes are made for the Commander, (i.e. change in CommanderInterface) save the new Commander as JSON on given path.
	 * @param Path for the JSON configuration file to be saved
	 * @throws IOException
	 */
	public void saveConfig(String path) throws IOException{
		Gson gson = new GsonBuilder().setPrettyPrinting().excludeFieldsWithModifiers(Modifier.TRANSIENT).create();
		FileWriter writer = new FileWriter(path);
		String json = gson.toJson(this);
		writer.write(json);
		writer.close();
	}
	/**
	 * Sets the CommandInterface with the one passed in as parameter.
	 * @param CommanderInterface
	 */
	private void setCommandInterface(CommandInterface ci) {
		interfaceConnection = ci;
	}
	
	/**
	 * Setting logger name and default log level for the Commander
	 * @param Name of the logger to be displayed
	 * @param Default log level for this Commander
	 */
	public void setLogger(String loggerName, String loglevel) {
		logger= LogManager.getLogger(loggerName);
		setLogLevel(loglevel);
		logger.debug("Logger has been prepared");
	}
	/**
	 * Make it available to set the log level even after the construction.
	 * @param Log level
	 */
	public void setLogLevel(String l) {
		LoggerContext context = (LoggerContext) LogManager.getContext(false);
		Configuration config = context.getConfiguration();
		LoggerConfig loggerConfig = config.getLoggerConfig("root");
		if (l.equalsIgnoreCase("all")) {	loggerConfig.setLevel(Level.ALL);	}
		else if (l.equalsIgnoreCase("trace")) {	loggerConfig.setLevel(Level.TRACE);}
		else if (l.equalsIgnoreCase("debug")) {	loggerConfig.setLevel(Level.DEBUG);}
		else if (l.equalsIgnoreCase("info")) {	loggerConfig.setLevel(Level.INFO);}
		else if (l.equalsIgnoreCase("warn")) {	loggerConfig.setLevel(Level.WARN);}
		else if (l.equalsIgnoreCase("error")) {	loggerConfig.setLevel(Level.ERROR);}
		else if (l.equalsIgnoreCase("fatal")) {	loggerConfig.setLevel(Level.FATAL);}
		else if (l.equalsIgnoreCase("off")) {	loggerConfig.setLevel(Level.OFF);}
		context.updateLoggers();
	}
	/**
	 * Submit method for Command that does not expect any response.
	 * Pass in empty Parameter Array as argument.
	 * @param Command name
	 * @return Since there will be no response, it would return null.
	 * @throws InvalidParameterException
	 * @throws InvalidCommandException
	 * @throws IOException
	 * @throws InvalidOutputException
	 * @throws InvalidConfigurationException
	 */
	public Object submit(String command) throws InvalidParameterException, InvalidCommandException, IOException, InvalidOutputException, InvalidConfigurationException {
		return submit(command, new Parameter[]{});
	}
	
	/**
	 * Wrapper method of the core submit method.
	 * Takes arguments as array of Object(Integer, Double, etc.), convert the strings as Parameters using parameter list of the Command.
	 * @param Name of the command that will be submitted
	 * @param Array of Object arguments that is required for Command
	 * @return Object, ArrayList of Object, or ArrayList of ArrayList of Object(for multiple line response)
	 * @throws InvalidParameterException
	 * @throws InvalidCommandException
	 * @throws IOException
	 * @throws InvalidOutputException
	 * @throws InvalidConfigurationException
	 */
	public Object submit(String commandKey, Object[] parameters) throws InvalidParameterException, InvalidCommandException, IOException, InvalidOutputException, InvalidConfigurationException {
		if (!connected) {
			throw new IOException("Not connected to " + name +".");
		}
			
		Command cmd = getCommandObject(commandKey);
		Parameter[] paramObjects = new Parameter[0];
		ArrayList<String> pkeylist = cmd.getParamKeyList();
		
		if (pkeylist.size()!=0) {
			paramObjects = new Parameter[pkeylist.size()]; 
						
			if (pkeylist.size() != parameters.length) {
				throw new InvalidParameterException("Number of parameters does not match. " + pkeylist.size() + " required, " + parameters.length + " entered: " + cmd.getParamKeyList());
			}
			
			for (int ii = 0; ii<pkeylist.size();ii++) {
				ParameterDefinition pdef = getParamDef(pkeylist.get(ii));
				if (pdef==null) {
					throw new InvalidParameterException("Parameter "+pkeylist.get(ii)+" is not found in configuration.");
				}
				Object o = parameters[ii];	
				paramObjects[ii] = pdef.getParameterFromObject(o);				
			}		
		}
		
		return submit(commandKey,paramObjects);
	}
	
	/**
	 * Core submit method. Takes name of Command and Array of Parameters and parse it as how device would take
	 * If the command is query type, it will read the response from the device and parse as 
	 * Object, ArrayList of Object, or ArrayList of ArrayList of Object(for multiple line response)
	 * Otherwise, It will just dump the command to the device.
	 * @param Name of the command that will be submitted
	 * @param Array of Parameter arguments that is required for Command
	 * @return Object, ArrayList of Object, or ArrayList of ArrayList of Object(for multiple line response)
	 * @throws InvalidCommandException
	 * @throws InvalidParameterException
	 * @throws IOException
	 * @throws InvalidOutputException
	 * @throws InvalidConfigurationException
	 */
	private Object submit(String commandKey, Parameter[] parameters) throws InvalidCommandException, InvalidParameterException, IOException, InvalidOutputException, InvalidConfigurationException{
		Object response;
		logger.debug("Submitting command {}", commandKey);
		Command cmd = getCommandObject(commandKey);
					
		if (cmd.isRequest()|| getSadRegex(cmd)!= null) {
			response = interfaceConnection.sendRequest(cmd.getCommand(), parameters);
			if (response instanceof String) {
				//logger.debug("Response to " + cmdString + "\n" + response.toString());
				//. TODO: use formatOutputObject regardless of return type.  parse into multiple objects if necessary
				return formatOutputObject(cmd, response.toString());
			} else {
				ArrayList<Object> ret = new ArrayList<Object>();
				ret.add(response);
				return ret;
			}
		} else {
			interfaceConnection.sendCommand(cmd.getCommand(), parameters);
			return null;
		}
	}
	/**
	 * Wrapper method of the String arguments submit method.
	 * Takes arguments as String, and splits the string using comma as delimiter..
	 * @param Name of the command that will be submitted
	 * @param String representation of arguments separated by commas
	 * @return Object, ArrayList of Object, or ArrayList of ArrayList of Object(for multiple line response)
	 * @throws InvalidParameterException
	 * @throws InvalidCommandException
	 * @throws IOException
	 * @throws InvalidOutputException
	 * @throws InvalidConfigurationException
	 */
	public Object submit(String command, String parameters) throws InvalidParameterException, InvalidCommandException, IOException, InvalidOutputException, InvalidConfigurationException {
		if (parameters.length()==0) {
			return submit(command,new String[]{});
		} else {
			String[] array = parameters.split(",\\s*");
			return submit(command,array);	
		}
		
	}
	/**
	 * Wrapper method of the core submit method.
	 * Takes arguments as array of String, convert the strings as Parameters using parameter list of the Command.
	 * @param Name of the command that will be submitted
	 * @param Array of String arguments that is required for Command
	 * @return Object, ArrayList of Object, or ArrayList of ArrayList of Object(for multiple line response)
	 * @throws InvalidParameterException
	 * @throws InvalidCommandException
	 * @throws IOException
	 * @throws InvalidOutputException
	 * @throws InvalidConfigurationException
	 */
	public Object submit(String commandKey, String[] parameters) throws InvalidParameterException, InvalidCommandException, IOException, InvalidOutputException, InvalidConfigurationException {
		
		if (!connected) {
			throw new IOException("Not connected to " + name +".");
		}
			
		Command cmd = getCommandObject(commandKey);
		Parameter[] paramObjects = new Parameter[0];
		ArrayList<String> pkeylist = cmd.getParamKeyList();
		
		if (pkeylist.size() != 0) {
			paramObjects = new Parameter[pkeylist.size()]; 
						
			if (pkeylist.size() != parameters.length) {
				throw new InvalidParameterException("Number of parameters does not match. " + pkeylist.size() + " required, " + parameters.length + " entered: " + cmd.getParamKeyList());
			}
			
			for (int ii = 0; ii<pkeylist.size();ii++) {
				ParameterDefinition pdef = getParamDef(pkeylist.get(ii));
				if (pdef==null) {
					throw new InvalidParameterException("Parameter "+pkeylist.get(ii)+" is not found in configuration.");
				}
				String s = parameters[ii];	
				paramObjects[ii] = pdef.getParameterFromString(s);				
			}		
		}
		
		return submit(commandKey,paramObjects);
	}
	/**
	 * Validates the configuration files by validating each HashMaps(connectionProperties, commandMap, paramDefMap).
	 * @throws InvalidConfigurationException
	 */
	private void validate() throws InvalidConfigurationException{
		//Commander field validate
		logger.debug("Validating JSON config");
		String connection = connectionProperties.get("type");
		if (!connection.equalsIgnoreCase("ice")&&!connection.equalsIgnoreCase("tcp")&&!connection.equalsIgnoreCase("udp")&&!connection.equalsIgnoreCase("serial")) {
			throw new InvalidConfigurationException("Invalid connection type : " + connection);
		}
		
		//CommandMap validate
		logger.debug("Validating commands definitions");
		for (Entry<String, Command> entry : commandMap.entrySet()) {
			entry.getValue().validate(paramDefMap);
		}
		//ParamDefMap validate
		logger.debug("Validating parameter definitions");
		for (Entry<String, ParameterDefinition> entry : paramDefMap.entrySet()) {
			entry.getValue().validate();
		}
	}
}
 