import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class Server {
    public static void main(String[] args) throws IOException, SQLException {
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
        } else if (operation.equals("login")) {

        } else throw new IllegalStateException();
    }
}