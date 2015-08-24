package edu.ucla.astro.irlab.io;


import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

public class ParameterDefinition {
	
	private final String name;
	private final String type;			// type of parameter (int, double, char, string, date, etc )
	private final String range;			// range of the acceptable value 
	private String paramFormat;		// format of the data to be used as parameter
	private String description;		// description of the parameter
	
	public static transient String DATETIMEFORMAT_DEFAULT = "%tY-%tm-%td %tH:%tM:%tS";
	public static transient String DEFAULT_PARAMFORMAT_STRING = "%s";
	public static transient String DEFUALT_PARAMFORMAT_DOUBLE = "%f";
	public static transient String DEFUALT_PARAMFORMAT_INT = "%d";
	public static transient String DEFAULT_PARAMFORMAT_CHAR = "%1s";
	
	public ParameterDefinition(String name, String type, String range, String paramFormat, String dexcription) {
		this.name = name;
		this.type = type;
		this.range = range;
		this.paramFormat = paramFormat;
		this.description = dexcription;
	}
	/**
	 * Convert object into string according to this parameter's definition
	 * @param obj
	 * @return
	 * @throws InvalidParameterException
	 */
	private String objectToString(Object obj) throws InvalidParameterException{
		// 1. ready the class type according to the type of the parameter
		Class<?> classType;
		if (type.equalsIgnoreCase("string")) {
			classType = String.class;
		} else if (type.equalsIgnoreCase("char")) {
			classType = Character.class;
		} else if (type.equalsIgnoreCase("int")) {
			classType = Integer.class;
		} else if (type.equalsIgnoreCase("byte")) {
			classType = Byte.class;
		} else if (type.equalsIgnoreCase("long")) {
			classType = Long.class;
		} else if (type.equalsIgnoreCase("double")) {
			classType = Double.class;
		} else if (type.equalsIgnoreCase("float")) {
			classType = Float.class;
		} else if (type.equalsIgnoreCase("boolean")) {
			classType = Boolean.class;
		} else if (type.equalsIgnoreCase("date")) {
			classType = Date.class;
		} else {
			classType = Object.class;
		}

		
		String out = "";
		if (classType.isInstance(obj)) {
		//TODO dateformat response&param seperate?
			if (type.equals("date")) {
				DateFormat df = paramFormat2SimpleDateFormat();
				out = df.format(obj);
				if (getParamFormat().indexOf("%tp")!=-1) {
					out = out.replaceAll("AM", "am").replaceAll("PM", "pm");
				}
			} else {
				if (paramFormat==null || paramFormat.equals("")) {
					out = obj.toString();
				} else {
					out = String.format(paramFormat.replace("%", "%1$"), obj);
				}
				
			}
		} else {
			String msg = name + " requires " + classType +": " + obj.getClass() + " entered.";
			throw new InvalidParameterException(msg);
		}
		return out;
	}
	/**
	 * Convert string into Object according to this parameter's definition
	 * @param s
	 * @return
	 * @throws InvalidParameterException
	 */
	private Object stringToObject(String s) throws InvalidParameterException, NumberFormatException{
		try{
			if (type.equalsIgnoreCase("string")) {
				if (paramFormat == null) {
					return s;
				} else {
					return String.format(paramFormat, s);
				}
			} else if (type.equalsIgnoreCase("char")) {
				if (s.length()==1) {
					return new Character(s.charAt(0));
				}
				else {
					throw new InvalidParameterException(String.format("Wrong type : \"%s\" entered for type of %s",s,type));
				}
			} else if (type.equalsIgnoreCase("int")) {
				return Integer.parseInt(s);
			} else if (type.equalsIgnoreCase("byte")) {
				return Byte.parseByte(s);
			} else if (type.equalsIgnoreCase("long")) {
				return Long.parseLong(s);
			} else if (type.equalsIgnoreCase("double")) {
				return Double.parseDouble(s);
			} else if (type.equalsIgnoreCase("float")) {
				return Float.parseFloat(s);
			} else if (type.equalsIgnoreCase("boolean")) {
				return Boolean.parseBoolean(s);
			} else if (type.equalsIgnoreCase("date")) {
				SimpleDateFormat dtf = paramFormat2SimpleDateFormat();
				Date d = dtf.parse(s,new ParsePosition(0));
				if (d == null) {
					throw new InvalidParameterException(String.format("Failed to parse \"%s\" into Date object. required input is in form \"%s\"", s, dtf.toPattern()));
				}
				return d;
			} else {
				return s;
			}
		//	 possible exception in this try block is format exception.
		//	 catch it, and throw new invalid parameter exception with some more detail.
		}catch(NumberFormatException e) {
			throw new InvalidParameterException(String.format("Wrong type : \"%s\" for type of %s",s,type));
		}
	}
	//TODO Union of range?
	private boolean withinRange(Object val) throws InvalidConfigurationException{
		// TODO InvalidConfigurationException should not be needed after validators are compelete
		if (range != null && range.length()>0) {
			if (range.contains("(") || range.contains("[")) {
				if (val instanceof Number) {
					double lower = 0, upper = 0, value = 0;

					int index = range.indexOf(",");
			
					lower = Double.parseDouble(range.substring(1,index));
					upper = Double.parseDouble(range.substring(index + 1, range.length()-1));
					value = Double.parseDouble(val.toString());
					
					if (range.contains("(") && range.contains(")")) {
						return ((value > lower) && (value < upper) && (lower < upper));
					} else if (range.contains("(") && range.contains("]")) {
						return ((value > lower) && (value <= upper) && (lower < upper));
					} else if (range.contains("[") && range.contains("]")) {
						return ((value >= lower) && (value <= upper) && (lower <= upper));
					} else if (range.contains("[") && range.contains(")")) {
						return ((value >= lower) && (value < upper) && (lower < upper));
					} else {
						throw new InvalidConfigurationException("Invalid interval notation: " + range);
					}

				} else {
					throw new InvalidConfigurationException("Interval range was found in config for non-numerical parameter");
				}
			}
			else if (range.contains(",")) {
				String[] array = range.replace(" ", "").split(",");
				ArrayList<String> arrylist = new ArrayList<String>(Arrays.asList(array));
				return arrylist.contains(val.toString());
			}
			else {
				throw new InvalidConfigurationException("Range is neither interval nor list form.");
			}
		} else {
			return true;
		}
		
		
	}
	
	public String getName() {
		return name;
	}
	public String getType() {
		return type;
	}
	public String getRange() {
		return range;
		}
	public String getParamFormat() {
		if (paramFormat != null) {
			return paramFormat;
		} else if (type.equalsIgnoreCase("int")) {
			return DEFUALT_PARAMFORMAT_INT;
		} else if (type.equalsIgnoreCase("char")) {
			return DEFAULT_PARAMFORMAT_CHAR;
		} else if (type.equalsIgnoreCase("double")) {
			return DEFUALT_PARAMFORMAT_DOUBLE;
		} else if (type.equalsIgnoreCase("date")) {
			return DATETIMEFORMAT_DEFAULT;
		} else {
			return DEFAULT_PARAMFORMAT_STRING;
		}
	}
	public String getDescription() {
		return description;
	}
	
	private SimpleDateFormat paramFormat2SimpleDateFormat() {
		String dateformat = getParamFormat();

		dateformat = dateformat.replaceAll("%tY","yyyy");	// 4-digit year
		dateformat = dateformat.replaceAll("%ty","yy");		// 2-digit year
		dateformat = dateformat.replaceAll("%tB", "MMMM");	// Month in Full String
		dateformat = dateformat.replaceAll("%tb","MMM");	// Month in abbreviated String
		dateformat = dateformat.replaceAll("%tm","MM");		// 2-digit Month
		dateformat = dateformat.replaceAll("%td","dd");		// 2-digit day with leading zero
		dateformat = dateformat.replaceAll("%te","d");		// 1 or 2-digit day
		dateformat = dateformat.replaceAll("%tH","HH");		// 2-digit hour with leading zero 00-23
		dateformat = dateformat.replaceAll("%tk","H");		// 1 or 2-digit hour 0-23
		dateformat = dateformat.replaceAll("%tI","hh");		// 2-digit hour with leading zero 01-12
		dateformat = dateformat.replaceAll("%tl","h");		// 1 or 2 digit hour 1-12
		dateformat = dateformat.replaceAll("%tM","mm");		// 2-digit minutes
		dateformat = dateformat.replaceAll("%tS","ss");		// 2-digit seconds
		dateformat = dateformat.replaceAll("%tL","SSS");	// 3-digit milliseconds
		dateformat = dateformat.replaceAll("%tZ","Z");		// TimeZoneName
		dateformat = dateformat.replaceAll("%tz","z");		// TimeZone Offset
		dateformat = dateformat.replaceAll("%tp", "a");     // am/pm indicator
		
		return new SimpleDateFormat(dateformat);
	}
	
	public Parameter getParameterFromString(String stringValue) throws InvalidParameterException, InvalidConfigurationException{
		if (stringValue==null || stringValue.length()==0) {
			throw new InvalidParameterException(String.format("Either null or empty string was entered for parameter %s", name));		
		}
		//. Convert to Object, checking if it's in correct type
		Object val = stringToObject(stringValue);
		String strVal;
		//. Checking if it's within range
		if (!withinRange(val)) {
			throw new InvalidParameterException(String.format("Out of range : \"%s\" entered for range of %s", val.toString(), range));
		}
		//. If in correct type and within range, make its string correspondence
		if (paramFormat!=null && paramFormat.length()>0) {
			strVal = String.format(paramFormat.replace("%", "%1$"), val);
		} else {
			strVal = val.toString();		//. TODO when there's no formatting, just use the stringValue that has been passed in?
		}
		//. Return Parameter object with values.
		return new Parameter(val, strVal);
	}
	public Parameter getParameterFromObject(Object objValue) throws InvalidParameterException, InvalidConfigurationException{
		if (objValue==null) {
			return null;
		}
		//. Convert to string, checking if it's in correct type
		String strVal = objectToString(objValue);		//TODO just check with classtype. 
		Object val = objValue;
		//. Checking if it's within range
		if (!withinRange(val)) {
			throw new InvalidParameterException(String.format("Out of range : \"%s\" entered for range of %s", val.toString(), range));
		}
		//. If in correct type and within range, make its string correspondence
		if (paramFormat!=null && paramFormat.length()>0) {
			strVal = String.format(paramFormat.replace("%", "%1$"), val);
		} else {
			strVal = val.toString();
		}
		//. Return Parameter object with values.
		return new Parameter(val, strVal);
	}
	public void validate() throws InvalidConfigurationException {
		//type
		if (!(type.equalsIgnoreCase("byte")
				|| type.equalsIgnoreCase("int")
				|| type.equalsIgnoreCase("long")
				|| type.equalsIgnoreCase("float")
				|| type.equalsIgnoreCase("double")
				|| type.equalsIgnoreCase("token")
				|| type.equalsIgnoreCase("string")
				|| type.equalsIgnoreCase("char")
				|| type.equalsIgnoreCase("boolean")
				|| type.equalsIgnoreCase("date"))) {
			throw new InvalidConfigurationException(String.format("Invalid type for parameter %s : %s", name, type));
		}
		//range
		if (range != null) {
			if ((range.contains("(") || range.contains("["))&&(range.contains(")") || range.contains("}"))) {	//if it is interval range
				int index1 = range.indexOf("(");															// get index of grouping symbol
				int index2 = range.indexOf(")");
				if (index1 == -1) {
					index1 = range.indexOf("[");
				}
				if (index2 == -1) {
					index2 = range.indexOf("]");
				}
		
				if (index1>index2) {													// if symbol indexes are in wrong order
					throw new InvalidConfigurationException(String.format("Invalid range for parameter %s : %s", name, range));
				}
				String content = range.substring(index1+1, index2);
				String[] vals = content.split(",");
				if (vals.length != 2) {												// if it isn't x,y format it's wrong.
					throw new InvalidConfigurationException(String.format("Invalid range for parameter %s : %s", name, range));
				}
				try{																// if it's not a number, throw exception
					double low = Double.parseDouble(vals[0]);
					double high = Double.parseDouble(vals[1]);
					if (low>high) {													// if lower limit and upper limit is switched
						throw new InvalidConfigurationException(String.format("Invalid range for parameter %s : %s", name, range));
					}
				}catch(NumberFormatException e) {
					throw new InvalidConfigurationException(String.format("Invalid range for parameter %s : %s", name, range));
				}
				
			} else if (!range.contains(",")) {										//So it's not an interval, and if it's not list using ","	
					throw new InvalidConfigurationException(String.format("Invalid range for parameter %s : %s", name, range));
			}
		}
		//paramFormat
	}
	
	public String toString(){
		String out = "Parameter: " + name;
		if (getDescription() != null){
			out += "\n" + getDescription();
		}
		if(getRange()!=null && getRange().length()>0){
			out += "\nRange : " + getRange();
		}
		return out;
	}
	
	
	
	
	
}
