import javax.swing.*;
import java.awt.*;

public class ServerGUI {

    private final JFrame frame;
    private final JTextArea clientsArea;
    public Server server;

    public ServerGUI(Server server) {
        this.server = server;
        frame = new JFrame("Server GUI");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(new Dimension(400, 400));

        JButton startTriviaButton = new JButton("Start Trivia");
        startTriviaButton.addActionListener(e -> server.startTrivia());
        frame.getContentPane().add(startTriviaButton, BorderLayout.SOUTH);

        clientsArea = new JTextArea();
        clientsArea.setEditable(false);
        clientsArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane scrollPane = new JScrollPane(clientsArea);
        frame.getContentPane().add(scrollPane, BorderLayout.CENTER);

        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    public void updateClientList(String clientInfo) {
        SwingUtilities.invokeLater(() -> clientsArea.append(clientInfo + "\n"));
    }

    public void display() {
        frame.setVisible(true);
    }

}
