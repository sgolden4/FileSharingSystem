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
import java.net.SocketTimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class SWE622Server implements Runnable {
    public final static int PACKET_SIZE = 10000000;

	private static final int TIMEOUT = 1000;
	
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
			connection.setSoTimeout(TIMEOUT);
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
				case "ul": receiveFile(instringparts, true);
					break;
				case "ulserve": receiveFile(instringparts, false);
					break;
				case "verify": verify();
					break;
				case "server": FileServerMain.addServer(Integer.parseUnsignedInt(instringparts[1]));
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

	private void receiveFile(String[] instringparts, boolean distribute) {
		
		BufferedOutputStream tofile = null;
		boolean resuming = false;
		
		if(instringparts.length < 3){
			pw.println("  Failure : Need more information");
			return;
		}
		
		String filename = instringparts[1];
		if(!filenameCheck(filename)){
			pw.println(" Failure: invalid filename");
			System.out.println("Client attempted to send invalid filename: "+filename);
			return;
		}
		long filelength = Long.parseLong(instringparts[2]);
		
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
					myfilelength = 0;
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
		

		long totalbytes = myfilelength;
		int bytesread = 0;
		try {
			pw.println("ready");
			System.out.println("Receiving file "+filename);
			InputStream instream = connection.getInputStream();
			tofile = new BufferedOutputStream(new FileOutputStream(infile, resuming));
			int buffersize = connection.getReceiveBufferSize();
			byte[] packet = new byte[buffersize];
			while(totalbytes < filelength){
				//System.out.println("about to read from instream, buffersize = "+buffersize);
				bytesread = instream.read(packet, 0, buffersize);
				//System.out.println("just got "+bytesread+" bytes from instream.");
				if (bytesread != -1) {
                    totalbytes += bytesread;
                    tofile.write(packet, 0 , bytesread);
                    System.out.println("File "+filename+": "+totalbytes
                    		+" bytes received out of "+filelength+".");
                } else {
                	if(totalbytes < filelength){
                		System.out.println("failed to receive file, only received "
                				+totalbytes+" of "+filelength);
	                	distribute = false;
	                	pw.println("received "+totalbytes);
	                	break;
                	}
                }
			}
			if(totalbytes == filelength)
				pw.println("success");
			if(instream != null) instream.close();
            if(tofile != null){ 
                tofile.flush();
                tofile.close();
            }
            System.out.println("File "+filename+" has finished with "+totalbytes
            		+" bytes received out of "+filelength+".");
            if(distribute)
            	FileServerMain.onUploadComplete(filepath, filename);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SocketTimeoutException e){
        	if(totalbytes < filelength){
        		System.out.println("failed to receive file, only received "
        				+totalbytes+" of "+filelength);
            	distribute = false;
            	pw.println("received "+totalbytes);
        	}
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
		if(!filenameCheck(filename)){
			pw.println(" Failure: invalid filename");
			System.out.println("Client attempted to request invalid filename: "+filename);
			return;
		}
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
	
	public boolean filenameCheck(String filename){
		Pattern pattern = Pattern.compile("^([a-zA-Z][a-zA-Z0-9_-]*.?)+[a-zA-Z0-9]+$");
		Matcher matcher = pattern.matcher(filename);
		return matcher.find();
	}

}
