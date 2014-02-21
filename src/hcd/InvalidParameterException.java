package hcd;


public class InvalidParameterException extends Exception{
	public InvalidParameterException(){
		super("Invalid Parameter");
	}
	public InvalidParameterException(String message){
		super(message);
	}
}