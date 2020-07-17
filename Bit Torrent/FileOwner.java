import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class FileOwner {

	private static int sPort; // The server will be listening on this port number
	static Map<Integer, ArrayList<Integer>> clientMap;

	public static void main(String[] args) throws Exception {
		int clientid = 1;
		System.out.println("File Owner running...");
		ServerSocket listener = new ServerSocket(8000);
		int numberOfClients = 5;
		try {
			
			File fileName = new File("test1.pdf");
			File outputFile = new File("chunks/output.txt");
			
			System.out.println("Splitting the given file into chunks");
			
			splitFile(fileName);
			
			String chunksLocation = "/chunks";
			File[] files = new File(chunksLocation).listFiles();

			int chunkCount = files.length;
			System.out.println("No. of Chunks:" + chunkCount);

			clientMap = new HashMap<Integer, ArrayList<Integer>>();
			for (int i = 1; i <= numberOfClients; i++) {
				ArrayList<Integer> arr = new ArrayList<Integer>();
				for (int j = i; j <= chunkCount; j += numberOfClients) {
					arr.add(j);
				}
				clientMap.put(i, arr);
			}						
			while (true) {
				if (clientid <= numberOfClients) {
					new Handler(listener.accept(), clientid, files).start();
					System.out.println("Connected to Client No. :" + clientid);
					clientid++;
				} else {					
					//break;
				}
			}

		} finally {
			listener.close();
		}

	}

	public static List<File> splitFile(File file) throws IOException {
		int counter = 1;
		List<File> files = new ArrayList<File>();
		int sizeOfChunk = 100 * 1000;
		byte[] buffer = new byte[sizeOfChunk];
		try (BufferedReader br = new BufferedReader(new FileReader(file))) {
			String name = file.getName();
			File newFile = null;
			try (FileInputStream fis = new FileInputStream(file);
					BufferedInputStream bis = new BufferedInputStream(fis)) {
				int bytesAmount = 0;
				while ((bytesAmount = bis.read(buffer)) > 0) {
					newFile = new File("/chunks", Integer.toString(counter++));
					try (FileOutputStream out1 = new FileOutputStream(newFile)) {
						out1.write(buffer, 0, bytesAmount);
						out1.flush();
					}
					System.out.println(bytesAmount);
				}
			}
			files.add(newFile);
		}
		return files;
	}
}

class Handler extends Thread {
	private Socket connection;
	private ObjectOutputStream out; // stream write to the socket
	private int id; // The index number of the client
	private File[] files;

	public Handler(Socket connection, int id, File[] files) {
		this.connection = connection;
		this.id = id;
		this.files = files;
	}

	public void run() {
		try {
			out = new ObjectOutputStream(connection.getOutputStream());
			List<Integer> chunksAllotted = FileOwner.clientMap.get(id);
			out.writeObject(files.length);
			out.writeObject(chunksAllotted.size());
			BufferedInputStream bis = null;
			FileInputStream fis = null;
			for (int i = 0; i < chunksAllotted.size(); i++) {
				int x = chunksAllotted.get(i);
				out.writeObject(x);
				fis = new FileInputStream(files[i]);
				byte[] chunk = new byte[100000];
				bis = new BufferedInputStream(fis);
				out.write(bis.read(chunk));				
				out.flush();
				Thread.sleep(1000);
			}
			bis.close();
			fis.close();
		}
		catch (IOException e) {
			e.printStackTrace();
		} catch (Exception ioException) {
			System.out.println("Disconnected with Client no. " + id);
		} finally {
			try {
				out.close();
				connection.close();
			} catch (IOException ioException) {
				System.out.println("Disconnected with Client no. " + id);
			}
		}
	}
}
