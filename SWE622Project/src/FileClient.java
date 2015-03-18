import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.net.Socket;
import java.net.SocketException;


public class FileClient {

    public final static int[] SOCKET_PORT = {3210,3211,3212};
    public final static String[] SERVER = {"1","2","3"};  //Server names
    public final static String SERVER_ADDRESS = "127.0.0.1";
    public final static String FILE_TO_RECEIVED = "c:/temp/client/";  
    public final static int PACKET_SIZE = 10000000;

    public static boolean EXIT = false; 
    public static Map<String, Long> ResumeDLList = new HashMap<String, Long>(); 
    public static Map<String, String> ResumeULList = new HashMap<String, String>(); 

    public static Scanner keyboard;
    
    public static void main (String [] args ) throws IOException {
        keyboard = new Scanner(System.in);
        try {
            System.out.println("Welcome to our File Server!\n");
            help();
            while (!EXIT) {
                System.out.print("> ");
                String input = keyboard.nextLine();
                input = input.trim();
                int op = parseInput(input);
                switch (op) {
                case 1: listserv(); break;
                case 2: dl(input); break;
                case 3: ul(input); break;
                case 4: rdl(input); break;
                case 5: rul(input); break;
                case 6: help(); break;
                case 7: exit(); break;     
                }
            }

        }
        finally {
            keyboard.close();
        }
    }

    public static void help() {
        System.out.println("Command options are as follows:\n" +
                "(1) listserv   :: Lists all available Servers\n" +
                "(2) dl  <filename> <Server Name> :: Downloads file <filename> from Server <ServerName>\n" +
                "(3) ul  <filename> <Server Name> :: Uploads file at <filepath> to Server <ServerName>\n" + 
                "(4) rdl <filename> <Server Name> :: Resumes downloading a partially downloaded file\n " +                
                "                                    from Server <ServerName>\n" +
                "(5) rul <filename>               :: Resumes an upload of file <filename> to the same Server\n" +
                "                                    as the previous upload attempt\n" +
                "(6) help  :: Lists all options\n" +
                "(7) exit  :: Closes File Sharing client\n");
    }

    public static void listserv() throws IOException {
        Socket[] sock = new Socket[3];
        for (int i = 0; i < SOCKET_PORT.length; i++ ) {
            try {
                sock[i] = new Socket(SERVER_ADDRESS, SOCKET_PORT[i]);
                System.out.printf("  Connecting to %s...\n",SERVER[i]); 
            } catch (SocketException se) {
                System.out.printf("  Cannot Connect to %s\n", SERVER[i]);
            } finally {
                for(Socket s: sock) {
                    if (s != null) {
                        s.close();
                    }
                }
            }
        }
    }

    public static int parseInput(String input) {
        String[] tokens = input.split(" ");    
        switch (tokens[0]) {
        case "listserv": return 1;
        case "dl": return 2;
        case "ul": return 3;
        case "rdl": return 4;
        case "rul": return 5;
        case "help": return 6;
        case "exit": return 7;
        default: System.out.println("  Unrecognized Command");
        }
        return 0;
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
        int socket = -1;
        for(int i = 0; i < SERVER.length; i++) {
            if (SERVER[i].equalsIgnoreCase(servname)) {
                socket = SOCKET_PORT[i] ;
                break;
            }
        }
        
        if (socket == -1) {
            System.out.println("  Server with that name does not exist");
            return;
        }

        servname = SERVER_ADDRESS;

        BufferedOutputStream toFile = null;
        BufferedReader inFromServer = null;
        InputStream fromServer = null;
        DataOutputStream outToServer = null;
        Socket sock = null;


        int bytesRead = 0;
        int current = 0;
        byte [] packet  = null;
        String filesize, response;
        long totalBytes = 0, length;

        //Attempt Connection with selected Server
        try {
            sock = new Socket(servname,socket);
        } catch (IOException ioe) {
            System.out.printf("  Cannot Connect to %s\n", servname);
            return;
        }


        //Run until finished, then break from loop and close all connections
        while (true) {
            //Get IO Streams
            try {
                fromServer = sock.getInputStream();
                inFromServer = new BufferedReader(new InputStreamReader(sock.getInputStream()));
                toFile = new BufferedOutputStream(new FileOutputStream(FILE_TO_RECEIVED + filename));
                outToServer = new DataOutputStream(sock.getOutputStream());
            } catch (IOException e) {
                System.out.println("  Failure to get IO Streams.");
                break;
            }

            //Send and Receive Info
            try {
                outToServer.writeBytes("dl " + filename + '\n');

                response = inFromServer.readLine();
                
                if (!response.equals("sending")) {
                    System.out.println("  " + response);
                    break;
                }
                
                filesize = inFromServer.readLine();
                if (filesize != null) {
                    try {
                        length = Long.parseLong(filesize.replaceAll("\n", ""));
                    } catch (NumberFormatException e) {
                        System.out.println("  Invalid file size received from server.");
                        break;
                    }
                } else {
                    System.out.println("  Could not get filesize from server.");
                    break;
                }
                System.out.println("  File size: " + Long.parseLong(filesize.replaceAll("\n", "")));

                totalBytes = 0;
                int buffersize = sock.getReceiveBufferSize();
                packet = new byte[buffersize];
                while (totalBytes < length) {
                    bytesRead = fromServer.read(packet,0, buffersize);
                    current = bytesRead;
                    if (current != -1) {
                        totalBytes += current;
                        toFile.write(packet, 0 , current);
                    } 
                    toFile.flush();
                }
                System.out.println("  File " + FILE_TO_RECEIVED + filename
                        + " downloaded. Size: " + totalBytes);

            } catch (IOException e) {
                if (totalBytes != 0) {
                    System.out.println("  Error in Receiving file. Can resume download using command rdl");
                    ResumeDLList.put(filename, totalBytes);
                } else {
                    System.out.println("  Failure to receive file: No bytes read");
                }
            }

            break;
        }

        //Close connections
        try {
            if (sock != null)  sock.close();
            if (fromServer != null)  fromServer.close();
            if (inFromServer != null)  inFromServer.close();
            if (outToServer != null)  outToServer.close();
            if (toFile != null)  toFile.close();
        } catch (IOException e) {
            System.out.println("  Unable to close connections");
            return;
        }
    }

    public static void ul(String input) {
        //Parse UL command
        String[] tokens = input.split(" ");
        if (tokens.length != 3) {
            System.out.println("  Invalid Command length");
            return;
        }
        String filename = tokens[1];
        String servname = tokens[2];
        int socket = -1;
        for(int i = 0; i < SERVER.length; i++) {
            if (SERVER[i].equalsIgnoreCase(servname)) {
                socket = SOCKET_PORT[i] ;
                break;
            }
        }
        if (socket == -1) {
            System.out.println("  Server with that name does not exist");
            return;
        }
        
        File myFile = new File (FILE_TO_RECEIVED + filename);
        if (!myFile.exists()) {
            System.out.println("  File with that name cannot be found.");
            return;
        }
        Socket sock = null;
        DataOutputStream outToServer = null;
        OutputStream os = null;
        BufferedInputStream bis = null;
        BufferedReader inFromServer = null;


        long length = myFile.length();
        byte[] bytearray = null;
        long totalWrite = 0;
        
        //Attempt Connection with selected Server
        try {
            sock = new Socket(SERVER_ADDRESS,socket);
        } catch (IOException ioe) {
            System.out.printf("  Cannot Connect to %s\n", servname);
            return;
        }

        while (true) {
            //Get IO Streams
            try {
                outToServer = new DataOutputStream(sock.getOutputStream());
                os = sock.getOutputStream();
                bis = new BufferedInputStream(new FileInputStream(myFile));
                inFromServer = new BufferedReader(new InputStreamReader(sock.getInputStream()));

            } catch (IOException e) {
                System.out.println("  Failure to get IO Streams");
                break;
            }

            //Send and Receive Info
            try {
                outToServer.writeBytes("ul " + filename + ' ' + myFile.length() + '\n');
                String inString = inFromServer.readLine();
                if(inString == null || !"ready".equals(inString)){
                    System.out.println("  No response from server.");
                	break;
                }
    			int buffersize = sock.getReceiveBufferSize();
    			bytearray = new byte[buffersize];
                while (totalWrite < length) {
                    if (length - totalWrite < buffersize) {
                        int remaining = (int) (length - totalWrite);
                        byte[] b = new byte[remaining];
                        int read = bis.read(b);
                        os.write(b);
                        totalWrite += read;
                    } else {
                        int read = bis.read(bytearray);
                        os.write(bytearray);
                        totalWrite += read;
                    }
                }
                System.out.println("  File " + FILE_TO_RECEIVED + filename
                        + " uploaded. Size: " + length);
            } catch (IOException e) {
                if (totalWrite != 0) {
                    System.out.println("  Error in Uploading file. Can resume upload using command rul");
                    ResumeULList.put(filename, servname);
                } else {
                    System.out.println("  Failure to upload file: No bytes sent");
                }            
            }
            break;
        }
        
        //Close Connections
        try {
            if (sock != null) sock.close();
            if (bis != null) bis.close();
            if (outToServer != null) outToServer.close();
            if (os != null) os.close();
        } catch (IOException e) {
            System.out.println("  Failure to close connections");
            return;
        }
    }

    public static void rdl(String input) {
        //Parse RDL command
        String[] tokens = input.split(" ");
        if (tokens.length != 3) {
            System.out.println("  Invalid Command length");
            return;
        }
        String filename = tokens[1];
        String servname = tokens[2];
        int socket = -1;
        for(int i = 0; i < SERVER.length; i++) {
            if (SERVER[i].equalsIgnoreCase(servname)) {
                socket = SOCKET_PORT[i] ;
                break;
            }
        }
        if (socket == -1) {
            System.out.println("  Server with that name does not exist");
            return;
        }
        
        servname = SERVER_ADDRESS;
        
        Long bytesDownloaded = ResumeDLList.get(filename);
        if (bytesDownloaded == null) {
            File fileOnSystem = new File(FILE_TO_RECEIVED + filename);
            if (!fileOnSystem.exists()) {
                System.out.println("  Cannot find partially downloaded file");
                return;
            } else {
                bytesDownloaded = fileOnSystem.length();
            }
        }
        
        BufferedOutputStream toFile = null;
        BufferedReader inFromServer = null;
        InputStream fromServer = null;
        DataOutputStream outToServer = null;
        Socket sock = null;


        int bytesRead = 0;
        int current = 0;
        byte [] packet  = null;
        String filesize, response;
        long totalBytes = 0, length;

        //Attempt Connection with selected Server
        try {
            sock = new Socket(servname,socket);
        } catch (IOException ioe) {
            System.out.printf("  Cannot Connect to %s\n", servname);
            return;
        }


        //Run until finished, then break from loop and close all connections
        while (true) {
            //Get IO Streams
            try {
                fromServer = sock.getInputStream();
                inFromServer = new BufferedReader(new InputStreamReader(sock.getInputStream()));
                toFile = new BufferedOutputStream(new FileOutputStream(FILE_TO_RECEIVED + filename, true));
                outToServer = new DataOutputStream(sock.getOutputStream());
            } catch (IOException e) {
                System.out.println("  Failure to get IO Streams.");
                break;
            }

            //Send and Receive Info
            try {
                outToServer.writeBytes("dl " + filename + " resume " +
                        bytesDownloaded.toString() + '\n');

                response = inFromServer.readLine();
                if (response == null) {
                    System.out.println("  No response from Server.");
                    break;
                } else if (!response.equals("sending")) {
                    System.out.println("  " + response);
                    break;
                }
                
                filesize = inFromServer.readLine();
                if (filesize != null) {
                    try {
                        length = Long.parseLong(filesize.replaceAll("\n", ""));
                    } catch (NumberFormatException e) {
                        System.out.println("  Invalid file size received from server.");
                        break;
                    }
                } else {
                    System.out.println("  Could not get filesize from server.");
                    break;
                }

                System.out.println("  Remaining File size: " + length);

                totalBytes = 0;
                int buffersize = sock.getReceiveBufferSize();
                packet = new byte[buffersize];
                while (totalBytes < length) {
                    bytesRead = fromServer.read(packet,0, buffersize);
                    current = bytesRead;
                    if (current != -1) {
                        totalBytes += current;
                        toFile.write(packet, 0 , current);
                    } 
                    toFile.flush();
                }
                System.out.println("  File " + FILE_TO_RECEIVED + filename
                        + " downloaded. Size: " + (totalBytes + bytesDownloaded));
                ResumeULList.remove(filename);
            } catch (IOException e) {
                if (totalBytes != 0) {
                    System.out.println("  Error in Receiving file. Can resume download using command rdl");
                    ResumeDLList.replace(filename, bytesDownloaded + totalBytes); 
                } else {
                    System.out.println("  Failure to receive file: No bytes read");
                }
            }

            break;
        }

        //Close connections
        try {
            if (sock != null)  sock.close();
            if (fromServer != null)  fromServer.close();
            if (inFromServer != null)  inFromServer.close();
            if (outToServer != null)  outToServer.close();
            if (toFile != null)  toFile.close();
        } catch (IOException e) {
            System.out.println("  Unable to close connections");
            return;
        }
        
    }
    
    public static void rul(String input) {
      //Parse RDL command
        String[] tokens = input.split(" ");
        if (tokens.length != 2) {
            System.out.println("  Invalid Command length");
            return;
        }
        String filename = tokens[1];
        String servname = ResumeULList.get(filename);
        if (servname == null) {
            System.out.println("  A previous partial upload does not exist for this session");
            boolean valid = false;
            while (!valid) {
                System.out.println("  Would you like to search a server for a partial file? (yes/no)");
                System.out.print("> ");
                String yesOrNo = keyboard.nextLine();
                if ("yes".equalsIgnoreCase(yesOrNo)) {
                    System.out.println("  Which server would you like to search?");
                    System.out.print("> ");
                    servname = keyboard.nextLine();
                    valid = true;
                } else if ("no".equalsIgnoreCase(yesOrNo)) {
                    return;
                } else {
                    System.out.print("  Invalid response.");
                }
            }
        }
        int socket = -1;
        for(int i = 0; i < SERVER.length; i++) {
            if (SERVER[i].equalsIgnoreCase(servname)) {
                socket = SOCKET_PORT[i] ;
                break;
            }
        }
        if (socket == -1) {
            System.out.println("  Server with that name does not exist");
            return;
        }

        File myFile = new File (FILE_TO_RECEIVED + filename);
        if (!myFile.exists()) {
            System.out.println("  File with that name cannot be found.");
            return;
        }
        Socket sock = null;
        DataOutputStream outToServer = null;
        OutputStream os = null;
        BufferedInputStream bis = null;
        BufferedReader inFromServer = null;


        long length = myFile.length();
        byte[] bytearray = null;
        long totalWrite = 0;
        String offsetString = null;
        Long offset;
        
        //Attempt Connection with selected Server
        try {
            sock = new Socket(SERVER_ADDRESS,socket);
        } catch (IOException ioe) {
            System.out.printf("  Cannot Connect to %s\n", servname);
            return;
        }

        while (true) {
            //Get IO Streams
            try {
                outToServer = new DataOutputStream(sock.getOutputStream());
                os = sock.getOutputStream();
                bis = new BufferedInputStream(new FileInputStream(myFile));
                inFromServer = new BufferedReader(new InputStreamReader(sock.getInputStream()));
            } catch (IOException e) {
                System.out.println("  Failure to get IO Streams");
                break;
            }

            //Send and Receive Info
            try {
                outToServer.writeBytes("ul " + filename + ' ' + myFile.length() + " resume\n");
                offsetString = inFromServer.readLine();
                if (offsetString != null) {
                    try {
                        offset = Long.parseLong(offsetString);
                    } catch (NumberFormatException e) {
                        System.out.println("  Invalid file size received from server.");
                        break;
                    }
                } else {
                    System.out.println("  Invalid file size received from server.");
                    break;
                }
                bis.skip(offset);
                String inString = inFromServer.readLine();
                if(!"ready".equals(inString)){
                    break;
                }
                int buffersize = sock.getReceiveBufferSize();
                bytearray = new byte[buffersize];
                while (totalWrite < (length - offset)) {
                    if (length - totalWrite < buffersize) {
                        int remaining = (int) (length - totalWrite);
                        byte[] b = new byte[remaining];                      
                        int read = bis.read(b);
                        os.write(b);
                        totalWrite += read;
                    } else {
                        int read = bis.read(bytearray);
                        os.write(bytearray);
                        totalWrite += read;
                    }
                }
                System.out.println("  File " + FILE_TO_RECEIVED + filename
                        + " uploaded. Size: " + (totalWrite + offset));
            } catch (IOException e) {
                if (totalWrite != 0) {
                    System.out.println("  Error in Uploading file. Can resume upload using command rul");
                    ResumeULList.put(filename, servname);
                } else {
                    System.out.println("  Failure to upload file: No bytes sent");
                }            
            }
            break;
        }
        
        //Close Connections
        try {
            if (sock != null) sock.close();
            if (bis != null) bis.close();
            if (outToServer != null) outToServer.close();
            if (os != null) os.close();
            if (inFromServer != null) inFromServer.close();
        } catch (IOException e) {
            System.out.println("  Failure to close connections");
            return;
        }
        
    }
    
    
    public static void exit() {
        EXIT = true;
    }
}

