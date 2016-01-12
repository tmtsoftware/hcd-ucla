package edu.ucla.astro.irlab.io.ice;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import Ice.Communicator;
import edu.ucla.astro.irlab.io.CommandInterface;
import edu.ucla.astro.irlab.io.InvalidCommandException;
import edu.ucla.astro.irlab.io.InvalidConfigurationException;
import edu.ucla.astro.irlab.io.InvalidParameterException;
import edu.ucla.astro.irlab.io.Parameter;

/* 
 * SidecarServer classes:
 * 
 * proxy: Sidecar.SidecarIce.ISidecarIceComPrx
 * helper (for cast): Sidecar.SidecarIce.ISidecarIceComPrxHelper
 * callback: edu.ucla.astro.irlab.sidecar.gpi.SidecarIceCallback; 
 * 
 */

public class ICECommandInterface implements CommandInterface {
	
	private final Logger logger = LogManager.getLogger(ICECommandInterface.class);

	private boolean isCallbackThreadRunning;
	private boolean getNewProxy;

	
	private Communicator ic;

	private Object proxyObject;
	private Class<?> proxyClass;
 	private String proxyClassName = "Sidecar.SidecarICE.ISidecarIceComPrx";
	private String iceProxyName;
 	
	private Ice.Object callbackObject;
	private String callbackClassName = "";
	
	private boolean createCallback = false;
	private HashMap<String, String> connectionProperties;
	HashMap<String, Method> proxyMethods = new HashMap<String, Method>();
	
	public ICECommandInterface(HashMap<String, String> settings) {
		isCallbackThreadRunning = false;
		getNewProxy = true;
		connectionProperties = settings;
		configureInterface();
	  startCommunicator();
	}
	private void configureInterface() {
		//. TODO throw exceptions 
		createCallback = false;
		proxyClassName = connectionProperties.get("proxyClassName");
		callbackClassName = connectionProperties.get("callbackClassName");
		if (callbackClassName != null) {
			if (callbackClassName.length() > 0) {
				createCallback = true;
			}
		}
		
		try {
			proxyClass = Class.forName(proxyClassName);
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		}  
		
		iceProxyName = constructProxyName();

		if (createCallback) {
			try {
				instantiateCallbackClass();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	private void startCommunicator() {
		logger.info("Initializing ICE connection...");
		Ice.Properties props = Ice.Util.createProperties();

		//. TODO use connectionProperties
		//. set max size of message to be one 2048x2048x4 byte frame
		//. plus a bit more for message overhead  (in kB)
		//.  = 2048 x 2048 x 4 bytes / 1024 bytes/kb + 4 kb
		props.setProperty("Ice.MessageSizeMax", "16388");
		props.setProperty("Ice.ACM.Client", "0");
		props.setProperty("Ice.Warn.Connections", "1");
		props.setProperty("Ice.Override.ConnectTimeout", "10000");

		Ice.InitializationData id = new Ice.InitializationData();
		id.properties = props;

		ic = Ice.Util.initialize(id);
		logger.info("success.");
	}

	
	@Override
	public void constructFromConfigurationFile(String path) throws IOException {
	}
		
	private void populateProxyMethods() throws IOException {
		//. go through configuration and fill in proxyMethods hash map
		//. this can only be done after connecting and instantiating a proxy object
		
		//. NOTE this isn't done yet.  right now, get method and add to map first time method is called.
		
	}
	
	public void instantiateCallbackClass() throws IOException {
		try {
			//. TODO callbackClassName should be passed in?
			Class<?> callbackClass = Class.forName(callbackClassName);  //. throws ClassNotFoundException
			callbackObject = (Ice.Object)(callbackClass.newInstance());  //. note cast.  perhaps needs a cast exception
			//. callback = new SidecarIceCallback();
		} catch (ClassNotFoundException cnfEx)	 {
			throw new IOException("Invalid callback class name: "+callbackClassName);
		} catch (InstantiationException iEx) {
			throw new IOException("Callback class <"+callbackClassName+"> cannot be instantiated.  Must have a non-abstract parameterless constructor.");
		} catch (IllegalAccessException iaEx) {
			throw new IOException("Parameterless constructor is not accessible for callback class <"+callbackClassName+">.");
		} catch (Exception ex) {
			//. invoke could also throw InvocationTargetException, SecurityException, ExceptionInInitializerError
			throw new IOException(ex);
		}

	}

	private String constructProxyName() {
		//. TODO: if null
		String proxy = connectionProperties.get("proxyName");
		String host = connectionProperties.get("host");
	  String port = connectionProperties.get("port");
	  String timeout = connectionProperties.get("timeout");
	  
	  //. TODO number format exception
	  int portNum = Integer.decode(port);
	  int timeoutNum = Integer.decode(timeout);
	  
		return String.format("%s -h %s -p %d -t %d", proxy, host, portNum, timeoutNum);
	}
	
	@Override
	public void connect() throws IOException {
		//. TODO: handle memory? possible leak

		try {
			/* if server is currently running, shut it down and restart */
			if (!ic.isShutdown()) {
				server_shutdown();
				startCommunicator();
			}


			if (getNewProxy) {
				
				logger.info("Getting ICE base... using " + iceProxyName);

				Ice.ObjectPrx iceBase = ic.stringToProxy(iceProxyName);
				if (iceBase == null) {
					throw new Exception("Cannot create proxy with url: " + iceProxyName);
				}
				logger.info(" success.  ObjectPrx OK.");

				logger.info("Creating proxy object...");
				String helperClassName = proxyClassName+"Helper";
				
				try {
					Class<?> proxyHelperClass = Class.forName(helperClassName);  //. throws ClassNotFoundException
					Method checkedCastMethod = proxyHelperClass.getDeclaredMethod("checkedCast", Ice.ObjectPrx.class);
					proxyObject = checkedCastMethod.invoke(null, (Object)iceBase);  // first arg is null because method is static
					//. sidecarProxy = Sidecar.SidecarIce.ISidecarIceComPrxHelper.checkedCast(iceBase);
				} catch (ClassNotFoundException cnfEx)	 {
					throw new IOException("Invalid proxy helper class name: "+helperClassName);
				} catch (NoSuchMethodException nsmEx) {
					throw new IOException("Proxy helper class <"+helperClassName+"> does not have a checkedCast method.");
				} catch (IllegalAccessException iaEx) {
					throw new IOException("Method checkedCast is not accessible for proxy helper class <"+helperClassName+">.");
				} catch (Exception ex) {
					//. invoke could also throw IllegalArgumentException, InvocationTargetException, NullPointerExceptoin, ExceptionInInitializerError
					//. note for InvocationTargetException: i don't think checkedCast throws exception
					throw new IOException(ex);
				}
				
				logger.info("Cast complete");
				if (proxyObject == null) {
					logger.info("Error creating proxy object.");
					throw new Exception("Invalid proxy");
				} else {
					logger.info("Proxy object created.");

					
					//. go ahead and use reflection to find all methods necessary based on configuration file
					populateProxyMethods();
					getNewProxy = false;

					if (createCallback) {
						if (!isCallbackThreadRunning) {
							logger.info("Starting callback thread...");

							Thread callbackThread = new Thread() {
								public void run() {
									try {
										callbackThreadFunction();
									} catch (IOException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
									}
								}
							};
							callbackThread.start();
						} else {
							logger.info("Callback thread already running.");
						}
					}
					logger.info("Initialization complete.");
				}      
			}
		} catch (Ice.LocalException ex) {
			logger.error("Ice Exception " + ex.getMessage());
			throw new IOException(ex);
		} catch (Exception e) {
		  logger.error("General error " + e.getMessage());
			throw new IOException(e);
		}
	}

	@Override
	public void disconnect() throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	public void sendCommand(String command, Parameter[] arguments)
			throws InvalidCommandException, InvalidParameterException, IOException {
		Object[] objarg = new Object[arguments.length];
		for (int ii = 0; ii<arguments.length; ii++) {
			objarg[ii] = arguments[ii].getValue();
		}
		
		Method m = getMethod(command, objarg);
		
		//. invoke returns null if method has void return type
		invokeProxyMethod(m, objarg);
	}

	@Override
	public Object sendRequest(String command, Parameter[] arguments)
			throws InvalidCommandException, InvalidParameterException, IOException {
		//. command = echo (string)
		//. command in this case is just first word
		Object[] objarg = new Object[arguments.length];
		for (int ii = 0; ii<arguments.length; ii++) {
			objarg[ii] = arguments[ii].getValue();
		}
		
		Method m = getMethod(command, objarg);
		
		//. invoke returns null if method has void return type
		return invokeProxyMethod(m, objarg);

	}

	private Method getMethod(String command, Object[] arguments)
			throws InvalidCommandException, InvalidParameterException, IOException {
		String methodName;
		int i = command.indexOf(" ");
		if (i < 0) {
			methodName = command;
		} else {
		 methodName = command.substring(0, i);
		}
		Method m = proxyMethods.get(methodName);
		if (m == null) {
			/* try to get method */
			Class<?>[] argumentTypes = new Class<?>[arguments.length];
			int ii=0;
			for (Object o : arguments) {
				argumentTypes[ii] = o.getClass();
				ii++;
			}
			try {
				m = proxyClass.getDeclaredMethod(methodName, argumentTypes);
				proxyMethods.put(command, m);
			} catch (SecurityException e) {
				e.printStackTrace();
				throw new InvalidCommandException("Command <"+methodName+"> is not a valid command.");
			} catch (NoSuchMethodException e) {
				e.printStackTrace();
				throw new InvalidCommandException("Command <"+methodName+"> is not a valid command.");
			}
		}
		return m;
	}
	/** Method to shutdown ICE communicator.
	 *
	 * @param : None
	 *
	 * @return none
	 */
	void  server_shutdown() {
		ic.shutdown();
		getNewProxy = true;
		/*   ic.destroy();*/
	}

	/** Callback thread for SIDECAR Ice Client
	 *
	 * Registers the callback method of the SidecarIceClient object 
	 *  with Sidecar Server as function to be called for callbacks.
	 * 
	 * @param obj [in] pointer to SidecearIceClient object
	 *
	 * @sa gpIfSidecarIceCallback.cpp
	 *
	 * @return none
	 * @throws IOException 
	 */
	void callbackThreadFunction() throws IOException {
		//. check for null?
		if (callbackObject == null) {
			//. TODO: improve
			throw new IOException("Callback object is null");
		}
		
		Ice.ObjectAdapter adapter = ic.createObjectAdapter("");
		Ice.Identity ident = new Ice.Identity();
		ident.name = Ice.Util.generateUUID();
		ident.category = "";
		adapter.add(callbackObject, ident);
		adapter.activate();
		((Ice.ObjectPrx)(proxyObject)).ice_getConnection().setAdapter(adapter);

		Object[] args = {ident};
		Method addCallbackMethod;
		try {
			addCallbackMethod = getMethod("addCallbackClient", args);
			
			if (addCallbackMethod != null ) { //. this check should be performed earlier?	
				invokeProxyMethod(addCallbackMethod, args);

				/* todo: synchronize? */
				logger.info("Callback Thread running...");
				setCallbackThreadRunning(true);

				ic.waitForShutdown();
			} else {
				throw new IOException("addCallbackClient method cannot be found.  Check configuration file.");
			}

		} catch (InvalidCommandException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvalidParameterException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		


		logger.info("Callback Thread exiting...");
		setCallbackThreadRunning(false);

	}
	
	/** Accessor method to set whether callback thread is running or not
	 *
	 * @param status [in] Whether callback thread is running
	 *
	 * @return none
	 */
	public synchronized void setCallbackThreadRunning(boolean status) {
		isCallbackThreadRunning=status;
	}

	private Object invokeProxyMethod(Method m, Object... args) throws IOException {
		try {
			//. for parameterless methods, args can be null or 0-length
			return m.invoke(proxyObject, args);  
			//. sidecarProxy = Sidecar.SidecarIce.ISidecarIceComPrxHelper.checkedCast(iceBase);
		} catch (IllegalAccessException iaEx) {
			throw new IOException("Method <"+m.getName()+"> is not accessible for proxy class <"+proxyClassName+">.");
		} catch (IllegalArgumentException argEx) {
			throw new IOException("Illegal argument for method <"+proxyClassName+"."+m.getName()+": "+ argEx.getMessage());
		} catch (InvocationTargetException itEx) {
			//. exception thrown by method
			Throwable ex = itEx.getCause();
			if  (ex instanceof Ice.TimeoutException) {
				throw new IOException("Ice Timeout Exception: " + ex.getMessage());
			} else if (ex instanceof Ice.LocalException ) {
				throw new IOException("Ice Exception: " + ex.getMessage());
			} else { 
				throw new IOException("Unknown InvocationException: "+ itEx.getMessage());
			}
		} catch (Exception ex) {
			//. invoke could also throw NullPointerExceptoin, ExceptionInInitializerError
			throw new IOException(ex);
		}
	}
	
	
}
