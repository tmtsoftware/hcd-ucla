package hcd;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Command {

	private String command;						// command line that will be passed on to device
	private String description;					// description of this command
	private ArrayList<String> paramList;		// list of parameter keys needed for this command
	private int responseLength;					// if known, it can read only that many chars
	private ArrayList<String> responseList;		// list of parameter keys in order of appearance 
	private String responseFormat;				// (auto-generated) how the response will be matched to grab data.
	
	
	
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
	
	
	/**
	 * Formats a response in String into ArrayList<Object>
	 * And each response will be an element of returning ArrayList<Object>.
	 * The "response" will be considered as single match to the responseFormat
	 * which is regex created by constructResponseFormat method
	 * @param response
	 * @param paramMap
	 * @return
	 * @throws InvalidParameterException
	 */
	public ArrayList<Object> formatOutputObject(String response,HashMap<String, Parameter> paramMap) throws InvalidParameterException {
		// 1. 	create the response format field according to paramList
		//		the pattern will have same number of groups as paramList
		responseFormat = constructResponseFormat(paramMap);
		// 2. 	create the pattern and matcher for this command
		Pattern pattern = Pattern.compile(responseFormat);
		Matcher matcher = pattern.matcher(response);
		
		ArrayList<Object> output = new ArrayList<Object>();		// ArrayList that will be output
		int numItem = matcher.groupCount();						// number of items will be in Array
		
		// 3. 	For ever match of the response format found,
		while (matcher.find())
		{
			if(matcher.group(1).length()>0){
				ArrayList<Object> outArray = new ArrayList<Object>();
		// 3.1 	going through the match
				for(int i = 0; i<numItem; i++){
		// 3.1.1	get the parameter object and get the corresponding string
					Parameter p = paramMap.get(responseList.get(i));
					String s = matcher.group(i+1);
		// 3.1.2	convert the string into object, and add to Array
					outArray.add(p.stringToObject(s));
				}
		// 3.2	 add the object to ArrayList
				output.add(outArray);
			}
			
		}
		return output;
	}
	/**
	 * Construct a regex of expected response according to the list of values expected.
	 * @param paramMap
	 * @return
	 */
	public String constructResponseFormat(HashMap<String, Parameter> paramMap) {
		String rformat = ".*?";
		for(int ii = 0; ii < responseList.size(); ii++){
			Parameter p = paramMap.get(responseList.get(ii));
			rformat += p.getDataFormat() + ".*?";
		}
		return rformat;
	}

	
}
