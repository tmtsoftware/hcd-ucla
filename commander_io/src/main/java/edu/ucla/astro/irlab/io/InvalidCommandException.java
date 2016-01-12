package edu.ucla.astro.irlab.io;


public class InvalidCommandException extends Exception{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public InvalidCommandException() {
		super("Invalid Commander");
	}
	public InvalidCommandException(String message) {
		super(message);
	}
}