package hcd;

import java.io.FileReader;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;

import com.google.gson.Gson;

public class Tester {

	public static void main(String[] args) throws StringIndexOutOfBoundsException, SocketTimeoutException, UnknownHostException, InvalidParameterException, InvalidCommandException, IOException, InvalidOutputException, InterruptedException {
		
		String jsonFileName = "Lakeshore340.json"; 
		Gson gson = new Gson();
		FileReader fr = new FileReader(jsonFileName);
		Commander cmdr = gson.fromJson(fr, Commander.class);
		cmdr.setLogger("NewCommander");
		
		cmdr.connect();
		
		
		/*
		 * Type in the command string manually using sendRequest method
		 * and receives unformatted string output from the device
		 */
		System.out.println(cmdr.sendRequest("KRDG? C3"));		// +292.286E+0
		System.out.println(cmdr.sendRequest("DATETIME?"));		// 02,24,2014,13,00,33,235
		
		
		/*
		 * Example of using submit method
		 * the output is designed to be 2D ArrayList<Object> to handle multiple line responses.
		 */
		String[] param1 = {"C3"};
		ArrayList<ArrayList<Object>> result1 = cmdr.submit("getTempK", param1);
		System.out.println(result1);							//[[292.286]]
		
		ArrayList<ArrayList<Object>> result2 = cmdr.submit("getDateTime");
		System.out.println(result2);							//[[Mon Feb 24 13:06:36 PST 2014]]
		
		ArrayList<Object> param3 = new ArrayList<Object>();
		param3.add(new Date());
		ArrayList<ArrayList<Object>> result3 = cmdr.submit("setDateTime", param3);
		System.out.println(result3);							// null

		
		
		
		cmdr.disconnect();
		
		
		// Sample outputs.
		/*
		 * 13:06:41.709 [DEBUG] NewCommander - Opening socket on ets3 port 3006
		 * 13:06:41.741 [DEBUG] NewCommander - Socket Timeout set to 5000 milliseconds.
		 * 13:06:41.742 [DEBUG] NewCommander - Opening I/O stream
		 * 13:06:41.743 [DEBUG] NewCommander - Sending command, KRDG? C3
		 * 13:06:41.743 [DEBUG] NewCommander - Reading until terminator is reached.
		 * +292.287E+0
		 * 13:06:41.945 [DEBUG] NewCommander - Sending command, DATETIME?
		 * 13:06:41.945 [DEBUG] NewCommander - Reading until terminator is reached.
		 * 02,24,2014,13,06,36,643
		 * 13:06:42.057 [DEBUG] NewCommander - Sending command, KRDG? C3
		 * 13:06:42.058 [DEBUG] NewCommander - Reading until terminator is reached.
		 * 13:06:42.155 [DEBUG] NewCommander - Response to KRDG? C3
		 * +292.287E+0
		 * [[292.287]]
		 * 13:06:42.155 [DEBUG] NewCommander - Sending command, DATETIME?
		 * 13:06:42.156 [DEBUG] NewCommander - Reading until terminator is reached.
		 * 13:06:42.275 [DEBUG] NewCommander - Response to DATETIME?
		 * 02,24,2014,13,06,36,853
		 * [[Mon Feb 24 13:06:36 PST 2014]]
		 * 13:06:42.280 [DEBUG] NewCommander - Sending command, DATETIME 02,24,2014,13,06,42,279
		 * null
		 * 13:06:42.280 [DEBUG] NewCommander - Closing I/O stream
		 * 13:06:42.280 [DEBUG] NewCommander - Closing socket on ets3 port 3006
		 */
		

	}
}
