package edu.ucla.astro.irlab.io.serial;

import java.io.IOException;
import java.util.HashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.ucla.astro.irlab.io.CommandInterface;
import edu.ucla.astro.irlab.io.InvalidConfigurationException;
import edu.ucla.astro.irlab.io.Parameter;
import edu.ucla.astro.irlab.io.PositionalSyntaxCommandConstructor;
import jssc.SerialPort;
import jssc.SerialPortException;

public class SerialCommandInterface implements CommandInterface{
	
	SerialPort serial;
	Logger logger;
	String port;
	int baudrate, databit, stopbit, parity;
	
	public SerialCommandInterface(HashMap<String, String> settings) throws IOException{
		port = settings.get("port");
		baudrate = Integer.parseInt(settings.get("baudrate"));
		databit = Integer.parseInt(settings.get("databit"));
		stopbit = Integer.parseInt(settings.get("stopbit"));
		parity = Integer.parseInt(settings.get("parity"));
		
		try {
			serial = new SerialPort(port);
			if (!serial.setParams(baudrate, databit, stopbit, parity)) {
				throw new IOException("Unable to set the parameters for the serial port");
			}
		} catch (SerialPortException e) {
			throw new IOException("Unable to set the parameters for the serial port",e);
		}
	}
	
	public SerialCommandInterface(String p, int baud, int data, int stop, int pair) throws IOException{
		port = p;
		baudrate = baud;
		databit = data;
		stopbit = stop;
		parity = pair;
		
		try {
			serial = new SerialPort(port);
			if (!serial.setParams(baudrate, databit, stopbit, parity)) {
				throw new IOException("Unable to set the parameters for the serial port");
			}
		} catch (SerialPortException e) {
			throw new IOException("Unable to set the parameters for the serial port",e);
		}
	}
	
	
	@Override
	public void constructFromConfigurationFile(String path) throws IOException {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void connect() throws IOException {
		try {
			if (serial.openPort()) {
				throw new IOException("Unable to open the serial port: "+ port);
			}
		} catch (SerialPortException e) {
			throw new IOException("Unable to open the serial port: "+ port,e);
		}		
	}

	@Override
	public void disconnect() throws IOException {
		try {
			if (serial.closePort()) {
				throw new IOException("Unable to open the serial port: "+ port);
			}
		} catch (SerialPortException e) {
			throw new IOException("Unable to open the serial port: "+ port,e);
		}	
	}

	@Override
	public void sendCommand(String command, Parameter[] arguments) throws IOException {
		String commandString = PositionalSyntaxCommandConstructor.constructCommandString(command, arguments);
		try {
			if (serial.writeString(commandString)) {
				throw new IOException("Failed writing to the port"+ port);
			}
		} catch (SerialPortException e) {
			throw new IOException("Failed writing to the port"+ port,e);
		}
	}

	@Override
	public Object sendRequest(String command, Parameter[] arguments) throws IOException {
		String response;
		try {
			sendCommand(command, arguments);
			response = serial.readString();
		} catch (SerialPortException e) {
			throw new IOException("Failed reading from the port"+ port,e);
		}
		return response;
	}
	
	// setting logger.
	public void setLogger(String loggerName) {
		logger= LogManager.getLogger(loggerName);
	}
	public void setLogger(Logger lgr) {
		logger = lgr;
	}

	

}
