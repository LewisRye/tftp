import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.time.Instant;
import java.util.Arrays;
import java.util.Scanner;

public class TFTPUDPClient {
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
            String filename;
            InetAddress serverAddress = InetAddress.getByName("127.0.0.1");
            int serverPort = 9000;
            byte[] packetData = new byte[512];
            DatagramSocket socket = new DatagramSocket(9999);
            socket.setSoTimeout(5000);

            if (input == 1) {
                System.out.print("Enter the filename to read from the server: ");
                filename = sc.next();

                byte[] rrqOpcode = new byte[]{(byte) 0, (byte) 1};
                byte[] rrqFilename = filename.getBytes();
                byte[] rrqMode = "octet".getBytes();
                byte[] rrqBytes = Arrays.copyOf(rrqOpcode, 2 + rrqFilename.length + 1 + rrqMode.length + 1);

                for (int i = 0; i < rrqFilename.length; i++) {
                    rrqBytes[i + 2] = rrqFilename[i]; // adding filename to rrq
                }
                rrqBytes[2 + rrqFilename.length] = (byte) 0; // add trailing 0

                for (int i = 0; i < rrqMode.length; i++) {
                    rrqBytes[i + 2 + rrqFilename.length + 1] = rrqMode[i]; // adding mode to rrq
                }
                rrqBytes[2 + rrqFilename.length + 1 + rrqMode.length] = (byte) 0; // add trailing 0

                DatagramPacket rrqPacket = new DatagramPacket(rrqBytes, rrqBytes.length, serverAddress, serverPort);
                socket.send(rrqPacket);
                System.out.println("\n" + Instant.now() + " | EVENT: Sent RRQ for " + filename);

                File fileToReceive = new File(filename);
                FileOutputStream fos = new FileOutputStream(fileToReceive);
                int blockNumber = 1;

                while (true) {
                    packetData = new byte[516];
                    DatagramPacket dataPacket = new DatagramPacket(packetData, 0, packetData.length);
                    socket.receive(dataPacket);

                    packetData = dataPacket.getData();
                    int length = dataPacket.getLength();
                    System.out.println(Instant.now() + " | EVENT: Received Packet " + blockNumber + " data length " + (length - 4));

                    if (packetData[1] == 5) {
                        System.out.println("File not found");
                        fos.close();
                        fileToReceive.delete();
                        break;
                    }

                    fos.write(Arrays.copyOfRange(packetData, 4, packetData.length), 0, length - 4);

                    packetData = new byte[]{(byte) 0, (byte) 4, (byte) ((blockNumber >> 8) & 0xFF), (byte) (blockNumber & 0xFF)};
                    DatagramPacket ackPacket = new DatagramPacket(packetData, packetData.length, dataPacket.getAddress(), dataPacket.getPort());
                    socket.send(ackPacket);
                    System.out.println(Instant.now() + " | EVENT: Sent Ack " + blockNumber);
                    blockNumber++;

                    if (length != 516) {
                        System.out.println(Instant.now() + " | EVENT: Receive File Complete");
                        fos.close();
                        break;
                    }
                }
            }

            if (input == 2) {
                System.out.print("Enter the filename to write to the server: ");
                filename = sc.next();

                if (new File(filename).exists()) {
                    byte[] wrqOpcode = new byte[]{(byte) 0, (byte) 2};
                    byte[] wrqFilename = filename.getBytes();
                    byte[] wrqMode = "octet".getBytes();
                    byte[] wrqBytes = Arrays.copyOf(wrqOpcode, 2 + wrqFilename.length + 1 + wrqMode.length + 1);

                    for (int i = 0; i < wrqFilename.length; i++) {
                        wrqBytes[i + 2] = wrqFilename[i]; // adding filename to rrq
                    }
                    wrqBytes[2 + wrqFilename.length] = (byte) 0;

                    for (int i = 0; i < wrqMode.length; i++) {
                        wrqBytes[i + 2 + wrqFilename.length + 1] = wrqMode[i]; // adding mode to rrq
                    }
                    wrqBytes[2 + wrqFilename.length + 1 + wrqMode.length] = (byte) 0;

                    DatagramPacket wrqPacket = new DatagramPacket(wrqBytes, wrqBytes.length, serverAddress, serverPort);
                    socket.send(wrqPacket);
                    System.out.println("\n" + Instant.now() + " | EVENT: Sent WRQ for " + filename);

                    File fileToSend = new File(filename);
                    FileInputStream fis = new FileInputStream(fileToSend);
                    int blockNumber = 1;

                    int readLength;
                    while ((readLength = fis.read(packetData)) != -1) {
                        DatagramPacket ackPacket = new DatagramPacket(new byte[4], 0, 4);
                        socket.receive(ackPacket);
                        System.out.println(Instant.now() + " | EVENT: Received Ack " + blockNumber);

                        byte[] header = new byte[]{(byte) 0, (byte) 3, (byte) ((blockNumber >> 8) & 0xFF), (byte) (blockNumber & 0xFF)};
                        byte[] assembledData = new byte[4 + readLength];

                        for (int i = 0; i < 4; i++) {
                            assembledData[i] = header[i]; // adding the opcode etc
                        }
                        for (int i = 0; i < readLength; i++) {
                            assembledData[i + 4] = packetData[i]; // adding the actual file content
                        }

                        DatagramPacket dataPacket = new DatagramPacket(assembledData, assembledData.length, ackPacket.getAddress(), ackPacket.getPort());
                        socket.send(dataPacket);
                        System.out.println(Instant.now() + " | EVENT: Sent Packet " + blockNumber + " data length " + (assembledData.length - 4));

                        blockNumber++;
                    }
                } else {
                    System.out.println("File not found");
                }
            }
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }
}