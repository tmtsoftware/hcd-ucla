package hcd;

public class InvalidOutputException extends Exception{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public InvalidOutputException(){
		super("Invalid Output");
	}
	public InvalidOutputException(String message){
		super(message);
	}
}
