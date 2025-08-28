import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Random;

public class Server {
    private static final int TILEMAP_SIZE = 100;
    private static final Path path = Path.of("tilemap.txt");
    private static final Tile[][] tilemap = new Tile[TILEMAP_SIZE][TILEMAP_SIZE];

    public static void main(String[] args) throws IOException, SQLException {
        loadOrGenerateTilemap();
        ServerSocket serverSocket = new ServerSocket(52);
        Socket socket = serverSocket.accept();
        BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        OutputStream outputStream = socket.getOutputStream();
        String operation = reader.readLine();
        if (operation.equals("register")) {
            String login = reader.readLine();
            String password = reader.readLine();
            String nickname = reader.readLine();
            Connection connection = DriverManager.getConnection("jdbc:sqlite:Legends of Grind.db");
            PreparedStatement preparedStatement = connection.prepareStatement("insert into users values (?, ?, ?, ?, ?)");
            preparedStatement.setString(1, login);
            preparedStatement.setString(2, password);
            preparedStatement.setString(3, nickname);
            preparedStatement.setInt(4, TILEMAP_SIZE / 2);
            preparedStatement.setInt(5, TILEMAP_SIZE / 2);
            outputStream.write(preparedStatement.executeUpdate());
            sendTilemap(outputStream);
        } else if (operation.equals("login")) {

        } else throw new IllegalStateException();
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
                tilemap[i][j] = random.nextBoolean() ? Tile.GRASS : Tile.BUSH;
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