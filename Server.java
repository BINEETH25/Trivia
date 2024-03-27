import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

public class Server extends JFrame {
    private JTextArea textArea;
    private ServerSocket serverSocket;
    private List<PrintWriter> clientWriters = new ArrayList<>();
    private JButton broadcastButton;
    private static final String BROADCAST_MESSAGE = "Hello from server!"; // Default message to send

    public Server() {
        super("Server");
        textArea = new JTextArea();
        textArea.setEditable(false);
        broadcastButton = new JButton("Broadcast Message");

        // Action listener for the button
        broadcastButton.addActionListener((ActionEvent e) -> broadcastMessage(BROADCAST_MESSAGE));

        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.add(new JScrollPane(textArea), BorderLayout.CENTER);
        panel.add(broadcastButton, BorderLayout.SOUTH);

        add(panel);
        setSize(500, 300);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);

        startServer();
    }

    private void startServer() {
        try {
            serverSocket = new ServerSocket(1234);
            while (true) {
                Socket socket = serverSocket.accept();
                PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
                synchronized (clientWriters) {
                    clientWriters.add(writer);
                }
                new Thread(new ClientHandler(socket, writer)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void broadcastMessage(String message) {
        synchronized (clientWriters) {
            for (PrintWriter writer : clientWriters) {
                writer.println(message);
            }
        }
    }

    private class ClientHandler implements Runnable {
        private Socket socket;
        private PrintWriter writer;

        public ClientHandler(Socket socket, PrintWriter writer) {
            this.socket = socket;
            this.writer = writer;
        }

        @Override
        public void run() {
            try {

                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                String clientMessage;
                while ((clientMessage = reader.readLine()) != null) {
                    final String messageForSwing = clientMessage; // Create a final variable for the lambda expression
                    SwingUtilities.invokeLater(() -> textArea.append(messageForSwing + "\n"));
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                synchronized (clientWriters) {
                    clientWriters.remove(writer);
                }
                try {
                    socket.close();
                } catch (IOException e) {
                    // Handle exception
                }
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Server());
    }
}
