import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Base64;
import java.util.Random;

public class Server {
    private static final int TILEMAP_SIZE = 100;
    private static final Path path = Path.of("tilemap.txt");
    private static final Tile[][] tilemap = new Tile[TILEMAP_SIZE][TILEMAP_SIZE];

    public static void main(String[] args) throws IOException, SQLException {
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
        httpServer.start();
        loadOrGenerateTilemap();
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