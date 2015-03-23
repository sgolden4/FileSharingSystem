import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.util.Scanner;


public class FileClientMain {

    public final static int[] SOCKET_PORT = {3210,3211,3212};
    public final static String[] SERVER_ADDRESS = {"127.0.0.1", "127.0.0.1", "127.0.0.1"};

    public static boolean EXIT = false;  
    public static Scanner keyboard;
    public static String directory;

    public static void main (String [] args) throws IOException {
        if(args.length != 1) {
            System.out.println("Must name directory for downloaded/uploading files.");
            return;
        }
        File dir = new File(args[0]);
        if(!dir.exists()) {
            if(!dir.mkdir()) {
                System.out.println("  Directory could not be made with that path");
                return;
            }
        }
        directory = args[0];
        
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
                case 2: dl(input); break;
                case 3: ul(input); break;
                case 4: help(); break;
                case 5: exit(); break;     
                }
            }

        } finally {
            keyboard.close();
        }
    }

    private static int parseInput(String input) {
        String[] tokens = input.split(" ");
        switch (tokens[0]) {
        case "listserv": return 1;
        case "dl": return 2;
        case "ul": return 3;
        case "help": return 4;
        case "exit": return 5;
        default: System.out.println("  Unrecognized Command");
        }
        return 0;
    }

    public static void listserv() throws IOException {
        Socket[] sock = new Socket[3];
        for (int i = 0; i < SOCKET_PORT.length; i++ ) {
            try {
                sock[i] = new Socket(SERVER_ADDRESS[i], SOCKET_PORT[i]);
                System.out.printf("  Connecting to %s...\n",SERVER_ADDRESS[i]); 
            } catch (SocketException se) {
                System.out.printf("  Cannot Connect to %s\n", SERVER_ADDRESS[i]);
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
        for(int i = 0; i < SERVER_ADDRESS.length; i++) {
            if (SERVER_ADDRESS[i].equalsIgnoreCase(servname)) {
                socket = SOCKET_PORT[i] ;
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
            sock = new Socket(servname,socket);
        } catch (IOException ioe) {
            System.out.printf("  Cannot Connect to %s\n", servname);
        }
        return sock;
    }

    private static void help() {
        System.out.println("Command options are as follows:\n" +
                "(1) listserv   :: Lists all available Servers\n" +
                "(2) dl  <filename> <Server Name> :: Downloads file <filename> from Server <ServerName>\n" +
                "(3) ul  <filename> <Server Name> :: Uploads file at <filepath> to Server <ServerName>\n" + 
                "(4) help  :: Lists all options\n" +
                "(5) exit  :: Closes File Sharing client\n");
    }

    private static void exit() {
        EXIT = true;
    }
}
