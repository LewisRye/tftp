import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

public class TFTPTCPServerThread extends Thread {
    private Socket slaveSocket = null;

    TFTPTCPServerThread(Socket socket) {
        this.slaveSocket = socket;
    }

    @Override
    public void run() {
        System.out.println("\u001B[1m\n" + Instant.now() + " | EVENT: New Thread for " + slaveSocket.getInetAddress().getHostAddress() + "\u001B[0m");

        FileOutputStream fos = null;
        FileInputStream fis = null;

        try {
            byte[] buf = new byte[512];
            String method = "";
            String filename = "";
            OutputStream out = slaveSocket.getOutputStream();
            InputStream in = slaveSocket.getInputStream();

            int bytesReceived = in.read(buf, 0, buf.length);
            String dataReceived = new String(buf, 0, bytesReceived, StandardCharsets.UTF_8);
            String[] request = dataReceived.split("\\s+");

            method = request[0];
            filename = request[1];

            if (method.equals("01")) { // handle rrq
                System.out.println("\n" + Instant.now() + " | EVENT: Received RRQ for " + filename);

                if (!new File(filename).exists()) {
                    System.out.println(Instant.now() + " | EVENT: Sent File Not Found Packet");
                    out.write(("File Not Found").getBytes());
                    return;
                }

                fis = new FileInputStream(filename);

                int read;
                while ((read = fis.read(buf)) != -1) { // read all bytes in the file
                    System.out.println(Instant.now() + " | EVENT: Sent Packet");
                    out.write(buf, 0, read); // send the bytes
                    out.flush(); // ensures all bytes were written correctly
                    buf = new byte[512]; // empty buf for next packet
                }
                System.out.println(Instant.now() + " | EVENT: Complete");
                fis.close();
            }

            if (method.equals("02")) { // handle wrq
                System.out.println("\n" + Instant.now() + " | EVENT: Received WRQ for " + filename);
                fos = new FileOutputStream(filename);

                while (true) {
                    System.out.println(Instant.now() + " | EVENT: Received Packet");

                    int data = in.read(buf, 0, buf.length);

                    fos.write(buf, 0, data);
                    if (data != 512) { // ends when there are not 512 more bytes to send
                        System.out.println(Instant.now() + " | EVENT: Complete");
                        fos.close();
                        break; // prevents connection reset error
                    }
                }
            }
        } catch (IOException e) {
            System.err.println(e.getMessage());;
        }
    }
}