package edu.ucla.astro.irlab.io;


public class PositionalSyntaxCommandConstructor {
	public static String constructCommandString(String command, Parameter[] arguments) {
		//. command comes in in the form:
		//.  (param1) (param2) ... (paramM) command (paramM+1) (paramM+2) ... (paramM+N)
		//. and could even have some static text between params. 
		//. note all params are optional in the sense that they may or may not be present
		//. over all commands
		//. this function will replace each param with a member from arguments.
		//. it assumes that there is the correct number of matching parenthesis.
		if (arguments != null && arguments.length>0) {
			StringBuffer output = new StringBuffer();
			int numArgs;
			if ((numArgs = arguments.length) > 0) {
				int argNum = 0;
				int charNum = 0;
				//. go through each char in command
				while (charNum < command.length()) {
					
					//. locate ( and replace with parameter string
					char c = command.charAt(charNum);
					if (c == '(') {
						//. prepare the parameter string
						String s;
						if (arguments[argNum]==null) {
							s = "";
						} else {
							s = arguments[argNum].toString();
						}
						output.append(s);
						argNum++;
						charNum = command.indexOf(')', charNum);
					} else {
						output.append(c);
					}
					charNum++;
				}
			}
			return output.toString();
		} else {
			return command;
		}
	}
}
