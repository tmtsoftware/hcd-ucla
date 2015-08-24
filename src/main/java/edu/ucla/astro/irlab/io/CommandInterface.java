package edu.ucla.astro.irlab.io;


import java.io.IOException;

public interface CommandInterface {
	public void constructFromConfigurationFile(String path) throws IOException;
	public void connect() throws  IOException;
	public void disconnect() throws IOException;
	public void sendCommand(String command, Parameter[] arguments) throws InvalidCommandException, InvalidParameterException, IOException;
	public Object sendRequest(String command, Parameter[] arguments) throws InvalidCommandException, InvalidParameterException, IOException;
}
