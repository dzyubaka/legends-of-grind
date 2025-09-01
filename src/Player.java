import java.awt.*;
import java.io.*;

class Player {
    String nickname;
    Point position;

    Player() {
        position = new Point();
    }

    Player(DataInputStream dis) {
        try {
            nickname = dis.readUTF();
            position = new Point(dis.readShort(), dis.readShort());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    Player(byte[] buf) {
        try (DataInputStream dis = new DataInputStream(new ByteArrayInputStream(buf))) {
            nickname = dis.readUTF();
            position = new Point(dis.readShort(), dis.readShort());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    byte[] serialize() {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             DataOutputStream dos = new DataOutputStream(bos)) {
            serialize(dos);
            return bos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    void serialize(DataOutputStream dos) {
        try {
            dos.writeUTF(nickname);
            dos.writeShort(position.x);
            dos.writeShort(position.y);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
