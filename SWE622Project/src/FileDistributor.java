import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;


public class FileDistributor implements Runnable {
	private static final String SERVER_ADDRESS = "127.0.0.1";
	String filename, filepath;
	Socket server;
	
	
	FileDistributor(String filepath, String filename, int serverport){
		try {
			this.server = new Socket(SERVER_ADDRESS, serverport);
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		};
		this.filename = filename;
		this.filepath = filepath;
	}

	@Override
	public void run() {
		File file = new File(filepath + filename);
		if(!file.exists()){
			System.out.println("Error: told to upload file to other servers, but file does not exist.");
			return;
		} else {
			long length = file.length();
			System.out.println("Attempting to send "+filename+" to server "+server.getPort()
					+".  Length = "+length);
			try {
				int buffersize = server.getReceiveBufferSize();
				byte[] packet = new byte[buffersize];
				BufferedInputStream instream = new BufferedInputStream(new FileInputStream(file));
				OutputStream output = server.getOutputStream();
				PrintWriter pw = new PrintWriter(output, true);
				pw.println("ulserve "+filename+" "+length);
				long remaining = length;
				while (remaining > 0) {
                    if (remaining < buffersize) {
                        packet = new byte[(int) remaining];
                    }
                    int bytesread = instream.read(packet);
                    output.write(packet);
                    remaining -= bytesread;
                    System.out.println("sent "+bytesread+" bytes to server "+server.getPort());
                }
				instream.close();
				output.close();
				System.out.println("File successfully sent to server.");
				server.close();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
		}
	}

}
