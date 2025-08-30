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

public class Server {
    private static final int TILEMAP_SIZE = 100;
    private static final Path path = Path.of("tilemap.txt");
    private static final Tile[][] tilemap = new Tile[TILEMAP_SIZE][TILEMAP_SIZE];
    private static final DatagramSocket server;
    private static final HashMap<SocketAddress, Point> clients = new HashMap<>();

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
        Base64.Decoder decoder = Base64.getDecoder();
        httpServer.createContext("/register", exchange -> {
            try {
                String[] auth = new String(decoder.decode(exchange.getRequestHeaders().getFirst("Authorization").split(" ")[1])).split(":");
                PreparedStatement preparedStatement = connection.prepareStatement("insert into users values (?, ?, ?, ?, ?)");
                preparedStatement.setString(1, auth[0]);
                preparedStatement.setString(2, auth[1]);
                preparedStatement.setString(3, null);
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
                byte[] buf = new byte[2];
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                server.receive(packet);
                SocketAddress client = packet.getSocketAddress();
                if (!clients.containsKey(client)) {
                    System.out.println("connected new client " + client);
                }
                System.out.printf("received %s from %s%n", Arrays.toString(buf), client);
                clients.put(client, new Point(buf[0], buf[1]));
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
                    for (SocketAddress client : clients.keySet()) {
                        byte[] buf = getClientsBytes();
                        DatagramPacket packet = new DatagramPacket(buf, buf.length, client);
                        server.send(packet);
                        System.out.printf("sent packet %s to client %s%n", Arrays.toString(buf), client);
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }, 0, 1000);
    }

    private static byte[] getClientsBytes() {
        byte[] buf = new byte[clients.size() * 2];
        int i = 0;
        for (Point client : clients.values()) {
            buf[i] = (byte) client.x;
            buf[i + 1] = (byte) client.y;
            i += 2;
        }
        return buf;
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