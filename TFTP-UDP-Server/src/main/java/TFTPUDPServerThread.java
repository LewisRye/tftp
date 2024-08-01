import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;

public class TFTPUDPServerThread extends Thread {
    private DatagramPacket receivedPacket = null;
    private DatagramSocket socket = null;

    TFTPUDPServerThread(DatagramPacket receivedPacket) {
        this.receivedPacket = receivedPacket;
    }

    @Override
    public void run() {
        System.out.println(Instant.now() + " | EVENT: New Thread for " + receivedPacket.getAddress());

        byte[] packetData = receivedPacket.getData();
        int opcode = packetData[1]; // second byte

        String[] sections = new String(packetData, StandardCharsets.UTF_8).split("\0");
        String filename = sections[1].substring(1);

        try {
            InetAddress clientAddress = receivedPacket.getAddress();
            int clientPort = receivedPacket.getPort();

            socket = new DatagramSocket(clientPort + 1); // use a different port to avoid errors

            if (opcode == (char) 1) { // handle rrq
                System.out.println("------------------------------------------------------");
                System.out.println(Instant.now() + " | RECEIVED READ REQUEST");
                System.out.println("------------------------------------------------------");

                File fileToSend = new File(filename);

                if (fileToSend.exists()) {
                    FileInputStream fis = new FileInputStream(fileToSend);
                    int blockNumber = 1;

                    int readLength;
                    while ((readLength = fis.read(packetData)) != -1) { // while there are bytes to read
                        byte[] header = new byte[]{(byte) 0, (byte) 3, (byte) ((blockNumber >> 8) & 0xFF), (byte) (blockNumber & 0xFF)};
                        byte[] assembledData = new byte[4 + readLength];

                        for (int i = 0; i < 4; i++) {
                            assembledData[i] = header[i]; // adding the opcode and block number
                        }
                        for (int i = 0; i < readLength; i++) {
                            assembledData[i + 4] = packetData[i]; // adding the raw byte data
                        }

                        DatagramPacket dataPacket = new DatagramPacket(assembledData, assembledData.length, clientAddress, clientPort);
                        socket.send(dataPacket);
                        System.out.println(Instant.now() + " | EVENT: Sent Packet " + blockNumber + " data length " + (assembledData.length - 4));

                        DatagramPacket ackPacket = new DatagramPacket(new byte[4], 0, 4);
                        socket.receive(ackPacket);
                        System.out.println(Instant.now() + " | EVENT: Received Ack " + blockNumber);

                        blockNumber++;
                    }
                    System.out.println(Instant.now() + " | EVENT: Send File Complete");
                    fis.close();
                } else {
                    byte[] notFound = new byte[]{(byte) 0, (byte) 5, (byte) 0, (byte) 1, (byte) 0};
                    DatagramPacket errorPacket = new DatagramPacket(notFound, notFound.length, clientAddress, clientPort);
                    socket.send(errorPacket);
                    System.out.println(Instant.now() + " | EVENT: Sent File Not Found Error");

                }
            } else if (opcode == (char) 2) {
                System.out.println("------------------------------------------------------");
                System.out.println(Instant.now() + " | RECEIVED WRITE REQUEST");
                System.out.println("------------------------------------------------------");

                File fileToReceive = new File(filename); // create a new file with the correct filename

                byte[] ackByte = new byte[]{(byte) 0, (byte) 4, (byte) 0, (byte) 0};
                DatagramPacket ackPacket = new DatagramPacket(ackByte, ackByte.length, clientAddress, clientPort);
                socket.send(ackPacket);
                System.out.println(Instant.now() + " | EVENT: Sent Ack 0");

                FileOutputStream fos = new FileOutputStream(fileToReceive, false);
                int blockNumber = 1;

                while (true) {
                    packetData = new byte[516];
                    receivedPacket = new DatagramPacket(packetData, 0, packetData.length);
                    socket.receive(receivedPacket);

                    packetData = receivedPacket.getData();
                    int length = receivedPacket.getLength();
                    System.out.println(Instant.now() + " | EVENT: Received Packet " + blockNumber + " data length " + (length - 4));

                    fos.write(Arrays.copyOfRange(packetData, 4, packetData.length), 0, length - 4); // -4 to ignore the opcodes

                    packetData = new byte[]{(byte) 0, (byte) 4, (byte) ((blockNumber >> 8) & 0xFF), (byte) (blockNumber & 0xFF)};
                    ackPacket = new DatagramPacket(packetData, packetData.length, clientAddress, clientPort);
                    socket.send(ackPacket);
                    System.out.println(Instant.now() + " | EVENT: Sent Ack " + blockNumber);
                    blockNumber++;

                    if (length != 516) {
                        System.out.println(Instant.now() + " | EVENT: Receive File Complete");
                        fos.close();
                        break;
                    }
                }
            } else {
                System.out.println("------------------------------------------------------");
                System.out.println(Instant.now() + " | RECEIVED UNKNOWN PACKET");
                System.out.println("------------------------------------------------------");

                // this means you can only send rrq and wrq to the server
                packetData = new byte[]{(byte) 0, (byte) 5, (byte) 0, (byte) 0, (byte) 0};
                DatagramPacket errorPacket = new DatagramPacket(packetData, packetData.length, receivedPacket.getAddress(), receivedPacket.getPort());
                socket.send(errorPacket);
                System.out.println(Instant.now() + " | EVENT: Sent Unknown Error");
            }
            socket.close();
        } catch (IOException e) {
            System.out.println("ERROR : (in TFTPUDPServerThread.java) " + e.getMessage());
        }

    }
}
