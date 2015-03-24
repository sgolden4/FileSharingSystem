import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;


public class FileDistributor implements Runnable {
	private static final String SERVER_ADDRESS = "127.0.0.1";
	String filename, filepath;
	int serverport;
	Socket server;
	
	int buffersize;
	byte[] packet;
	OutputStream output;
	BufferedReader br;
	PrintWriter pw;
	
	
	FileDistributor(String filepath, String filename, int serverport){
		this.filename = filename;
		this.filepath = filepath;
		this.serverport = serverport;
	}

	@Override
	public void run() {
		File file = new File(filepath + filename);
		if(!file.exists()){
			System.out.println("Error: told to upload file to other servers, but file does not exist.");
			return;
		} else {
			long length = file.length();
			try {
				BufferedInputStream instream = new BufferedInputStream(new FileInputStream(file));
				if(!openConnection()){
					System.out.println("error opening connection, distribution halted.");
					instream.close();
					closeConnections();
					return;
				}
				System.out.println("Attempting to send "+filename+" to server "+server.getPort()
						+".  Length = "+length);
				pw.println("ulserve "+filename+" "+length+" resume");
    			String instring = br.readLine();
    			long startpoint = Long.parseLong(instring);
    			instream.skip(startpoint);
    			instring = br.readLine();
    			System.out.println("Response received: "+instring);
    			if(!"ready".equals(instring)){
    				System.out.println("server didn't respond ready");
    				instream.close();
    				closeConnections();
    				return;
    			}
				long remaining = length-startpoint;
				while (remaining > 0) {
                    if (remaining < buffersize) {
                        packet = new byte[(int) remaining];
                    }
                    int bytesread = instream.read(packet);
                    output.write(packet);
                	output.flush();
                    remaining -= bytesread;
                    System.out.println("sent "+bytesread+" bytes to server "+server.getPort());
                    if(remaining == 0){
                    	instring = br.readLine();
            			String[] instringparts = instring.split(" ");
            			System.out.println("message from server: "+instring);
            			if("received".equals(instringparts[0])){
            				long resumepoint = Long.parseUnsignedLong(instringparts[1]);
            				remaining = length - resumepoint;
            				closeConnections();
            				instream.close();
            				instream = new BufferedInputStream(new FileInputStream(file));
            				instream.skip(remaining);
            				openConnection();
            				pw.println("ulserve "+filename+" "+length+" resume");
            				instring = br.readLine(); //response with length
            				instring = br.readLine(); //response with ready
            				if(!"ready".equals(instring)){
                				System.out.println("server didn't respond ready");
                				instream.close();
                				closeConnections();
                				return;
            				}
            					
            			}
                    }
                }
				instream.close();
				closeConnections();
				System.out.println("File successfully sent to server.");
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
		}
	}

	private void closeConnections() throws IOException{
		if(output != null) output.close();
		if(server != null) server.close();
	}
	
	private boolean openConnection(){

		try {
			this.server = new Socket(SERVER_ADDRESS, serverport);
			buffersize = server.getReceiveBufferSize();
			packet = new byte[buffersize];
			output = server.getOutputStream();
			br = new BufferedReader(new InputStreamReader(server.getInputStream()));
			pw = new PrintWriter(output, true);
			return true;
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;

	}

}
