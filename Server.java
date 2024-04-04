import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Server {

    private static final Logger LOGGER = Logger.getLogger(Server.class.getName());
    private static final AtomicInteger clientIdGenerator = new AtomicInteger(0);
    private static final ConcurrentHashMap<Integer, ClientThread> clientThreads = new ConcurrentHashMap<>();
    private static ServerGUI serverGUI;
    public static UDPThread udpThread;

    private volatile boolean running = true;

    public void startTrivia() {
        for (ClientThread clientThread : clientThreads.values()) {
            clientThread.startGame();
        }
    }

    public void runServer(int port) {
        try {
            udpThread = new UDPThread(port); // Assuming UDPThread is properly implemented elsewhere
            new Thread(udpThread).start();
            LOGGER.log(Level.INFO, "UDP Thread started on port {0}", port);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Could not start UDP thread on port {0}", port);
            return;
        }

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            LOGGER.log(Level.INFO, "Server listening on port {0}", port);
            while (true) { // Changed to true to keep server running indefinitely
                try {
                    Socket clientSocket = serverSocket.accept();
                    int clientId = clientIdGenerator.incrementAndGet();
                    ClientThread clientThread = new ClientThread(clientSocket, clientId, udpThread, serverGUI);
                    clientThreads.put(clientId, clientThread);
                    new Thread(clientThread).start();
                } catch (IOException e) {
                    LOGGER.log(Level.SEVERE, "Exception caught when trying to accept a new connection: {0}", e.getMessage());
                    if (!running) {
                        break; // Break out of the loop if the server is stopping
                    }
                }
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Could not listen on port {0}: {1}", new Object[]{port, e.getMessage()});
        }
    }

    public void stopServer() {
        running = false;
        // Additional cleanup if necessary
    }

    public static void main(String[] args) {
        final int port = 1244;
        Server serverInstance = new Server();
        javax.swing.SwingUtilities.invokeLater(() -> {
            serverGUI = new ServerGUI(serverInstance);
            serverGUI.display();
        });

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            serverInstance.stopServer();
            LOGGER.log(Level.INFO, "Server is shutting down...");
            // Additional cleanup if necessary
        }));

        serverInstance.runServer(port); // Run the server loop with the specified port
    }
}
