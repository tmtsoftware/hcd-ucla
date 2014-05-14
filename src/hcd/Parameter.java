package hcd;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.text.ParsePosition;
import java.util.Date;

public class Parameter {
	
	private String name;
	private String type;			// type of parameter (int, double, char, string, date, )
	private String range;			// range of the acceptable value 
	private String dataFormat;		// format of the data to be read
	private String paramFormat;		// format of the data to be used as parameter
	private String description;		// description of the parameter
	public static String DATETIMEFORMAT_DEFAULT = "yyyy-MM-dd HH:mm:ss";
	public static String DEFAULT_PARAMFORMAT_STRING = "%s";
	public static String DEFUALT_PARAMFORMAT_DOUBLE = "%f";
	public static String DEFUALT_PARAMFORMAT_INT = "%d";
	public static String DEFAULT_PARAMFORMAT_CHAR = "%1s";
	
	/**
	 * Covert a Object value of this parameter into String value
	 * after it checks if the type of the object is valid for this parameter.
	 * @param obj
	 * @return
	 * @throws InvalidParameterException
	 */
	public String objectToString(Object obj) throws InvalidParameterException{
		// 1. ready the class type according to the type of the parameter
		Class<?> classType;
		String defaultParamformatString = DEFAULT_PARAMFORMAT_STRING;
		if(type.equalsIgnoreCase("string")) {
			classType = String.class;
		}else if(type.equalsIgnoreCase("int")) {
			classType = Integer.class;
			defaultParamformatString = DEFUALT_PARAMFORMAT_INT;
		}else if (type.equalsIgnoreCase("char")) {
			classType = Character.class;
			defaultParamformatString = DEFAULT_PARAMFORMAT_CHAR;
		}else if (type.equalsIgnoreCase("double")) {
			classType = Double.class;
			defaultParamformatString = DEFUALT_PARAMFORMAT_DOUBLE;
		}else if (type.equalsIgnoreCase("date")) {
			classType = Date.class;
			defaultParamformatString = DATETIMEFORMAT_DEFAULT;
		}else {
			classType = Object.class;
		}
		if(paramFormat == null){
			paramFormat = defaultParamformatString;
		}
		// 2. 	ready the string for return
		String out = "";
		
		// 3.	if the object is in correct type, parse it into String.
		//		if not, throw InvalidParameterException
		if(classType.isInstance(obj)){
		// 3.1 if it is type of "date", parse it according to the dateFormat
		//TODO dateformat response&param seperate?
			if (type.equals("date")){
				DateFormat df = paramFormat2SimpleDateFormat();
				out = df.format(obj);
		// 3.2 if it is other types, parse it according to paramFormat;
			} else {
				out = String.format(paramFormat, obj);
			}
		} else {
			String msg = name + " requires " + classType +": " + obj.getClass() + " entered.";
			throw new InvalidParameterException(msg);
		}
		
		// 4. 	check if the string is one of the accepted value. if not, thrown invalidParameterException.
		if(withinRange(out)) {
			return out;
		} else {
			throw new InvalidParameterException("Out of range: " + range + " required, "+ out + " entered");
		}
	}
	/**
	 * Check if the param is within the range of values configured by JSON.
	 * @param param
	 * @return
	 */
	public boolean withinRange(String param) {
		//1.if range is not null, check the value against given range. else, return true.
		if(range != null){
		//1.1 	if range is in interval notation, check accordingly(it is understood that when 
		//		interval notation is used, its type is either int or double)
			if (range.contains("(") || range.contains("[")) {
		// 1.1.1 create the value holders
				double lower = 0, upper = 0, value = 0;
		// 1.1.2. get the location of the comma
				int index = range.indexOf(",");
		// 1.1.3. trim any unnecessary white spaces, and set lower and upper values
				lower = Double.parseDouble(range.replace("\\s", "").substring(1,
						index));
				upper = Double.parseDouble(range.replace("\\s", "").substring(
						index + 1, range.length()-1));
		// 1.1.4 	set the value as double
				value = Double.parseDouble(param);
		// 1.1.5	both end exclusive
				if (range.contains("(") && range.contains(")")) {
					return ((value > lower) && (value < upper) && (lower < upper));
		// 1.1.6 	low ex, up in
				} else if (range.contains("(") && range.contains("]")) {
					return ((value >= lower) && (value < upper) && (lower < upper));
		// 1.1.7	both inclusive
				} else if (range.contains("[") && range.contains("]")) {
					return ((value >= lower) && (value <= upper) && (lower < upper));
		// 1.1.8 	low in, up ex
				} else if (range.contains("[") && range.contains(")")) {
					return ((value > lower) && (value <= upper) && (lower < upper));
				}
		// 1.2 if hyphen is used, both end is inclusive.(int, double, char)
			} else if (range.contains("-")) {
				double lower = 0, upper = 0, value = 0;
		// 1.2.1 for int and double, parse them to doubles
				if (type.equalsIgnoreCase("double") || type.equalsIgnoreCase("int")) {
					int index = range.indexOf("-");
					lower = Double.parseDouble(range.replace(" ", "").substring(0,
							index));
					upper = Double.parseDouble(range.replace(" ", "").substring(
							index + 1));
					value = Double.parseDouble(param);
		// 1.2.2 for char, get the char first and cast them to double
				} else if (type.equalsIgnoreCase("char")) {
					int index = range.indexOf("-");
					lower = (double) range.replace(" ", "").charAt(index - 1);
					upper = (double) range.replace(" ", "").charAt(index + 1);
					value = (double) param.charAt(0);
				}
				return ((value >= lower) && (value <= upper) && (lower < upper));
		// 1.3 if range is in form of list, check accordingly going through the list.
			}else if (range.contains(",")) {
		// 1.3.1 create the list of valid parameters
				String[] list = range.replace(" ", "").split(",");
		// 1.3.2 going through the list
				for (int i = 0; i < list.length; i++) {
		// if match is found, return true
					if (param.equals(list[i])) {
						return true;
					}
				}
			}
		// 1.3. if range is not null, but not in interval notation or list form, return false.
		// TODO config file validation. 
			return false;
		}
		// 2 if no range is in config, there is no limit. so return true.
		else{return true;}
	}
	
	public String getName(){
		return name;
	}
	public String getType(){
		return type;
	}
	public String getDataFormat(){
		return dataFormat;
	}


	
	// parse the input string into an object according to its configured type.
	public Object stringToObject(String s) throws InvalidParameterException {
		try {
			if(type.equals("int")){
				return Integer.parseInt(s);
			}else if (type.equals("double")){
				return Double.parseDouble(s);
			}else if (type.equals("char")) {
				if(s.length()==1){
					return s.charAt(0);
				}
				else{
					throw new NumberFormatException();
				}
			}else if (type.equals("date")){
				SimpleDateFormat dtf = paramFormat2SimpleDateFormat();
				return dtf.parse(s,new ParsePosition(0));
			}else{
				return s;
			}
		// possible exception in this try block is format exception.
		// catch it, and throw new invalid parameter exception with some more detail.
		} catch (Exception e) {
			throw new InvalidParameterException(String.format("For %s, %s was entered for type of %s.",name, s, type));
		}
	}
	public String getDescription(){
		return description;
	}

	public SimpleDateFormat paramFormat2SimpleDateFormat(){
		String dateformat = paramFormat;
		dateformat = dateformat.replaceAll("%tY","yyyy");
		dateformat = dateformat.replaceAll("%ty","yy");
		dateformat = dateformat.replaceAll("%tB", "MMMM");
		dateformat = dateformat.replaceAll("%tb","MMM");
		dateformat = dateformat.replaceAll("%tm","MM");
		dateformat = dateformat.replaceAll("%td","dd");
		dateformat = dateformat.replaceAll("%te","d");
		dateformat = dateformat.replaceAll("%tH","HH");
		dateformat = dateformat.replaceAll("%tk","H");
		dateformat = dateformat.replaceAll("%tI","hh");
		dateformat = dateformat.replaceAll("%tl","h");
		dateformat = dateformat.replaceAll("%tM","mm");
		dateformat = dateformat.replaceAll("%tS","ss");
		dateformat = dateformat.replaceAll("%tL","SSS");
		dateformat = dateformat.replaceAll("%tZ","Z");
		dateformat = dateformat.replaceAll("%tz","z");
		
		return new SimpleDateFormat(dateformat);
	}
}
