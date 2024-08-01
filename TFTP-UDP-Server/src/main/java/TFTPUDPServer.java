import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.time.Instant;

public class TFTPUDPServer {
    public static void main(String[] args) {
        // use any port over 1024
        int port = 9000;

        try (DatagramSocket socket = new DatagramSocket(port)) {
            System.out.println(Instant.now() + " | EVENT: Server Online");

            while (true) {
                // packet size is 512
                DatagramPacket requestPacket = new DatagramPacket(new byte[512], 0, 512);
                socket.receive(requestPacket); // receives request packet

                byte[] requestData = requestPacket.getData();

                if (requestData[1] == 1 || requestData[1] == 2) { // let the client time out if not a request
                    new TFTPUDPServerThread(requestPacket).start(); // multithreading for many connections
                }
            }
        } catch (IOException e) {
            System.err.println("ERROR : (in TFTPUDPServer.java) " + e.getMessage());
        }
    }
}