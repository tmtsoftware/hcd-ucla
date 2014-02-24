hcd-ucla
========

HCD code from UCLA

Commander class is the class that communicates to the devices
using pre-configured Command objects and Parameter objects read from JSON file.
Also in JSON file, there are socket communicating informations such as address, port, timeout, terminators, etc.
Each JSON file will hold information for one device. 

Once JSON file was read and Commander object is deserialized using JSON, first thing to do would be connecting.
For connecting, use connect() method. It will open a socket connection to the device, and open I/O stream as well.
The socket connection will stay connected until disconnect() method is called.
After connection is established with the device, one of the three methods and their overloaded methods can be used to communicate with the device.

sendCommand(String) - used for command that does not expect any response.
sendRequest(String,int) - used for command that requests some kind of response.
submit(String, ArrayList<Object>) - main method that has ability to distinguish command and request, as well as output formatting.

