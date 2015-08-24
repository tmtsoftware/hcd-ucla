package edu.ucla.astro.irlab.io;


import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
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
public class Commander {
	// name of the hardware
	private String name;
	private String deviceSadRegex;
	private String errorNotifier;
	
	// Map of connection settings
	private HashMap<String, String> connectionProperties;
	// Maps of Command Object and Parameter Objects that will hold references.
	private HashMap<String, Command> commandMap;
	private HashMap<String, ParameterDefinition> paramDefMap;
	public static final transient HashMap<String, ParameterDefinition> defaultParamDefMap;
	public static final transient HashMap<String, String> happyRegularExpressions;
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
	
	
	// boolean monitoring socket connection status
	public transient boolean connected = false;
	// Log4J logger. (might change to custom logging) 
	public transient Logger logger;
	private transient CommandInterface interfaceConnection;
	
	// setting logger.
	public void setLogger(String loggerName) {
		logger= LogManager.getLogger(loggerName);
		logger.debug("Logger has been prepared");
	}
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
	public static Commander getCommanderObject(String path) throws IOException, InvalidConfigurationException{
		String jsonFilePath = path; 
		Gson gson = new Gson();
		FileReader fr = new FileReader(jsonFilePath);
		Commander cmdr = gson.fromJson(fr, Commander.class);
		cmdr.setLogger(cmdr.getName());
		cmdr.validate();		
		cmdr.createCommandInterface();
		return cmdr;
	}
	
	public CommandInterface getCommandInterface() {
		return interfaceConnection;
	}
	
	private void setCommandInterface(CommandInterface ci) {
		interfaceConnection = ci;
	}
	
	//. TODO set connected to false on certain errors (e.g. timeouts)
	public void connect() throws IOException {
		logger.debug("Connecting to {}", name);
		if (!connected) {
			interfaceConnection.connect();
			connected = true;
		}
	}
	
	public void disconnect() throws IOException {
		if (connected) {
			logger.debug("Disconnecting from {}", name);
			interfaceConnection.disconnect();
			connected = false;
		}
	}
	
	
	/**
	 * Returns the command object using the key
	 * @param key
	 * @return
	 */
	public Command getCommandObject(String key) throws InvalidCommandException {
		Command cmd = commandMap.get(key);
		if (cmd == null) {
			throw new InvalidCommandException("Command \"" + key + "\" was not found in configuration.");		
		}
		return cmd;
	}		
		
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
	public Object submit(String command, String parameters) throws InvalidParameterException, InvalidCommandException, IOException, InvalidOutputException, InvalidConfigurationException {
		if (parameters.length()==0) {
			return submit(command,new String[]{});
		} else {
			String[] array = parameters.split(",\\s*");
			return submit(command,array);	
		}
		
	}
	public Object submit(String command) throws InvalidParameterException, InvalidCommandException, IOException, InvalidOutputException, InvalidConfigurationException {
		return submit(command, new String[]{});
	}
	

	/** 
	 * Returns the name of the Hardware
	 * @return
	 */
	public String getName() {return name;}

	/**
	 * returns the parameter object using the key
	 * @param key
	 * @return
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
	
	private String getSadRegex(Command cmd) throws InvalidCommandException{
		if (cmd.getSadRegex() == null || cmd.getSadRegex().equals("")) {
			cmd.setSadRegex(deviceSadRegex);
		} 
		return cmd.getSadRegex();
	}
		
	// TODO unknown number of outputs?!
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
	
	public void saveConfig(String path) throws IOException{
		Gson gson = new GsonBuilder().setPrettyPrinting().excludeFieldsWithModifiers(Modifier.TRANSIENT).create();
		FileWriter writer = new FileWriter(path);
		String json = gson.toJson(this);
		writer.write(json);
		writer.close();
	}
	
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
	public HashMap<String,Command> getCommandMap() {
		return commandMap;
	}
	public HashMap<String,ParameterDefinition> getParamDefMap() {
		return paramDefMap;
	}

	
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
 