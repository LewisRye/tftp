import java.io.*;
import java.net.Socket;
import java.time.Instant;
import java.util.Arrays;
import java.util.Scanner;

public class TFTPTCPClient {
    public static void main(String[] args) {

        Scanner sc = new Scanner(System.in);
        System.out.print("Enter 1 to read a file from the server or 2 to send a file to the server: ");

        int input = 0;
        boolean done = false;
        while (!done) {
            input = sc.nextInt();

            if (!(input == 1 || input == 2)) {
                System.out.println("Invalid Choice");
                System.out.println("Enter 1 to read a file from the server or 2 to send a file to the server: ");
            } else {
                done = true;
            }
        }

        try {
            Socket socket = new Socket("127.0.0.1", 10000);
            byte[] buf = new byte[512];

            OutputStream out = socket.getOutputStream();
            InputStream in = socket.getInputStream();

            if (input == 1) {
                System.out.print("Enter the filename to read from the server: ");
                String filename = sc.next();

                File file = new File(filename);
                byte[] userInput = Arrays.copyOf(("01 " + file.getName() + " 0 octet 0").getBytes(), 512);
                out.write(userInput);
                out.flush();
                System.out.println(Instant.now() + " | EVENT: Sent RRQ for " + filename);

                FileOutputStream fos = new FileOutputStream(file);

                while (true) {
                    int data = in.read(buf, 0, buf.length);
                    if (new String(buf, 0, data).equals("File Not Found")) {
                        System.out.println(Instant.now() + " | ERROR: File Not Found");
                        fos.close();
                        file.delete();
                        return;
                    }

                    System.out.println(Instant.now() + " | EVENT: Received Packet");

                    fos.write(buf, 0, data);
                    if (data != 512) {
                        System.out.println(Instant.now() + " | EVENT: Complete");
                        fos.close();
                        break; // prevents connection reset error
                    }
                }
            }

            if (input == 2) {
                System.out.print("Enter the filename to write to the server: ");
                String filename = sc.next();

                File file = new File(filename);

                if (!file.exists()) {
                    System.out.println(Instant.now() + " | ERROR: File Not Found");
                    file.delete();
                    return;
                }

                byte[] userInput = Arrays.copyOf(("02 " + file.getName() + " 0 octet 0").getBytes(), 512);
                out.write(userInput);
                out.flush();
                System.out.println(Instant.now() + " | EVENT: Sent WRQ for " + filename);

                FileInputStream fis = new FileInputStream(file);

                int read;
                while ((read = fis.read(buf)) != -1) {
                    System.out.println(Instant.now() + " | EVENT: Sent Packet");
                    out.write(buf, 0, read);
                    out.flush();
                    buf = new byte[512]; // get ready for next packet
                }
                System.out.println(Instant.now() + " | EVENT: Complete");
                fis.close();
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }
}