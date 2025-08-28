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

public class Server {
    private static final Path path = Path.of("tilemap.txt");
    private static final Tile[][] tilemap = new Tile[100][100];

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
            PreparedStatement preparedStatement = connection.prepareStatement("insert into users values (?, ?, ?)");
            preparedStatement.setString(1, login);
            preparedStatement.setString(2, password);
            preparedStatement.setString(3, nickname);
            preparedStatement.execute();
            outputStream.write(1);
            sendTilemap(outputStream);
        } else if (operation.equals("login")) {

        } else throw new IllegalStateException();
    }

    private static void sendTilemap(OutputStream outputStream) throws IOException {
        for (int i = 0; i < tilemap.length; i++) {
            for (int j = 0; j < tilemap.length; j++) {
                outputStream.write(tilemap[i][j].ordinal());
            }
        }
        System.out.println("Successfully sent tilemap to client");
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
            for (int i = 0; i < tilemap.length; i++) {
                for (int j = 0; j < tilemap.length; j++) {
                    tilemap[i][j] = Tile.values()[bufferedReader.read() - '0'];
                }
                bufferedReader.read();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void generateTilemap() {
        for (int i = 0; i < tilemap.length; i++) {
            for (int j = 0; j < tilemap.length; j++) {
                tilemap[i][j] = Tile.GRASS;
            }
        }
    }

    private static void saveTilemap() throws IOException {
        try (BufferedWriter bufferedWriter = Files.newBufferedWriter(path, StandardOpenOption.CREATE)) {
            for (int i = 0; i < tilemap.length; i++) {
                for (int j = 0; j < tilemap.length; j++) {
                    bufferedWriter.write(tilemap[i][j].ordinal() + '0');
                }
                bufferedWriter.write('\n');
            }
        }
    }
}