import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;


public class FileServerMain {
	private static final String SERVER_ADDRESS = "127.0.0.1";
	private static final int STARTING_PORT = 3210;
	private static final String FILEPATH = "c:\\temp\\server";
	private static int myport;
	private static ServerSocket mysocket;
	private static List<Socket> servers;
	private static boolean serveractive = true;
	
	
	private static void startServer() {
		try{
			mysocket = new ServerSocket(myport);
		} catch(IOException e){
			
		}
		while(serveractive){
			try{
				Socket socket = mysocket.accept();
				SWE622Server server = new SWE622Server(mysocket, socket, FILEPATH+myport+"\\");
				Thread thread = new Thread(server);
				thread.start();
			} catch(IOException e){
				
			}
		}
		
	}
	
	public static void onUploadComplete(String filename){
		//TODO: 
		
	}

	private static void findOtherServers() {

		myport = STARTING_PORT;
		if(servers == null)
			servers = new ArrayList<Socket>();
		boolean openportfound = false;
		while(!openportfound){
			try{
				Socket socket = new Socket(SERVER_ADDRESS, myport);
				if(verifyServer(socket))
					servers.add(socket);
				myport++;
			} catch(IOException e){
				openportfound = true;
			}
		}
	}
	
	private static boolean verifyServer(Socket socket) {

		try {
			BufferedReader input = new BufferedReader(new InputStreamReader(
	                socket.getInputStream()));
			PrintWriter pw = new PrintWriter(socket.getOutputStream(), true);
			pw.println("verify");
			String check = input.readLine();
			return "42".equals(check);
		} catch (IOException e) {
			return false;
		}
	}

	public static void main(String [] args){
		findOtherServers();
		startServer();
	}
}
