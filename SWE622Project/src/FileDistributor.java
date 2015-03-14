import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;


public class FileDistributor implements Runnable {
    public final static int PACKET_SIZE = 10000000;
	String filename;
	Socket server;
	
	
	FileDistributor(String filepath, Socket server){
		this.server = server;
		filename = filepath;
	}

	@Override
	public void run() {
		//TODO: send file to server
		File file = new File(filename);
		if(!file.exists()){
			System.out.println("Error: told to upload file to other servers, but file does not exist.");
			return;
		} else {
			long length = file.length();
			byte[] packet = new byte[PACKET_SIZE];
			try {
				BufferedInputStream instream = new BufferedInputStream(new FileInputStream(file));
				OutputStream output = server.getOutputStream();
				long remaining = length;
				while (remaining > 0) {
                    if (remaining < PACKET_SIZE) {
                        packet = new byte[(int) remaining];
                    }
                    int bytesread = instream.read(packet);
                    output.write(packet);
                    remaining -= bytesread;
                }
				instream.close();
				output.close();
			} catch (FileNotFoundException e) {

			} catch (IOException e) {

			}
			
		}
	}

}
