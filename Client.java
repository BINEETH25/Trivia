import java.io.BufferedReader;
import java.io.Console;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException; // Import for UnknownHostException

public class Client {
    public static void main(String[] args) {
        final String serverIP = "127.0.0.1"; // Server IP address
        final int serverPort = 1234; // Server port number
        final String clientID = "Client4"; // Example client ID, this should be unique for each client

        // Correction here: Moved the try-with-resources closing parenthesis to include
        // the Socket
        try (Socket socket = new Socket(serverIP, serverPort);
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            System.out.println(clientID + " is connected to the server at " + serverIP + ":" + serverPort);

            // Send ClientID to the server
            out.println(clientID);

            Console console = System.console();
            String text;

            do {
                text = console.readLine("> ");

                out.println(text); // Send text to the server

                InputStream input = socket.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(input));

                String response = reader.readLine(); // Read the response from the server
                System.out.println(response); // Display the response

            } while (!text.equals("bye"));

            // The socket will be closed automatically by try-with-resources
        } catch (UnknownHostException ex) {
            System.out.println("Server not found: " + ex.getMessage());
        } catch (IOException ex) {
            System.out.println("I/O error: " + ex.getMessage());
        }
    }
}
