package hcd;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Command {

	private String name;
	private String command;						// command line that will be passed on to device
	private String description;					// description of this command
	private int responseLength;					// if known, it can read only that many chars 
	private String responseFormat;	
	private String errorStatusRegex;			// For the case this command have different type of response for error/status
	private ArrayList<String> paramList;
	
	/**
	 * Return this command's name
	 * @return
	 */
	public String getName(){
		return name;
	}
	/**
	 * Return command for hardware
	 * @return
	 */
	public String getCommand(){
		return command;
	}
	/**
	 * Return description of the command 
	 * @return
	 */
	public String getDescription(){return description;}	

	public String getResponseFormat(){return responseFormat;}
	
	/**
	 * Return number of characters expected in response
	 * @return
	 */
	public int getResponseLength(){
		return responseLength;
	}
	/**
	 * Return number of response parameters.
	 * @return
	 */
	public String getErrorStatusRegex(){
		return errorStatusRegex;
	}
	public boolean isRequest(){
		if(responseFormat != null) { return true; }
		else { return false; }
	}
	
	public void makeParamList(){
		Pattern pattern = Pattern.compile("(\\(\\w*?\\))");
		Matcher matcher = pattern.matcher(command);
		
		paramList = new ArrayList<String>();
		
		while(matcher.find()){
			String found = matcher.group();
			paramList.add(found.replace("(","").replace(")",""));
		}
	}
	public ArrayList<String> getParamList(){
		if(paramList == null){
			makeParamList();
		}
		return paramList;
	}
}
