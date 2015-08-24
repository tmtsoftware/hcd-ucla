package edu.ucla.astro.irlab.io;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import jline.console.*;
import jline.console.completer.AggregateCompleter;
import jline.console.completer.ArgumentCompleter;
import jline.console.completer.Completer;
import jline.console.completer.FileNameCompleter;
import jline.console.completer.StringsCompleter;

public class CommanderConsole {
	
	ConsoleReader reader;
	String path;
	Commander cmdr;
	ArrayList<String> commandlist;
	ArrayList<String> utillist;
	FileNameCompleter pathCompeleter;
	Completer commandCompeleter;
	Completer utilCompleter;
	
	public CommanderConsole(){
		try {
			reader = new ConsoleReader(System.in, System.out);
			reader.setHistoryEnabled(true);
		} catch (IOException e) {
			System.out.println(e.getMessage());
			System.exit(-1);
		}
		
		utillist = new ArrayList<String>(Arrays.asList("connect", "disconnect", "info", "setconfig","confirmconfig"));
		
		pathCompeleter = new FileNameCompleter();
		reader.addCompleter(pathCompeleter);
	}
	
	public void initialize(String[] args) {
		// token for successful initialization
		boolean success = false;
		
		// retry until successful or 
		while (success == false){
			try {
				// initialize cmdr object
				if (args.length == 0){
					System.out.println("Please enter the path to the JSON config file for the Commander.");
					path = read().trim();
				} else {
					path = args[0];
				}
				cmdr = Commander.getCommanderObject(path);
				
				// initialize commandlist and compeleter.
				String[] commandarray = cmdr.getCommandMap().keySet().toArray(new String[0]);
				Arrays.sort(commandarray);
				commandlist = new ArrayList<String>(Arrays.asList(commandarray));
				commandCompeleter = new StringsCompleter(commandlist);
				
				String[] paramarray = cmdr.getParamDefMap().keySet().toArray(new String[0]);
				Arrays.sort(paramarray);
				StringsCompleter paramCompleter = new StringsCompleter(new ArrayList<String>(Arrays.asList(paramarray)));
				
				// arrange compeleters
				reader.removeCompleter(pathCompeleter);
				// info takes Parameter names as arguemtn as well.
				StringsCompleter utilStringComp = new StringsCompleter("connect", "disconnect");
				ArgumentCompleter utilArgComp = new ArgumentCompleter(new StringsCompleter("info"), new AggregateCompleter(commandCompeleter, paramCompleter));
				ArgumentCompleter utilPathComp = new ArgumentCompleter(new StringsCompleter("setconfig", "confirmconfig"), pathCompeleter);
				
				
				reader.addCompleter(new AggregateCompleter(utilStringComp, utilArgComp, utilPathComp ,commandCompeleter));
				System.out.printf("Commander Console for %s, press tab for list of commands.\n", cmdr.getName());
				
				success = true;
			} catch (IOException e) {
				System.out.println(e.getMessage());
			} catch (InvalidConfigurationException e) {
				System.out.println(e.getMessage());
			}
		}
	}
	
	public void startConsole(){
		while (true){
			String input = read();
			String output;
			try {
				output = process(input);
				System.out.println("> " + output);
			} catch (IOException e) {
				System.out.println(e.getMessage());
			} catch (InvalidParameterException e) {
				System.out.println(e.getMessage());
			} catch (InvalidCommandException e) {
				System.out.println(e.getMessage());
			} catch (InvalidOutputException e) {
				System.out.println(e.getMessage());
			} catch (InvalidConfigurationException e) {
				System.out.println(e.getMessage());
			}
		}
	}
	
	private String read() {
		boolean success = false;
		String str = "";
		while (success == false){
			try {
				str = reader.readLine("< ");
				if (str.length() > 0){
					success = true;	
				}
			} catch (IOException e) {
				System.out.println(e.getMessage());
			}
		}
		if (str.equalsIgnoreCase("quit") || str.equalsIgnoreCase("q") || str.equalsIgnoreCase("exit")){
			quit();
		}
		return str;
	}
	
	private void quit() {
		if (cmdr!= null && cmdr.connected){
			try {
				cmdr.disconnect();
			} catch (IOException e) {
				System.out.println(e.getMessage());
			}
		}
		System.out.println("> Good Bye!");
		System.exit(0);
	}
		
	private String process(String input) throws InvalidCommandException, InvalidParameterException, IOException, InvalidOutputException, InvalidConfigurationException{
		String[] inputArray = input.split(" ");
		String key = inputArray[0];
		String[] params = Arrays.copyOfRange(inputArray, 1, inputArray.length);
		
		if (commandlist.contains(key)){
			return processCommand(key, params);
		} else if (utillist.contains(key)){
			return processUtil(key, params);
		} else {
			throw new InvalidCommandException(String.format("\"%s\" was not found from list of command/util.", key));
		}
	}
	
	private String processCommand(String command,  String[] params) throws InvalidParameterException, InvalidCommandException, IOException, InvalidOutputException, InvalidConfigurationException{
		Object out = cmdr.submit(command, params);
		if (out instanceof ArrayList<?>){
			ArrayList<Object> outList =  (ArrayList<Object>)out;
			// in case of ArrayList of ArrayList
			if (outList.get(0) instanceof ArrayList<?>){
				String outstring = "";
				for(Object outItem : outList){
					ArrayList<Object> itemList = (ArrayList<Object>)outItem;
					outstring += itemList.toString() + "\n";
				}
				return outstring;
			} 
			// in case of ArrayList
			else {
				return outList.toString();
			}
		} 
		// in case of command, which returns null.
		else if (out == null){
			return "DONE!";
		} 
		// in case of single object
		else {
			return out.toString();
		}
	}
	
	private String processUtil(String util,  String[] params) throws InvalidParameterException, InvalidCommandException, IOException, InvalidOutputException, InvalidConfigurationException{
		if (util.equalsIgnoreCase("connect")){
			cmdr.connect();
			return "Connected";
		} 
		
		else if (util.equalsIgnoreCase("disconnect")){
			cmdr.disconnect();
			return "Disconnected";
		}
		
		else if (util.toLowerCase().indexOf("info") == 0){
			if(params.length == 0){
				throw new InvalidParameterException("Missing name of command/parameter to query info.");
			}
			String item = params[0];
			
			if (cmdr.getCommandMap().containsKey(item)){
				return cmdr.getCommandObject(item).toString();
			} else if (cmdr.getParamDefMap().containsKey(item)){
				return cmdr.getParamDef(item).toString();
			} else {
				throw new InvalidParameterException(String.format("\"%s\" was not found from configuration", item));
			}
		}
		
		else if (util.toLowerCase().indexOf("setconfig") == 0){
			if(params.length==0){
				throw new InvalidParameterException("Requires path to JSON config for device parameters.");
			}
			return setConfig(params[0]);
		}
		
		else if (util.toLowerCase().indexOf("confirmconfig") == 0){
			if(params.length == 0){
				throw new InvalidParameterException("Requires path to JSON config for device parameters.");
			}
			return confirmConfig(params[0]);
		}
		
		else {
			return String.format("\"%s\" not processed", util);
		}
	}

	private String setConfig(String configjsonpath) throws InvalidParameterException, InvalidCommandException, IOException, InvalidOutputException, InvalidConfigurationException {
		Gson gson  = new Gson();
		FileReader fr;
		
		fr = new FileReader(configjsonpath);
				
		ArrayList<ArrayList<String>> list = gson.fromJson(fr, new TypeToken<ArrayList<ArrayList<String>>>() { }.getType());
		for (ArrayList<String> set : list){
//			System.out.println(set);
			cmdr.submit(set.get(0),set.get(1));
		}
			
		return "Config Setting Compeleted";
	}
	
	private String confirmConfig(String configjsonpath) throws InvalidParameterException, InvalidCommandException, IOException, InvalidOutputException, InvalidConfigurationException {
		Boolean pass = true;
		String failedList = "";
		Gson gson  = new Gson();
		FileReader fr;
		
		fr = new FileReader(configjsonpath);
				
		ArrayList<ArrayList<String>> list = gson.fromJson(fr, new TypeToken<ArrayList<ArrayList<String>>>() { }.getType());
		for (ArrayList<String> set : list){
			String out = cmdr.submit(set.get(0),set.get(1)).toString();
			if (!out.equalsIgnoreCase(set.get(2))) {
				pass = false;
				failedList += String.format("< %s should returned %s. %s expected.\n", set.get(0), out, set.get(2));
			}
		}
		
		if (pass == false){
			return "Config Confirmation failed in following areas.\n" + failedList; 
		} else {
			return "Config Confirmation Successful.";
		}
		
		
	}
	
	public static void main(String[] args) {
		CommanderConsole console = new CommanderConsole();
		console.initialize(args);
		console.startConsole();
	}
	
}
