package hcd;


public class InvalidCommandException extends Exception{
	public InvalidCommandException(){
		super("Invalid Commander");
	}
	public InvalidCommandException(String message){
		super(message);
	}
}