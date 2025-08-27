import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

public class Client {
    public static void main(String[] args) {
        JFrame frame = new JFrame("Legends of Grind");
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new GridLayout(4, 4));
        frame.add(new JLabel("Login"));
        JTextField registerLogin = new JTextField();
        frame.add(registerLogin);
        frame.add(new JLabel("Login"));
        JTextField logInLogin = new JTextField();
        frame.add(logInLogin);
        frame.add(new JLabel("Password"));
        JPasswordField registerPassword = new JPasswordField();
        frame.add(registerPassword);
        frame.add(new JLabel("Password"));
        JPasswordField logInPassword = new JPasswordField();
        frame.add(logInPassword);
        frame.add(new JLabel("Nickname"));
        JTextField nickname = new JTextField();
        frame.add(nickname);
        frame.add(new Container());
        JButton logInButton = new JButton("Log In");
        logInButton.addActionListener(_ -> {
            try {
                Socket socket = new Socket("localhost", 52);
                OutputStream outputStream = socket.getOutputStream();
                outputStream.write("login".getBytes());
                outputStream.write(logInLogin.getText().getBytes());
                outputStream.write(logInPassword.getText().getBytes());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        frame.add(logInButton);
        frame.add(new Container());
        JButton registerButton = new JButton("Register");
        registerButton.addActionListener(_ -> {
            try {
                Socket socket = new Socket("localhost", 52);
                OutputStream outputStream = socket.getOutputStream();
                outputStream.write("register\n".getBytes());
                outputStream.write((registerLogin.getText() + '\n').getBytes());
                outputStream.write((registerPassword.getText() + '\n').getBytes());
                outputStream.write((nickname.getText() + '\n').getBytes());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        frame.add(registerButton);
        frame.pack();
        frame.setVisible(true);
    }
}