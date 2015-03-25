import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.util.Scanner;


public class FileClientMain {

    public final static int[] SOCKET_PORT = {3210,3211,3212};
    public final static String[] SERVER_ADDRESS = {"127.0.0.1", "127.0.0.1", "127.0.0.1"};
    public final static String[] SERVER_NAMES = {"Server1", "Server2", "Server3"};
    public static boolean EXIT = false;  
    public static Scanner keyboard;
    public static String directory;
    private static String[] serveraddresses;

    public static void main (String [] args) throws IOException {
        if(args.length < 1) {
            //System.out.println("Must name directory for downloaded/uploading files.");
        	directory = ".";
            //return;
        	serveraddresses = SERVER_ADDRESS;
        } else{
        	directory = args[0];
    		serveraddresses = new String[args.length-1];
    		System.arraycopy(args, 1, serveraddresses, 0, args.length-1);        	
        }
        File dir = new File(directory);
        if(!dir.exists()) {
            if(!dir.mkdir()) {
                System.out.println("  Directory could not be made with that path");
                return;
            }
        }
        

        
        keyboard = new Scanner(System.in);
        try {
            System.out.println("Welcome to our File Server!\n");
            help();
            while (!EXIT) {
                System.out.print("> ");
                String input = keyboard.nextLine();
                if (input == null) {
                    System.out.println("  Unrecognized Command");
                    continue;
                }
                input = input.trim();
                int op = parseInput(input);
                switch (op) {
                case 1: listserv(); break;
                case 2: listfiles(input); break;
                case 3: dl(input); break;
                case 4: ul(input); break;
                case 5: help(); break;
                case 6: exit(); break;     
                }
            }

        } finally {
            keyboard.close();
        }
    }

    private static void listfiles(String input) {
        String[] tokens = input.split(" ");
        if (tokens.length != 2) {
            System.out.println("  Invalid Command length");
            return;
        }
        String servname = tokens[1];

        Socket sock = getSocket(servname);
        if (sock == null) {
            return;
        } else {
            try {
            	String inputline = "";
            	System.out.println("Getting directory listing from "+servname+":");
    			PrintWriter pw = new PrintWriter(sock.getOutputStream(), true);
    			pw.println("listfiles");
				BufferedReader inFromServer = new BufferedReader(new InputStreamReader(sock.getInputStream()));
				while(!"done!".equals(inputline)){
					inputline = inFromServer.readLine();
					System.out.println(inputline);
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        }
	}

	private static int parseInput(String input) {
        String[] tokens = input.split(" ");
        switch (tokens[0]) {
        case "listserv": return 1;
        case "listfiles": return 2;
        case "dl": return 3;
        case "ul": return 4;
        case "help": return 5;
        case "exit": return 6;
        default: System.out.println("  Unrecognized Command");
        }
        return 0;
    }

    public static void listserv() throws IOException {
        Socket[] sock = new Socket[3];
        for (int i = 0; i < SOCKET_PORT.length; i++ ) {
            try {
                sock[i] = new Socket(serveraddresses[i], SOCKET_PORT[i]);
                System.out.printf("  Connecting to <%s> : %s:%d ...\n",SERVER_NAMES[i], serveraddresses[i],SOCKET_PORT[i]); 
            } catch (SocketException se) {
                System.out.printf("  Cannot Connect to <%s> : %s:%d\n",SERVER_NAMES[i], serveraddresses[i],SOCKET_PORT[i]);
            } finally {
                for(Socket s: sock) {
                    if (s != null) {
                        s.close();
                    }
                }
            }
        }
    }

    public static void dl(String input) {
        //Parse DL Command
        String[] tokens = input.split(" ");
        if (tokens.length != 3) {
            System.out.println("  Invalid Command length");
            return;
        }
        String filename = tokens[1];
        String servname = tokens[2];

        Socket sock = getSocket(servname);
        if (sock == null) {
            return;
        } else {
            ClientReceive cr = new ClientReceive(directory, filename, sock);
            Thread thread = new Thread(cr);
            thread.start();
            try {
                thread.join();
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    public static void ul(String input) {
        //Parse DL Command
        String[] tokens = input.split(" ");
        if (tokens.length != 3) {
            System.out.println("  Invalid Command length");
            return;
        }
        String filename = tokens[1];
        String servname = tokens[2];

        Socket sock = getSocket(servname);
        if (sock == null) {
            return;
        } else {
            ClientSend cs = new ClientSend(directory, filename, sock);
            Thread thread = new Thread(cs);
            thread.start();
            try {
                thread.join();
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    private static Socket getSocket(String servname) {
        int socket = -1;
        String servaddr = null;
        for(int i = 0; i < serveraddresses.length; i++) {
            if (serveraddresses[i].equalsIgnoreCase(servname)) {
                socket = SOCKET_PORT[i] ;
                servaddr = serveraddresses[i];
                break;
            }
        }

        if (socket == -1) {
            System.out.println("  Server with that name does not exist");
            return null;
        }

        //Attempt Connection with selected Server
        Socket sock = null;
        try {
            sock = new Socket(servaddr,socket);
        } catch (IOException ioe) {
            System.out.printf("  Cannot Connect to %s\n", servname);
        }
        return sock;
    }

    private static void help() {
        System.out.println("Command options are as follows:\n" +
                "(1) listserv   :: Lists all available Servers\n" +
                "(2) listfiles <Server Name>   :: Lists all files on server <ServerName>\n" +
                "(3) dl  <filename> <Server Name> :: Downloads file <filename> from Server <ServerName>\n" +
                "(4) ul  <filename> <Server Name> :: Uploads file at <filepath> to Server <ServerName>\n" + 
                "(5) help  :: Lists all options\n" +
                "(6) exit  :: Closes File Sharing client\n");
    }

    private static void exit() {
        EXIT = true;
    }
}
