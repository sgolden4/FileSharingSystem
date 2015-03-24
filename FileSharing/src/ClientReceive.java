import java.net.Socket;
import java.net.SocketException;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class ClientReceive implements Runnable {

    public final int TIMEOUT = 11000;
    
    private String FILE_FOLDER;
    private String filename;
    private Socket sock;
    private BufferedOutputStream toFile = null;
    private BufferedReader inFromServer = null;
    private InputStream fromServer = null;
    private DataOutputStream outToServer = null;

    public ClientReceive(String dir, String file, Socket s) {
        filename = file;
        sock = s;
        if (dir.endsWith("/") || dir.endsWith("\\")) {
            FILE_FOLDER = dir;
        } else {
            FILE_FOLDER = dir + '/';
        }
        /*try {
            sock.setSoTimeout(TIMEOUT);
        } catch (SocketException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }*/
    }

    public void run() {
        System.out.println("  Starting download.");

        long bytesDownloaded;
        int bytesRead = 0;
        int current = 0;
        byte [] packet  = null;
        String filesize, response;
        long totalBytes = 0, length;

        File file = new File(FILE_FOLDER + filename);
        if (!file.exists()) {
            bytesDownloaded = 0;
        } else {
            bytesDownloaded = file.length();
        }

        //Run until finished, then break from loop and close all connections
        while (true) {
            //Get IO Streams
            try {
                fromServer = sock.getInputStream();
                inFromServer = new BufferedReader(new InputStreamReader(sock.getInputStream()));
                toFile = new BufferedOutputStream(new FileOutputStream(FILE_FOLDER + filename, true));
                outToServer = new DataOutputStream(sock.getOutputStream());
            } catch (IOException e) {
                System.out.println("  Failure to get IO Streams.");
                break;
            }

            //Send and Receive Info
            try {
                outToServer.writeBytes("dl " + filename + " resume " +
                        bytesDownloaded + '\n');

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
                outToServer.writeBytes("ready\n");
                while (totalBytes < length && current != -1) {
                    bytesRead = fromServer.read(packet,0, buffersize);
                    current = bytesRead;
                    if (current != -1) {
                        totalBytes += current;
                        toFile.write(packet, 0 , current);
                    } 
                    toFile.flush();
                    int j = (int) ((totalBytes * 80.0)/length);
                    System.out.printf("  Received: %5.2f", (double) (totalBytes * 100.0)/length);
                    System.out.print("%  [");
                    for (int i = 0; i < 20; i++) {
                        if (i < j/4) {
                            System.out.print("#");
                        } else {
                            int k = j%4;
                            switch (k) {
                            case 0: System.out.print("-"); break;
                            case 1: System.out.print("\\"); break;
                            case 2: System.out.print("|"); break;
                            case 3: System.out.print("/"); break;
                            }
                        }
                    }
                    System.out.print("]\r");
                }
                if (totalBytes < length) {
                    System.out.println("Only downloaded " + totalBytes + 
                            "out of " + (bytesDownloaded + length));
                }
                System.out.println("  File " + FILE_FOLDER + filename
                        + " downloaded. Size: " + (totalBytes + bytesDownloaded));
            } catch (IOException e) {
                if (totalBytes != 0) {
                    System.out.println("  Error in Receiving file. Received " + 
                            (totalBytes + bytesDownloaded) + " bytes.");
                } else {
                    System.out.println("  Failure to receive file: No bytes read");
                }
            }

            break;
        }

        closeConnections();
    }
    
    public void closeConnections() {
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

}
