package edu.ucla.astro.irlab.io;


public class InvalidConfigurationException extends Exception{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public InvalidConfigurationException() {
		super("Invalidity in Configuration");
	}
	public InvalidConfigurationException(String message) {
		super(message);
	}
}
