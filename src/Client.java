import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Arrays;

public class Client {
    private static final int TILEMAP_SIZE = 100;
    private static final int TILE_SIZE = 64;
    private static final Tile[][] tilemap = new Tile[TILEMAP_SIZE][TILEMAP_SIZE];
    private static final boolean[] wasd = new boolean[4];
    private static final Player player = new Player(null, new Point());
    private static final HttpClient httpClient = HttpClient.newHttpClient();
    private static final InetSocketAddress serverAddress = new InetSocketAddress("localhost", 8888);
    private static BufferedImage playerImage, grass, bush, stone;
    private static Player[] players = new Player[0];

    public static void main(String[] args) throws IOException {
        loadSprites();
        openLogInFrame();
    }

    private static void loadSprites() throws IOException {
        playerImage = ImageIO.read(Client.class.getResourceAsStream("player.png"));
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
        JTextField nicknameField = new JTextField();
        logInFrame.add(nicknameField);
        logInFrame.add(new Container());
        logInFrame.add(new JButton("Log In"));
        logInFrame.add(new Container());
        JButton registerButton = new JButton("Register");
        registerButton.addActionListener(_ -> {
            try {
                player.nickname = nicknameField.getText();
                String body = String.format("login=%s&password=%s&nickname=%s", registerLogin.getText(), registerPassword.getText(), player.nickname);
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost/register"))
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .build();
                httpClient.send(request, HttpResponse.BodyHandlers.discarding());
                openMainFrame();
                logInFrame.dispose();
            } catch (IOException | InterruptedException e) {
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

    private static void openMainFrame() throws SocketException {
        JFrame frame = new JFrame("Legends of Grind");
        frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                int halfWidthTileCount = getWidth() / 2 / TILE_SIZE;
                int halfHeightTileCount = getHeight() / 2 / TILE_SIZE;
                for (int i = player.position.x / TILE_SIZE - halfWidthTileCount - 1; i < player.position.x / TILE_SIZE + halfWidthTileCount + 1; i++) {
                    for (int j = -player.position.y / TILE_SIZE - halfHeightTileCount - 2; j < -player.position.y / TILE_SIZE + halfHeightTileCount + 2; j++) {
                        int tileIndexX = i + TILEMAP_SIZE / 2;
                        int tileIndexY = j + TILEMAP_SIZE / 2;
                        BufferedImage tileSprite = tilemap[tileIndexX][tileIndexY] == Tile.STONE ? stone : (tilemap[tileIndexX][tileIndexY] == Tile.BUSH ? bush : grass);
                        int tileX = i * TILE_SIZE + getWidth() / 2 - player.position.x;
                        int tileY = j * TILE_SIZE + getHeight() / 2 + player.position.y;
                        g.drawImage(tileSprite, tileX, tileY, TILE_SIZE, TILE_SIZE, null);
                    }
                }
                renderPlayers(g);
                int playerX = (getWidth() - TILE_SIZE) / 2;
                int playerY = (getHeight() - TILE_SIZE) / 2;
                g.drawImage(playerImage, playerX, playerY, TILE_SIZE, TILE_SIZE, null);
            }

            private void renderPlayers(Graphics g) {
                for (Player p : players) {
                    if (!p.nickname.equals(player.nickname)) {
                        int playerX = p.position.x + (getWidth() - TILE_SIZE) / 2 - player.position.x;
                        int playerY = -p.position.y + (getHeight() - TILE_SIZE) / 2 + player.position.y;
                        g.drawImage(playerImage, playerX, playerY, TILE_SIZE, TILE_SIZE, null);
                    }
                }
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
        DatagramSocket server = new DatagramSocket();
        new Timer(20, _ -> {
            try {
                byte[] buf = serializePlayer();
                server.send(new DatagramPacket(buf, 0, buf.length, serverAddress));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).start();
        new Thread(() -> {
            try {
                while (true) {
                    byte[] buf = new byte[16];
                    server.receive(new DatagramPacket(buf, buf.length));
                    players = deserializePlayers(buf);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).start();
    }

    private static byte[] serializePlayer() throws IOException {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             DataOutputStream dos = new DataOutputStream(bos)) {
            dos.writeUTF(player.nickname);
            dos.writeShort(player.position.x);
            dos.writeShort(player.position.y);
            return bos.toByteArray();
        }
    }

    private static Player[] deserializePlayers(byte[] buf) throws IOException {
        try (DataInputStream dis = new DataInputStream(new ByteArrayInputStream(buf))) {
            Player[] players = new Player[dis.read()];
            for (int i = 0; i < players.length; i++) {
                Player player = new Player(new String(dis.readUTF()), new Point(dis.readShort(), dis.readShort()));
                players[i] = player;
            }
            return players;
        }
    }

    private static void move() {
        if (wasd[0]) {
            player.position.y++;
        }
        if (wasd[1]) {
            player.position.x--;
        }
        if (wasd[2]) {
            player.position.y--;
        }
        if (wasd[3]) {
            player.position.x++;
        }
    }
}