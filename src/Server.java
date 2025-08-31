import com.sun.net.httpserver.HttpServer;

import java.awt.*;
import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

public class Server {
    private static final int TILEMAP_SIZE = 100;
    private static final Path path = Path.of("tilemap.txt");
    private static final Tile[][] tilemap = new Tile[TILEMAP_SIZE][TILEMAP_SIZE];
    private static final DatagramSocket server;
    private static final HashMap<SocketAddress, Player> clients = new HashMap<>();

    static {
        try {
            server = new DatagramSocket(8888);
        } catch (SocketException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) throws IOException, SQLException, InterruptedException {
        Connection connection = DriverManager.getConnection("jdbc:sqlite:Legends of Grind.db");
        HttpServer httpServer = HttpServer.create(new InetSocketAddress(80), 0);
        httpServer.createContext("/register", exchange -> {
            try {
                Map<String, String> formData = Arrays.stream(new String(exchange.getRequestBody().readAllBytes()).split("&"))
                        .map(s -> s.split("="))
                        .collect(Collectors.toMap(s -> s[0], s -> s[1]));
                PreparedStatement preparedStatement = connection.prepareStatement("insert into users values (?, ?, ?, ?, ?)");
                preparedStatement.setString(1, formData.get("login"));
                preparedStatement.setString(2, formData.get("password"));
                preparedStatement.setString(3, formData.get("nickname"));
                preparedStatement.setInt(4, TILEMAP_SIZE / 2);
                preparedStatement.setInt(5, TILEMAP_SIZE / 2);
                preparedStatement.executeUpdate();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            exchange.sendResponseHeaders(200, 0);
            exchange.close();
        });
        new Thread(httpServer::start).start();
        loadOrGenerateTilemap();
        scheduleSend();
        receive();
    }

    private static void receive() {
        try {
            while (true) {
                byte[] buf = new byte[16];
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                server.receive(packet);
                try (ByteArrayInputStream bis = new ByteArrayInputStream(buf)) {
                    String nickname = new String(bis.readNBytes(bis.read()));
                    Player player = new Player(nickname, new Point(bis.read(), bis.read()));
                    clients.put(packet.getSocketAddress(), player);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void scheduleSend() {
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    byte[] buf = serializePlayers();
                    for (SocketAddress player : clients.keySet()) {
                        DatagramPacket packet = new DatagramPacket(buf, buf.length, player);
                        server.send(packet);
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }, 0, 20);
    }

    private static byte[] serializePlayers() throws IOException {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            bos.write(clients.size());
            for (Player player : clients.values()) {
                bos.write(player.nickname.length());
                bos.write(player.nickname.getBytes());
                bos.write(player.position.x);
                bos.write(player.position.y);
            }
            return bos.toByteArray();
        }
    }

    private static void sendTilemap(OutputStream outputStream) throws IOException {
        for (int i = 0; i < TILEMAP_SIZE; i++) {
            for (int j = 0; j < TILEMAP_SIZE; j++) {
                outputStream.write(tilemap[i][j].ordinal());
            }
        }
    }

    private static void loadOrGenerateTilemap() {
        if (Files.exists(path)) {
            loadTilemap();
        } else {
            generateTilemap();
        }
    }

    private static void loadTilemap() {
        try (BufferedReader bufferedReader = Files.newBufferedReader(path)) {
            for (int i = 0; i < TILEMAP_SIZE; i++) {
                for (int j = 0; j < TILEMAP_SIZE; j++) {
                    tilemap[i][j] = Tile.values()[bufferedReader.read() - '0'];
                }
                bufferedReader.read();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void generateTilemap() {
        Random random = new Random();
        for (int i = 0; i < TILEMAP_SIZE; i++) {
            for (int j = 0; j < TILEMAP_SIZE; j++) {
                tilemap[i][j] = random.nextInt(10) == 0 ? (random.nextInt(10) == 0 ? Tile.STONE : Tile.BUSH) : Tile.GRASS;
            }
        }
    }

    private static void saveTilemap() throws IOException {
        try (BufferedWriter bufferedWriter = Files.newBufferedWriter(path, StandardOpenOption.CREATE)) {
            for (int i = 0; i < TILEMAP_SIZE; i++) {
                for (int j = 0; j < TILEMAP_SIZE; j++) {
                    bufferedWriter.write(tilemap[i][j].ordinal() + '0');
                }
                bufferedWriter.write('\n');
            }
        }
    }
}