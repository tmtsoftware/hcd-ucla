package edu.ucla.astro.irlab.io;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Command {

	private String name;						// name of the command
	private String command;						// command string that will be passed on to device with parameters placeholders
	private String responseFormat;
	private String description;					// description of this command

	private transient String sadRegex;	
	private transient String happyRegex;
	private transient ArrayList<String> paramList;
	private transient ArrayList<String> responseList;
	
	/**
	 * Return this command's name
	 * @return 
	 */
	public String getName() {
		return name;
	}
	/**
	 * Return command for hardware
	 * @return
	 */
	public String getCommand() {
		return command;
	}
	/**
	 * Return description of the command 
	 * @return
	 */
	public String getDescription() {return description;}	

	public String getResponseFormat() {return responseFormat;}
	
	/**
	 * 
	 * @param happy
	 */
	public void setHappyRegex(String happy) {
		happyRegex = happy;
	}
	public String getHappyRegex() {
		return happyRegex;
	}
	public void setSadRegex(String sad) {
		sadRegex = sad;
	}
	public String getSadRegex() {
		return sadRegex;
	}
	public boolean isRequest() {
		if (responseFormat != null) { return true; }
		else { return false; }
	}
	
	private void makeParamKeyList() {
		Pattern pattern = Pattern.compile("(\\(\\S*?\\))");
		Matcher matcher = pattern.matcher(command);
		
		paramList = new ArrayList<String>();
		
		while(matcher.find()) {
			String found = matcher.group();
			paramList.add(found.replace("(","").replace(")",""));
		}
	}
	public ArrayList<String> getParamKeyList() {
		if (paramList == null) {
			makeParamKeyList();
		}
		return paramList;
	}

	private void makeResponseList() {
		if (responseFormat != null) {
			Pattern pattern = Pattern.compile("(\\(\\S*?\\))");
			Matcher matcher = pattern.matcher(responseFormat);
			
			responseList = new ArrayList<String>();
			
			while(matcher.find()) {
				String found = matcher.group();
				responseList.add(found.replace("(","").replace(")",""));
			}
		} else {
			responseList = new ArrayList<String>();
		}
	}
	public ArrayList<String> getResponseList() {
		if (!isRequest()) {
			return new ArrayList<String>();
		} else if (responseList == null) {
			makeResponseList();
		}
		return responseList;
	}
	public void validate(HashMap<String,ParameterDefinition> pMap) throws InvalidConfigurationException{
		//TODO complete method
		makeParamKeyList();
		makeResponseList();
		
		for (String s : paramList) {
			if (!pMap.containsKey(s) && !Commander.defaultParamDefMap.containsKey(s)) {
				 throw new InvalidConfigurationException("Invalid Configuration : unknown parameter\"" + s +"\" required in command \"" + name + "\".");
			}
		}
		for (String s : responseList) {
			if (!pMap.containsKey(s) && !Commander.defaultParamDefMap.containsKey(s)) {
				 throw new InvalidConfigurationException("Invalid Configuration : unknown parameter\"" + s +"\" expected in command \"" + name + "\".");
			}
		}
		
	}
	
	public String toString(){
		String out = "Command: " + name;
		if (getDescription() != null){
			out += "\n" + getDescription();
		}
		if(getParamKeyList().size() > 0){
			out += "\nParameters : " + getParamKeyList().toString();
		}
		return out;
	}
}
