import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ClientThread implements Runnable {
    private static final Set<ClientThread> CliThreads = Collections.synchronizedSet(new HashSet<>());
    private static final Logger LOGGER = Logger.getLogger(ClientThread.class.getName());

    private static int currentQuestionIndex = 1;
    private final Socket clientSocket;
    private final UDPThread udpThread;
    private PrintWriter out;
    private String receivedID;
    private DataOutputStream dos;
	private DataInputStream dis;
    private BufferedReader in;
    private final int clientId;
    private String correctAnswer;

    private String clientName;
    private final ServerGUI serverGUI;
    private boolean gameStarted = false;
    private final Object gameStartMonitor = new Object();

    public ClientThread(Socket socket, int clientId, UDPThread udpThread, ServerGUI serverGUI) {
        this.clientSocket = socket;
        this.udpThread = udpThread;
        this.clientId = clientId;
        this.serverGUI = serverGUI;
        try {
            dos = new DataOutputStream(clientSocket.getOutputStream());
			dis = new DataInputStream(socket.getInputStream());
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            out = new PrintWriter(clientSocket.getOutputStream(), true);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error setting up streams for client " + clientId, e);
        }
    }

    @Override
    public void run() {
        try {
            // Initialize client name and update server GUI.
            clientName = dis.readUTF();
            LOGGER.log(Level.INFO, "Client " + clientId + " connected as: " + clientName);
            javax.swing.SwingUtilities.invokeLater(() -> {
                serverGUI.updateClientList("Client " + clientId + " (" + clientName + ") connected");
            });

            dos.writeUTF("Wait, the game is about to start...");

            // Add this client thread to the set of client threads.
            synchronized (CliThreads) {
                CliThreads.add(this);
            }

            // Wait for the server to start the game.
            synchronized (gameStartMonitor) {
                while (!gameStarted) {
                    try {
                        gameStartMonitor.wait();
                    } catch (InterruptedException e) {
                        LOGGER.log(Level.SEVERE, "Thread interrupted: {0}", e.getMessage());
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            }

            // Send the current question to the client.
            sendCurrentQuestion();

            // Process client feedback.
            String feedback;
            while ((feedback = dis.readUTF()) != null) {
                synchronized (CliThreads) {
                    handleClientFeedback(feedback);
                }
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error in communication with client " + clientId, e);
        } finally {
            cleanupResources();
        }
    }

    private void cleanupResources() {
        // Close resources like DataOutputStream, DataInputStream, Socket, etc.
        try {
            if (dos != null) dos.close();
            if (dis != null) dis.close();
            if (clientSocket != null) clientSocket.close();
            if (out != null) out.close();
            if (in != null) in.close();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error closing resources for client " + clientId, e);
        }
    }

    private void handleClientFeedback(String feedback) throws IOException {
        LOGGER.log(Level.INFO, "Feedback from client {0}: {1}", new Object[]{clientId, feedback});
        if ("buzz".equals(feedback.trim())) {
            receivedID = dis.readUTF();
            handleBuzz();
        } else if ("Correct".equals(feedback.trim())) {
            handleCorrectAnswer();
        } else if ("Next".equals(feedback.trim())) {
            handleNextQuestion();
        }
        // Other cases as needed...
    }

    private void handleCorrectAnswer() throws IOException {
        // Logic for handling a correct answer from the client
        LOGGER.log(Level.INFO, "Client {0} answered correctly", clientId);
        // Send some form of acknowledgment or update to the client or game state
    }


// Other methods...


    private void handleNext() throws IOException {
        synchronized (ClientThread.class) {
            currentQuestionIndex++;
            findAnswer();
            for (ClientThread CliThread : CliThreads) {

                CliThread.sendCurrentQuestion();
            }
        }
    }

    private void handleNextQuestion() throws IOException {
        // Logic to proceed to the next question
        currentQuestionIndex++;
        findAnswer();
        sendCurrentQuestion();
    }

    private void findAnswer() {
        String questionFilePath = "Question" + currentQuestionIndex + ".txt";
        try (BufferedReader reader = new BufferedReader(new FileReader(questionFilePath))) {
			String line;
			while ((line = reader.readLine()) != null) {
				if (line.startsWith("Correct: ")) {
					correctAnswer = line.replace("Correct: ", "");
				}
			}
		} catch (IOException e) {
			System.err.println("Error reading the question file: " + e.getMessage());
		}
    }

    private void sendCurrentQuestion() throws IOException {
        String questionFilePath = "Question" + currentQuestionIndex + ".txt";
        byte[] fileContent = Files.readAllBytes(Paths.get(questionFilePath));
        System.out.println(fileContent.length);
        dos.writeUTF("Next Question");
        dos.writeInt(fileContent.length);
        dos.write(fileContent);
        dos.flush();
    }

    private void handleBuzz() throws IOException {
        String firstClientId = udpThread.firstInLine();
        if (receivedID.equals(firstClientId)) {
            System.out.println("sending ack");
            dos.writeUTF("ack");
            dos.flush();
        } else {
            dos.writeUTF("nack");
            dos.flush();
        }
    }
    public void startGame() {
        try {
            dos.writeUTF("Start Trivia");
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error sending start command to client " + clientId, e);
        }
    }
}