import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Set;

public class Peer3 {
	Socket requestSocket; // socket connect to the server
	ObjectOutputStream out; // stream write to the socket
	ObjectInputStream in; // stream read from the socket
	String msg; // msg send to the server
	String[] listOfServerFiles;
	String[] inputOperation;
	Set<Integer> chunksList = new HashSet<Integer>(); ;

	public static void main(String[] args) throws Exception {
		int fileOwnerPortNumber = Integer.parseInt(args[0]);
		int peerPortNumber = Integer.parseInt(args[1]);
		int neighborPortNumber = Integer.parseInt(args[2]);
		Peer3 p = new Peer3();
		p.execute(fileOwnerPortNumber, peerPortNumber , neighborPortNumber);
		System.out.println("SENT ALL CHUNKS TO PEER WITH PORT " + neighborPortNumber);
	}

	void execute(int fileOwnerPortNumber , int peerPortNumber , int neighborPortNumber) throws Exception {
		try {
			// create a socket to connect to the server
			requestSocket = new Socket("localhost", fileOwnerPortNumber);
			System.out.println("Connected to IP FILE OWNER in port " + fileOwnerPortNumber);

			out = new ObjectOutputStream(requestSocket.getOutputStream());
			out.flush();
			in = new ObjectInputStream(requestSocket.getInputStream());

			int numberOfChunks = (int) in.readObject();
			for(int i = 1; i <= numberOfChunks;i++) {
				chunksList.add(i);
			}
			int chunksAllotted = (int) in.readObject();
			System.out.println("Chunks alloted for this peer ["+chunksAllotted+"]");
			System.out.println(chunksAllotted);
			int bytesRead = 0;
			BufferedOutputStream bos = null;
			FileOutputStream fos = null;
			System.out.println("chunk id's allotted are :");
			while (chunksAllotted > 0) {
				// write each chunk of data into separate file with different number in name
				int chunkName = (int) in.readObject();
				System.out.print(chunkName+" ");
				chunksList.remove(chunkName);
				File file = new File("C:\\Users\\NIKHIL MALLADI\\Desktop\\CN1\\Peer3", Integer.toString(chunkName));
				byte[] a = new byte[100000];
				fos = new FileOutputStream(file);
				bos = new BufferedOutputStream(fos);

				in.read(a);
				bos.write(a);

				// System.out.println(bytesAmount);
				chunksAllotted--;
			}
			System.out.println();
			in.close();
			out.close();
			Thread thread = new Thread(new Runnable() {
				public void run() {
					try {
						ServerSocket receiveSocket = new ServerSocket(peerPortNumber);
						new Handler3(receiveSocket.accept()).start();
						System.out.println("Connection accepted for peer with port " + neighborPortNumber);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			});
			thread.start();

			boolean scanning = true;
			while (scanning) {
				try {
					int neighborPort = neighborPortNumber;
					requestSocket = new Socket("localhost", neighborPort);
					out = new ObjectOutputStream(requestSocket.getOutputStream());
					out.flush();
					in = new ObjectInputStream(requestSocket.getInputStream());
					System.out.println("Connection successfully established!");
					scanning = false;
				} catch (Exception e) {
					System.out.println("Trying to connect...");
					try {
						Thread.sleep(2000);
					} catch (InterruptedException ie) {
						ie.printStackTrace();
					}
				}
			}

			while (true) {
				System.out.println("Peer3 receiving chunks");
				if (!chunksList.isEmpty()) {
					Integer[] a = chunksList.toArray(new Integer[chunksList.size()]);
					for (int i = 0; i < a.length; i++) {
						System.out.println("File to get is " + a[i]);
						int x = a[i];
						out.writeObject(x);
						out.flush();
						boolean status = (boolean) in.readObject();
						if (status) {
							File file = new File("C:\\Users\\NIKHIL MALLADI\\Desktop\\CN1\\Peer3",
									Integer.toString(a[i]));
							byte[] bytes = new byte[100000];

							fos = new FileOutputStream(file);
							bos = new BufferedOutputStream(fos);
							in.read(bytes);
							chunksList.remove(a[i]);
							bos.write(bytes);
							Thread.sleep(2000);
						}
					}
				} else {
					mergeFiles();
					break;
				}
			}
		} catch (Exception e) {
		}
	}

	public static void mergeFiles() throws IOException {
		String directoryPath = "C:\\Users\\NIKHIL MALLADI\\Desktop\\CN1\\Peer3";
		byte[] chunk = new byte[100000]; // this buffer size could be
											// anything
		File[] files = new File(directoryPath).listFiles();
		new File(directoryPath + "/out/").mkdirs();
		try {
			FileOutputStream fileOutStream = new FileOutputStream(
					new File(directoryPath + "/out/" + "test.pdf"));
			BufferedOutputStream bufferOutStream = new BufferedOutputStream(fileOutStream);
			for (File f : files) {
				FileInputStream fileInStream = new FileInputStream(f);
				BufferedInputStream bufferInStream = new BufferedInputStream(fileInStream);
				int bytesRead = 0;
				while ((bytesRead = bufferInStream.read(chunk)) > 0) {
					bufferOutStream.write(chunk, 0, bytesRead);
				}
				fileInStream.close();
			}
			fileOutStream.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}

class Handler3 extends Thread {
	private Socket connection;
	ObjectOutputStream out; // stream write to the socket
	String chunkLoc;
	ObjectInputStream in;

	public Handler3(Socket connection) throws IOException {
		this.connection = connection;
		this.chunkLoc = "C:\\Users\\NIKHIL MALLADI\\Desktop\\CN1\\Peer3";
	}

	public void run() {
		try {
			System.out.println("Peer3 sending file chunks");
			Thread.sleep(3000);


			out = new ObjectOutputStream(connection.getOutputStream());
			out.flush();
			in = new ObjectInputStream(connection.getInputStream());

			while (true) {
				File[] files = new File(chunkLoc).listFiles();
				boolean doesFileExist = false;
				File currentFile = null;
				String s;
				BufferedInputStream bis = null;
				FileInputStream fis = null;
				int chunkId = (int) in.readObject();
				for (int i = 0; i < files.length; i++) {
					System.out.println(chunkId);
					s = files[i].getName();
					if (chunkId == Integer.parseInt(s)) {
						doesFileExist = true;
						currentFile = files[i];
						break;
					}
				}
				out.writeObject(doesFileExist);
				System.out.println(doesFileExist);
				if (doesFileExist) {
					byte[] chunk = new byte[100000];
					fis = new FileInputStream(currentFile);
					bis = new BufferedInputStream(fis);
					out.write(bis.read(chunk));
					out.flush();
				}
			}

		} catch (Exception e) {

		}
	}
}
