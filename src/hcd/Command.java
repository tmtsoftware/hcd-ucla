package hcd;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Command {

	private String name;
	private String command;						// command line that will be passed on to device
	private String description;					// description of this command
	private ArrayList<String> paramList;		// list of parameter keys needed for this command
	private int responseLength;					// if known, it can read only that many chars
	private ArrayList<String> responseList;		// list of parameter keys in order of appearance 
	private String responseFormat;				// (auto-generated) how the response will be matched to grab data.
	private String errorStatusRegex;			// For the case this command have different type of response for error/status
	
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
	/**
	 * Return the list or Parameter's key
	 * @return
	 */
	public ArrayList<String> getParamList(){
		return paramList;
	}
	/**
	 * Return the number of Parameters
	 * @return
	 */
	public int getNumParameters(){
		if(paramList==null){
			return 0;
		}else{
			return paramList.size();
		}
	}
	/**
	 * Return parameter's name at given index
	 * @param i
	 * @return
	 */
	public String getParameterName(int i){
		return paramList.get(i);
	}
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
	public int getNumResponse(){
		return responseList.size();
	}
	/**
	 * Return list of keys of response.
	 * @return
	 */
	public ArrayList<String> getResponseList(){
		return responseList;
	}
	/**
	 * Return the status of the command. true if it is a request, false otherwise.
	 * @return
	 */
	public boolean isRequest(){
		if(responseList == null){
			return false;
		} else {
			return true;
		}
	}
	public String getErrorStatusRegex(){
		return errorStatusRegex;
	}
	/**
	 * Construct a regex of expected response according to the list of values expected.
	 * @param paramMap
	 * @return
	 */
	public String constructResponseFormat(HashMap<String, Parameter> paramMap) {
		String rformat = ".*?";
		if(responseList != null){
			for(int ii = 0; ii < responseList.size(); ii++){
				Parameter p = paramMap.get(responseList.get(ii));
				rformat += p.getDataFormat() + ".*?";
			}
			return rformat;
		}
		else{
			return null;
		}
	}

	
}
