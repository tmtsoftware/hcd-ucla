package hcd;


public class InvalidOutputException extends Exception{
	public InvalidOutputException(){
		super("Invalid Output");
	}
	public InvalidOutputException(String message){
		super(message);
	}
}
