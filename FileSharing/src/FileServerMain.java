import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class FileServerMain {
	private static final String SERVER_ADDRESS = "127.0.0.1";
	private static final int STARTING_PORT = 3210;
	private static final String FILEPATH = "server";
	private static final int MAX_PORT = 3220;
	private static int myport;
	private static ServerSocket mysocket;
	private static List<Integer> serverports;
	private static Map<Integer, String> servers;
	private static boolean serveractive = true;
	
	private static void checkDir(String directory) {
        File dir = new File(directory);
        if(!dir.exists()) {
            if(!dir.mkdir()) {
                System.out.println("Server directory does not exist and cannot be created... exiting!");
                System.exit(1);
            }
        }
	}
	
	private static void startServer() {
		checkDir(FILEPATH+myport);
		notifyServers();
		System.out.println("Server ready, now listening for connections...");
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
		int size = serverports.size();
		for(int i=0; i<size; i++){
			try {
				int port = serverports.get(i);
				Socket socket = new Socket(servers.get(port), port);
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
		int size = serverports.size();
		for(int i=0; i<size; i++){
			int port = serverports.get(i);
			FileDistributor fd = new FileDistributor(filepath, filename, servers.get(port), port);
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
	

	private static void findOtherServers(String[] args) {
		mysocket = null;
		String[] serverlist = new String[args.length+1];
		serverlist[0] = SERVER_ADDRESS;
		servers = new HashMap<Integer, String>();
		System.arraycopy(args, 0, serverlist, 1, args.length);
		myport = STARTING_PORT;
		int portnum = myport;
		if(serverports == null)
			serverports = new ArrayList<Integer>();
		boolean openportfound = false;
		while(portnum <= MAX_PORT){
			boolean portadded=false;
			for(int i=0; i<serverlist.length; i++){
				System.out.println("cehcking for server at: "+serverlist[i]+":"+portnum);
				portadded = false;
				try{
					Socket socket = new Socket(serverlist[i], portnum);
					openportfound = false;
					if(verifyServer(socket)){
						serverports.add(portnum);
						servers.put(portnum, serverlist[i]);
						System.out.println("server found at port: "+portnum+", adding to server list");
						portnum++;
						portadded=true;
					}
					socket.close();
				} catch(IOException e){
					openportfound = true;
				}
			}
			if(mysocket == null && openportfound){
				myport = portnum;
				try {
					mysocket = new ServerSocket(myport);
					System.out.println("Server started on port: "+myport);
					portnum++;
					portadded=true;
				} catch (IOException e1) {
					System.out.println("unable to start server on port "+myport+", closing server.");
					e1.printStackTrace();
					System.exit(1);
				}
			}
			if(!portadded)
				portnum++;
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

	public static void addServer(String address, int portnum) {
		serverports.add(portnum);
		servers.put(portnum, address);
		System.out.println("added server "+address+":"+portnum+" to servers list.");		
	}
	
	public static void main(String [] args){
		findOtherServers(args);
		startServer();
	}

}
