import java.net.Socket;
import java.net.SocketException;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;

public class ClientSend implements Runnable {

    public final int TIMEOUT = 5000;
    
    private String FILE_FOLDER;  
    private String filename;
    private Socket sock;
    DataOutputStream outToServer = null;
    OutputStream os = null;
    BufferedInputStream bis = null;
    BufferedReader inFromServer = null;
    
    public ClientSend(String dir, String file, Socket s) {
        filename = file;
        sock = s;
        if (dir.endsWith("/")) {
            FILE_FOLDER = dir;
        } else {
            FILE_FOLDER = dir + '/';
        }
        try {
            sock.setSoTimeout(TIMEOUT);
        } catch (SocketException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    public void run() {
        
        File myFile = new File (FILE_FOLDER + filename);
        if (!myFile.exists()) {
            System.out.println("  File with that name cannot be found.");
            return;
        }
        
        System.out.println("  Starting upload.");
        
        long length = myFile.length(),  totalWrite = 0, offset = 0;
        byte[] bytearray = null;
        String offsetString = null;
        
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
                System.out.println("  File " + FILE_FOLDER + filename
                        + " uploaded. Size: " + (totalWrite + offset));
            } catch (IOException e) {
                if (totalWrite != 0) {
                    System.out.println("  Error in Uploading file. Sent" +
                            (totalWrite + offset) + " bytes.");
                } else {
                    System.out.println("  Failure to upload file: No bytes sent");
                }            
            }
            break;
        }
        
        closeConnections();
    }
    
    public void closeConnections() {
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
}
