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
    private static BufferedImage player, grass, bush, stone;
    private static int x, y;

    public static void main(String[] args) throws IOException {
        loadSprites();
        openLogInFrame();
    }

    private static void loadSprites() throws IOException {
        player = ImageIO.read(Client.class.getResourceAsStream("player.png"));
        grass = ImageIO.read(Client.class.getResourceAsStream("grass.png"));
        bush = ImageIO.read(Client.class.getResourceAsStream("bush.png"));
        stone = ImageIO.read(Client.class.getResourceAsStream("stone.png"));
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
                int halfWidthTileCount = getWidth() / 2 / TILE_SIZE;
                int halfHeightTileCount = getHeight() / 2 / TILE_SIZE;
                for (int i = x / TILE_SIZE - halfWidthTileCount - 1; i < x / TILE_SIZE + halfWidthTileCount + 1; i++) {
                    for (int j = -y / TILE_SIZE - halfHeightTileCount - 2; j < -y / TILE_SIZE + halfHeightTileCount + 2; j++) {
                        int tileIndexX = i + TILEMAP_SIZE / 2;
                        int tileIndexY = j + TILEMAP_SIZE / 2;
                        BufferedImage tileSprite = tilemap[tileIndexX][tileIndexY] == Tile.STONE ? stone : (tilemap[tileIndexX][tileIndexY] == Tile.GRASS ? grass : bush);
                        int tileX = i * TILE_SIZE + getWidth() / 2 - x;
                        int tileY = j * TILE_SIZE + getHeight() / 2 + y;
                        g.drawImage(tileSprite, tileX, tileY, TILE_SIZE, TILE_SIZE, null);
                    }
                }
                int playerX = (getWidth() - TILE_SIZE) / 2;
                int playerY = (getHeight() - TILE_SIZE) / 2;
                g.drawImage(player, playerX, playerY, TILE_SIZE, TILE_SIZE, null);
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
            move();
            panel.repaint();
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