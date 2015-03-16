import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;


public class SWE622Server implements Runnable {
    public final static int PACKET_SIZE = 10000000;
	
	Socket connection;
	ServerSocket mysocket;
	BufferedReader input;
	OutputStream output;
	PrintWriter pw;
	String filepath;
	
	SWE622Server(ServerSocket mysocket, Socket connection, String filepath){
		this.connection = connection;
		this.mysocket = mysocket;
		this.filepath = filepath;
	}

	public void run() {
		try {
			input = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			output = connection.getOutputStream();
			pw = new PrintWriter(output, true);
			String instring = input.readLine();
			if(instring == null){
				closeConnections();
				return;
			}
			String[] instringparts = instring.split(" ");
			switch(instringparts[0]){
				case "dl": sendFile(instringparts);
					break;
				case "ul": receiveFile(instringparts);
					break;
				case "verify": verify();
					break;
				case "server": FileServerMain.addServer(connection);
			}
			
		} catch (IOException e) {

		}
        //Close connections
		closeConnections();
	}
	
	private void closeConnections(){
        try {
            if(pw != null)  pw.close();
            if(input != null)  input.close();
            if(output != null)  output.close();
            if(connection != null)  connection.close();
        } catch (IOException e) {
            System.out.println("Unable to close connections");
            return;
        }		
	}

	private void verify() {
		pw.println("42");
	}

	private void receiveFile(String[] instringparts) {
		
		BufferedOutputStream tofile = null;
		boolean resuming = false;
		
		if(instringparts.length < 3){
			pw.println("  Failure : Need more information");
			return;
		}
		
		String filename = instringparts[1];
		Long filelength = Long.parseLong(instringparts[2]);
		
		//TODO: for security, check/sanitize filename

		if(instringparts.length > 3 && "resume".equals(instringparts[3])){
			resuming = true;
		}
		
		File infile = new File(filepath+filename);
		long myfilelength = 0;
		if(infile.exists()){
			myfilelength = infile.length();
			if(resuming)
				pw.println(myfilelength);
			else{
				try {
					infile.delete();
					infile.createNewFile();
					System.out.println("File "+filename+" already exists, overwriting.");
				} catch (IOException e) {
					pw.println("  Error creating file");
					System.out.println("error creating file "+filename);
					return;
				}
			}
				
		} else {
			try {
				System.out.println("File "+filename+" does not exist, creating.");
				infile.createNewFile();
			} catch (IOException e) {
				pw.println("  Error creating file");
	            return;
			}
		}
		

		
		try {
			System.out.println("Receiving file "+filename);
			InputStream instream = connection.getInputStream();
			tofile = new BufferedOutputStream(new FileOutputStream(infile, resuming));
			long totalbytes = myfilelength;
			int bytesread = 0;
			int buffersize = connection.getReceiveBufferSize();
			byte[] packet = new byte[buffersize];
			while(totalbytes < filelength){
				bytesread = instream.read(packet, 0, buffersize);
				if (bytesread != -1) {
                    totalbytes += bytesread;
                    tofile.write(packet, 0 , bytesread);
                    System.out.println("File "+filename+": "+totalbytes
                    		+" bytes received out of "+filelength+".");
                } 
			}
			if(instream != null) instream.close();
            if(tofile != null){ 
                tofile.flush();
                tofile.close();
            }
            System.out.println("File "+filename+" has finished with "+totalbytes
            		+" bytes received out of "+filelength+".");
            FileServerMain.onUploadComplete(filepath+filename);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	private void sendFile(String[] instringparts) {

		if(instringparts.length < 2){
			pw.println("  Failure : Need more information");
			return;
		}
		
		String filename = instringparts[1];
		//boolean resuming = false;
		long startposition = 0;
		if(instringparts.length > 3 && "resume".equals(instringparts[2])){
			startposition = Long.parseLong(instringparts[3]);
		}
		
		File file = new File(filepath + filename);
		if(!file.exists()){
			pw.println("  Failure : File does not exist");
			return;
		} else {
			pw.println("sending");
			long length = file.length() - startposition;
			pw.println(length);
			try {
				int buffersize = connection.getReceiveBufferSize();
				byte[] packet = new byte[buffersize];
				BufferedInputStream instream = new BufferedInputStream(new FileInputStream(file));
				instream.skip(startposition);
				long remaining = length;
				while (remaining > 0) {
                    if (remaining < buffersize) {
                        packet = new byte[(int) remaining];
                    }
                    int bytesread = instream.read(packet);
                    output.write(packet);
                    remaining -= bytesread;
                }
				instream.close();
			} catch (FileNotFoundException e) {

			} catch (IOException e) {

			}
			
		}
		
	}

}
