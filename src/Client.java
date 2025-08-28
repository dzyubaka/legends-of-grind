import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class Client {
    private static final int TILE_SIZE = 64;
    private static final Tile[][] tilemap = new Tile[100][100];
    private static BufferedImage image;

    public static void main(String[] args) throws IOException {
        image = ImageIO.read(Client.class.getResourceAsStream("grass.png"));
        openLogInFrame();
//        openMainFrame();
    }

    private static void openLogInFrame() {
        JFrame logInFrame = new JFrame("Legends of Grind");
        logInFrame.setLocationRelativeTo(null);
        logInFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        logInFrame.setLayout(new GridLayout(4, 4));
        logInFrame.add(new JLabel("Login"));
        JTextField registerLogin = new JTextField();
        logInFrame.add(registerLogin);
        logInFrame.add(new JLabel("Login"));
        JTextField logInLogin = new JTextField();
        logInFrame.add(logInLogin);
        logInFrame.add(new JLabel("Password"));
        JPasswordField registerPassword = new JPasswordField();
        logInFrame.add(registerPassword);
        logInFrame.add(new JLabel("Password"));
        JPasswordField logInPassword = new JPasswordField();
        logInFrame.add(logInPassword);
        logInFrame.add(new JLabel("Nickname"));
        JTextField nickname = new JTextField();
        logInFrame.add(nickname);
        logInFrame.add(new Container());
        JButton logInButton = new JButton("Log In");
        logInButton.addActionListener(_ -> {
            try {
                Socket socket = new Socket("localhost", 52);
                OutputStream outputStream = socket.getOutputStream();
                outputStream.write("login".getBytes());
                outputStream.write('\n');
                outputStream.write(logInLogin.getText().getBytes());
                outputStream.write('\n');
                outputStream.write(logInPassword.getText().getBytes());
                outputStream.write('\n');
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        logInFrame.add(logInButton);
        logInFrame.add(new Container());
        JButton registerButton = new JButton("Register");
        registerButton.addActionListener(_ -> {
            try {
                Socket socket = new Socket("localhost", 52);
                InputStream inputStream = socket.getInputStream();
                OutputStream outputStream = socket.getOutputStream();
                outputStream.write("register\n".getBytes());
                outputStream.write(registerLogin.getText().getBytes());
                outputStream.write('\n');
                outputStream.write(registerPassword.getText().getBytes());
                outputStream.write('\n');
                outputStream.write(nickname.getText().getBytes());
                outputStream.write('\n');
                if (inputStream.read() == 1) {
                    receiveTilemap(inputStream);
                    openMainFrame();
                    logInFrame.dispose();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        logInFrame.add(registerButton);
        logInFrame.pack();
        logInFrame.setVisible(true);
    }

    private static void receiveTilemap(InputStream inputStream) throws IOException {
        for (int i = 0; i < tilemap.length; i++) {
            for (int j = 0; j < tilemap.length; j++) {
                tilemap[i][j] = Tile.values()[inputStream.read()];
            }
        }
        System.out.println("Successfully received tilemap from server: " + tilemap);
    }

    private static void openMainFrame() {
        JFrame frame = new JFrame("Legends of Grind");
        frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.drawImage(image, 0, 0, TILE_SIZE, TILE_SIZE, null);
            }
        };
        frame.add(panel);
        frame.setVisible(true);
        try {
            while (true) {
                panel.repaint();
                Thread.sleep(20);
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}