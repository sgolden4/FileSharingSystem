import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;


public class FileServerMain {
	private static final String SERVER_ADDRESS = "127.0.0.1";
	private static final int STARTING_PORT = 3210;
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
				SWE622Server server = new SWE622Server(mysocket, socket);
				Thread thread = new Thread(server);
				thread.start();
			} catch(IOException e){
				
			}
		}
		
	}
	
	public static void onUploadComplete(){
		
	}
	
	public static void onDownloadComplete(){
		
	}

	private static void findOtherServers() {
		// TODO Auto-generated method stub
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
		// TODO Auto-generated method stub
		return false;
	}

	public static void main(String [] args){
		findOtherServers();
		startServer();
	}
}
