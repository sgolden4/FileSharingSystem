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
			System.out.println("Server started on port: "+myport);
		} catch(IOException e){
			System.out.println("unable to start server on port "+myport+", closing server.");
			e.printStackTrace();
			return;
		}
		while(serveractive){
			try{
				Socket socket = mysocket.accept();
				System.out.println("client connected from "
						+socket.getInetAddress().toString()+":"+socket.getPort());
				SWE622Server server = new SWE622Server(mysocket, socket, FILEPATH+myport+"\\");
				Thread thread = new Thread(server);
				thread.start();
			} catch(IOException e){
				
			}
			if(mysocket != null)
				try {
					mysocket.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		}
		
	}
	
	public static void onUploadComplete(String filename){
		int size = servers.size();
		for(int i=0; i<size; i++){
			FileDistributor fd = new FileDistributor(filename, servers.get(i));
			Thread thread = new Thread(fd);
			thread.start();
		}
	}

	private static void findOtherServers() {

		myport = STARTING_PORT;
		if(servers == null)
			servers = new ArrayList<Socket>();
		boolean openportfound = false;
		while(!openportfound){
			try{
				Socket socket = new Socket(SERVER_ADDRESS, myport);
				if(verifyServer(socket)){
					servers.add(socket);
					System.out.println("server found at port: "+myport+", adding to server list");
				}
				myport++;
			} catch(IOException e){
				openportfound = true;
			}
		}
	}
	
	private static boolean verifyServer(Socket socket) {
		boolean verified = false;
		try {
			BufferedReader input = new BufferedReader(new InputStreamReader(
	                socket.getInputStream()));
			PrintWriter pw = new PrintWriter(socket.getOutputStream(), true);
			pw.println("verify");
			String check = input.readLine();
			verified = "42".equals(check);
			if(verified)
				pw.println("server");
			return verified;
		} catch (IOException e) {
			return false;
		}
	}

	public static void addServer(Socket connection) {
		servers.add(connection);
		
	}
	
	public static void main(String [] args){
		findOtherServers();
		startServer();
	}
}
