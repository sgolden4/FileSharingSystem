import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
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
			String[] instringparts = instring.split(" ");
			switch(instringparts[0]){
				case "dl": sendFile(instringparts);
					break;
				case "ul": receiveFile(instringparts);
					break;
				case "verify": verify();
					break;
				
			}
			
		} catch (IOException e) {

		}
		
	}

	private void verify() {
		pw.println("42");
	}

	private void receiveFile(String[] instringparts) {
		/* TODO: 1. check string for whether we are resuming or not
				 2. check if file exists already
				 3. create file if it doesn't exist and we are not resuming, 
				 		open file if it does exist and we are resuming
				 4. send "receiving\n" if everything is good, otherwise send "failure\n" and return. 
				 5. get data from client
				 6. if file finished, call FileServerMain.onUploadComplete(filename) so it can
				  	start sending the file to the other servers.
		*/
		
		BufferedOutputStream tofile = null;
		byte[] packet = new byte[PACKET_SIZE + 1];
		boolean resuming = false;
		
		if(instringparts.length < 3){
			pw.println("failure : need more information");
			return;
		}
		
		String filename = instringparts[1];
		int filelength = Integer.parseUnsignedInt(instringparts[2]);
		
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
				} catch (IOException e) {
					pw.println("error creating file");
					return;
				}
			}
				
		} else {
			try {
				infile.createNewFile();
			} catch (IOException e) {
				pw.println("error creating file");
				return;
			}
		}
		

		
		try {
			InputStream instream = connection.getInputStream();
			tofile = new BufferedOutputStream(new FileOutputStream(infile, resuming));
			int totalbytes = (int) myfilelength;
			int bytesread = 0;
			while(totalbytes < filelength){
				bytesread = instream.read(packet, 0, PACKET_SIZE);
				if (bytesread != -1) {
                    totalbytes += bytesread;
                    tofile.write(packet, 0 , bytesread);
                } 
                tofile.flush();
			}
			if(instream != null) instream.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
        //Close connections
        try {
            if(pw != null)  pw.close();
            if(input != null)  input.close();
            if(output != null)  output.close();
            if(tofile != null)  tofile.close();
            if(connection != null)  connection.close();
        } catch (IOException e) {
            System.out.println("Unable to close connections");
            return;
        }
	}

	private void sendFile(String[] instringparts) {
		/* TODO 1. check string for whether we are resuming or not
				2. check if file exists, reply "sending\n" if it does, reply "failure\n" 
					if it does not and return
				3. open file
				4. send file
		*/
	}

}
