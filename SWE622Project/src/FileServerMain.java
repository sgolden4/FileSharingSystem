import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;


public class FileServerMain {
	private static final String SERVER_ADDRESS = "127.0.0.1";
	private static final int STARTING_PORT = 3210;
	private static final String FILEPATH = "c:\\temp\\server";
	private static final int MAX_PORT = 3220;
	private static int myport;
	private static ServerSocket mysocket;
	private static List<Integer> servers;
	private static boolean serveractive = true;
	
	
	private static void startServer() {
		try{
			mysocket = new ServerSocket(myport);
			System.out.println("Server started on port: "+myport);
			notifyServers();
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
		}
	}
	
	private static void notifyServers() {
		int size = servers.size();
		for(int i=0; i<size; i++){
			try {
				Socket socket = new Socket(SERVER_ADDRESS, servers.get(i));
				PrintWriter pw;
				pw = new PrintWriter(socket.getOutputStream(), true);
				pw.println("server "+myport);
				socket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public static void onUploadComplete(String filepath, String filename){
		int size = servers.size();
		for(int i=0; i<size; i++){
			FileDistributor fd = new FileDistributor(filepath, filename, servers.get(i));
			Thread thread = new Thread(fd);
			thread.start();
		}
	}
	
	@Override
	protected void finalize() {
		try {
			if(mysocket != null)
				mysocket.close();
			super.finalize();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Throwable e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	

	private static void findOtherServers() {

		myport = STARTING_PORT;
		int portnum = myport;
		if(servers == null)
			servers = new ArrayList<Integer>();
		boolean openportfound = false;
		while(portnum <= MAX_PORT){
			try{
				Socket socket = new Socket(SERVER_ADDRESS, portnum);
				if(verifyServer(socket)){
					servers.add(portnum);
					System.out.println("server found at port: "+portnum+", adding to server list");
				}
				portnum++;
				socket.close();
			} catch(IOException e){
				if(!openportfound){
					openportfound = true;
					myport = portnum;
				}
				portnum++;
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

	public static void addServer(int portnum) {
		servers.add(portnum);
		System.out.println("added server "+portnum+" to servers list.");		
	}
	
	public static void main(String [] args){
		findOtherServers();
		startServer();
	}
}
