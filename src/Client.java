import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class Client {
    private static final int TILEMAP_SIZE = 100;
    private static final int TILE_SIZE = 64;
    private static final Tile[][] tilemap = new Tile[TILEMAP_SIZE][TILEMAP_SIZE];
    private static final boolean[] wasd = new boolean[4];
    private static BufferedImage player, grass, bush;
    private static int x, y;

    public static void main(String[] args) throws IOException {
        loadSprites();
        openLogInFrame();
    }

    private static void loadSprites() throws IOException {
        player = ImageIO.read(Client.class.getResourceAsStream("player.png"));
        grass = ImageIO.read(Client.class.getResourceAsStream("grass.png"));
        bush = ImageIO.read(Client.class.getResourceAsStream("bush.png"));
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
        for (int i = 0; i < TILEMAP_SIZE; i++) {
            for (int j = 0; j < TILEMAP_SIZE; j++) {
                tilemap[i][j] = Tile.values()[inputStream.read()];
            }
        }
    }

    private static void openMainFrame() {
        JFrame frame = new JFrame("Legends of Grind");
        frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                for (int i = x / TILE_SIZE; i < x / TILE_SIZE + 16; i++) {
                    for (int j = -y / TILE_SIZE; j < -y / TILE_SIZE + 8; j++) {
                        BufferedImage tileSprite = tilemap[i + 8][j + 4] == Tile.GRASS ? grass : bush;
                        g.drawImage(tileSprite, i * TILE_SIZE + (getWidth() - 16 * TILE_SIZE) / 2 - x, j * TILE_SIZE + (getHeight() - 8 * TILE_SIZE) / 2 + y, TILE_SIZE, TILE_SIZE, null);
                        g.drawRect(i * TILE_SIZE + (getWidth() - 16 * TILE_SIZE) / 2 - x, j * TILE_SIZE + (getHeight() - 8 * TILE_SIZE) / 2 + y, TILE_SIZE, TILE_SIZE);
                        g.drawImage(player, (getWidth() - TILE_SIZE) / 2, (getHeight() - TILE_SIZE) / 2, TILE_SIZE, TILE_SIZE, null);
                        g.drawRect((getWidth() - TILE_SIZE) / 2, (getHeight() - TILE_SIZE) / 2, TILE_SIZE, TILE_SIZE);
                    }
                }
                g.drawString("x = " + x, 0, 8);
                g.drawString("y = " + y, 0, 16);
            }
        };
        frame.add(panel);
        frame.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {

            }

            @Override
            public void keyPressed(KeyEvent e) {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_W -> wasd[0] = true;
                    case KeyEvent.VK_A -> wasd[1] = true;
                    case KeyEvent.VK_S -> wasd[2] = true;
                    case KeyEvent.VK_D -> wasd[3] = true;
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_W -> wasd[0] = false;
                    case KeyEvent.VK_A -> wasd[1] = false;
                    case KeyEvent.VK_S -> wasd[2] = false;
                    case KeyEvent.VK_D -> wasd[3] = false;
                }
            }
        });
        frame.setVisible(true);
        new Timer(1000 / 50, _ -> {
            try {
                move();
                panel.repaint();
                Thread.sleep(20);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }).start();
    }

    private static void move() {
        if (wasd[0]) {
            y++;
        }
        if (wasd[1]) {
            x--;
        }
        if (wasd[2]) {
            y--;
        }
        if (wasd[3]) {
            x++;
        }
    }
}