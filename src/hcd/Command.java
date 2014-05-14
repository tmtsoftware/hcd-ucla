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
	private String happyRegex;
	private String sadRegex;			// For the case this command have different type of response for error/status
	private ArrayList<String> paramList;
	private ArrayList<String> responseList;
	
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
	public void setHappyRegex(String happy){
		happyRegex = happy;
	}
	public String getHappyRegex(){
		return happyRegex;
	}
	public void setSadRegex(String sad){
		sadRegex = sad;
	}
	public String getSadRegex(){
		return sadRegex;
	}
	public boolean isRequest(){
		if(responseFormat != null) { return true; }
		else { return false; }
	}
	
	private void makeParamList(){
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

	private void makeResponseList(){
		Pattern pattern = Pattern.compile("(\\(\\w*?\\))");
		Matcher matcher = pattern.matcher(responseFormat);
		
		responseList = new ArrayList<String>();
		
		while(matcher.find()){
			String found = matcher.group();
			responseList.add(found.replace("(","").replace(")",""));
		}
	}
	public ArrayList<String> getResponseList(){
		if(!isRequest()){
			return null;
		}else if(responseList == null){
			makeResponseList();
		}
		return responseList;
	}
}
