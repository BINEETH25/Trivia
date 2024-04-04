import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import javax.swing.JOptionPane;
import javax.swing.*;
import java.io.*;


public class Client {
	private Socket socket;
	public String CLIENT_ID;
	public String clientId;
	private final String serverAddress;
	private final int serverPort;
	private ClientWindow clientWindow;
	public PrintWriter out;
	private static final int UDP_port = 4445;
	//public boolean read = true;
	public DataInputStream dis;
	private DataOutputStream dos;
	public static String usersIP = "127.0.0.1";

	private String clientName;

	public Client(String serverAddress, int serverPort) {
		this.serverAddress = serverAddress;
		this.serverPort = serverPort;
		promptAndSendName();
	}

	private void promptAndSendName() {
		String name = JOptionPane.showInputDialog(null, "Enter your name:", "Client Name", JOptionPane.PLAIN_MESSAGE);
		if (name != null && !name.trim().isEmpty()) {
			connectToServer(name);
		} else {
			JOptionPane.showMessageDialog(null, "You must enter a name.", "Error", JOptionPane.ERROR_MESSAGE);
			System.exit(1);
		}
	}

	public void connectToServer(String name) {
		try {
			socket = new Socket(serverAddress, serverPort);
			dos = new DataOutputStream(socket.getOutputStream());
			out = new PrintWriter(socket.getOutputStream(), true);
			dis = new DataInputStream(socket.getInputStream());
			dos.writeUTF(name); // Send the name to the server
			clientId = dis.readUTF(); // Read client ID assigned by server
			listenForServerMessages();
		} catch (IOException e) {
			JOptionPane.showMessageDialog(null, "Could not connect to the server: " + e.getMessage(), "Connection Error", JOptionPane.ERROR_MESSAGE);
			System.exit(1); // Exit the application as we cannot proceed without a server connection
		}
	}



	private void waitForStart() {
		try {
			while (true) {
				String serverCommand = dis.readUTF();
				if ("Start Trivia".equals(serverCommand)) {
					SwingUtilities.invokeLater(() -> {
						clientWindow = new ClientWindow(this);
						clientWindow.display();
						clientWindow.displayMessage("Game is about to begin.");// Ensure you have a method in ClientWindow to make the window visible
					});
					listenForServerMessages();
					break;
				}
			}
		} catch (IOException e) {
			JOptionPane.showMessageDialog(null, "Lost connection to the server: " + e.getMessage(), "Connection Error", JOptionPane.ERROR_MESSAGE);
			System.exit(1); // Exit the application
		}
		listenForServerMessages();
	}

	private void listenForServerMessages() {
	    new Thread(() -> {
	        try {
	            while (!socket.isClosed()) {
	                String response = dis.readUTF();
	                System.out.println("Response from server: " + response);
	                switch (response) {
						case "Wait, the game is about to start...":
							// Display this message in the client's GUI
							if (clientWindow != null) {
								clientWindow.displayMessage("Wait, the game is about to start...");
							}
							break;
						case "Start Trivia":
							SwingUtilities.invokeLater(() -> {
								clientWindow = new ClientWindow(this);
								clientWindow.display();
							});
							if (clientWindow != null) {
								clientWindow.startGame();
							}
							break;
	                    case "ack":
	                        clientWindow.enableOptions();
							clientWindow.resetTimer(10);
	                        System.out.println("I was first!");
	                        break;
	                    case "nack":
	                        System.out.println("Not first.");
	                        break;
	                    case "Next Question":
	                    	System.out.println("Curr Question is happening");
							int fileLength = dis.readInt();
							System.out.println("file length " + fileLength);
							if (fileLength > 0) {
								System.out.println("Curr Question is happening");
								byte[] content = new byte[fileLength];
								dis.readFully(content, 0, fileLength);
								String fileName = "clientQuestion.txt";
								saveToFile(fileName, content);
								displayQuestionFromFile(fileName);
								clientWindow.disableOptions();

							}
							break;
	                }
	            }
	        } catch (IOException e) {
	            System.err.println("Error reading from server: " + e.getMessage());
	        }
	    }).start();
	}

	private static void saveToFile(String fileName, byte[] content) throws IOException {
		try (FileOutputStream fos = new FileOutputStream(fileName)) {
			fos.write(content);
		}
	}

	private void displayQuestionFromFile(String fileName) {
		try (BufferedReader reader = new BufferedReader(new FileReader(fileName))) {
			StringBuilder questionBuilder = new StringBuilder();
			ArrayList<String> options = new ArrayList<>();
			String correctAnswer = "";

			String line;
			while ((line = reader.readLine()) != null) {
				if (line.startsWith("Correct: ")) {
					correctAnswer = line.replace("Correct: ", "");
				} else if (!line.trim().isEmpty()) {
					if (questionBuilder.isEmpty()) {
						questionBuilder.append(line);
					} else {
						options.add(line);
					}
				}
			}
			this.clientWindow.updateQuestion(questionBuilder.toString());
			this.clientWindow.setOptions(options.toArray(new String[0]), correctAnswer);

		} catch (IOException e) {
			System.err.println("Error reading the question file: " + e.getMessage());
		}
	}

	public void sendAnswerFeedback(String feedback) {
		System.out.println(feedback);
		if (dos != null) {
			try {
				dos.writeUTF(feedback);
				dos.flush();
			} catch (IOException e) {
				System.err.println("Error sending feedback: " + e.getMessage());
			}
		} else {
			System.out.println("DataOutputStream 'dos' is not initialized.");
		}
	}

	public void sendBuzz() {
		try (DatagramSocket socket = new DatagramSocket()) {
			String message = String.valueOf(CLIENT_ID);
			byte[] messageBytes = message.getBytes();
			InetAddress serverAddress = InetAddress.getByName(usersIP);
			DatagramPacket packet = new DatagramPacket(messageBytes, messageBytes.length, serverAddress, UDP_port);
			socket.send(packet);
			sendBuzzToServer();
		} catch (IOException e) {
			System.err.println("An error occurred: " + e.getMessage());
		}
	}

	public void sendBuzzToServer() {
		if (dos != null) {
			try {
				dos.writeUTF("buzz");
				dos.writeUTF(CLIENT_ID);
			} catch (Exception e) {
				System.out.println("An error occurred: " + e.getMessage());
			}
		} else {
			System.out.println("DataOutputStream 'dos' is not yet initialized or socket is closed.");
		}
	}

	public static void main(String[] args) {
		String serverAddress = "127.0.0.1"; // Example server address
		int serverPort = 1244;
		Client client = new Client(serverAddress, serverPort);
		client.connectToServer(client.clientName);
	}
}