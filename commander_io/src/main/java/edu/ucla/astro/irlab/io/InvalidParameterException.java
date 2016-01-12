package edu.ucla.astro.irlab.io;


public class InvalidParameterException extends Exception{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public InvalidParameterException() {
		super("Invalid Parameter");
	}
	public InvalidParameterException(String message) {
		super(message);
	}
}