import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.Instant;

public class TFTPTCPServer {
    public static void main(String[] args) throws IOException {
        ServerSocket masterSocket;
        Socket slaveSocket;

        masterSocket = new ServerSocket(10000);
        System.out.println(Instant.now() + " | EVENT: Server Started");

        while (true) {
            slaveSocket = masterSocket.accept();
            new TFTPTCPServerThread(slaveSocket).start(); // multithreading for multiple connections
        }
    }
}
