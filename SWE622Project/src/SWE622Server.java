import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;


public class SWE622Server implements Runnable {
	Socket connection;
	ServerSocket mysocket;
	BufferedReader input;
	OutputStream output;
	
	SWE622Server(ServerSocket mysocket, Socket connection){
		this.connection = connection;
		this.mysocket = mysocket;
	}

	public void run() {
		try {
			input = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			String instring = input.readLine();
			String[] instringparts = instring.split(" ");
			switch(instringparts[0]){
				case "dl": sendFile(instringparts);
					break;
				case "ul": receiveFile(instringparts);
					break;
				case "verify": verify();
					break;
				
			}
			
		} catch (IOException e) {

		}
		
	}

	private void verify() {
		//TODO: reply to verify that this is the correct server type
		
	}

	private void receiveFile(String[] instringparts) {
		/* TODO: 1. check string for whether we are resuming or not
				 2. check if file exists already
				 3. create file if it doesn't exist and we are not resuming, 
				 		open file if it does exist and we are resuming
				 4. send "receiving\n" if everything is good, otherwise send "failure\n" and return. 
				 5. get data from client
				 6. if file finished, call FileServerMain.onUploadComplete(filename) so it can
				  	start sending the file to the other servers.
		*/
	}

	private void sendFile(String[] instringparts) {
		/* TODO 1. check string for whether we are resuming or not
				2. check if file exists, reply "sending\n" if it does, reply "failure\n" 
					if it does not and return
				3. open file
				4. send file

		*/
	}

}
