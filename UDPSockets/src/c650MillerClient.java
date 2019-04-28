import java.io.*;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

public class c650MillerClient {
    private static c650MillerClient mClient;
    private int PORT_UPPER_BOUND = 65535;
    private int PORT_LOWER_BOUND = 16384;


    public static void main(String[] args) throws IOException {
        mClient = new c650MillerClient();
        int port = mClient.generatePortNumber();

        new Thread(new Runnable(){
            @Override
            public void run(){
                try {
                    mClient.receiveData(port);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();

        mClient.giveServerPort(port);

    }

    /**
     * // 2.2 give the server a socket number
     * @param port
     */
    private void giveServerPort(int port) {
        PrintStream sout;

        try (Socket sk = new Socket("127.0.0.1", 21111)){
            sout = new PrintStream(sk.getOutputStream());
            sout.println("hello");
            sout.println(port);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * Generate a random port number
     * @return port number
     */
    private int generatePortNumber(){
        Random r = new Random();
        int portnum = r.nextInt(PORT_UPPER_BOUND - PORT_LOWER_BOUND) + PORT_LOWER_BOUND;
        return portnum;
    }

    /**
     * receive data on UDP port
     * @param port port data will be received on
     * @throws IOException
     */
    private void receiveData(int port) throws IOException{
        DatagramSocket uSocket = new DatagramSocket(port);
        System.out.print("Listening on " + port);
        byte[] buff = new byte[1500];
        long fileSize;
        long numOfBytesReceived = 0;
        int expectedPacketNumber = 0;
        DatagramPacket packet = null;
        int ServerPort = 0;
        boolean ackSent = false;

        ArrayList<byte[]> orderedPackets = new ArrayList<>();
        ArrayList<byte[]> outOfOrderPackets = new ArrayList<>();

        while(true){

            packet = new DatagramPacket(buff, buff.length);

            uSocket.receive(packet);

            // the size of the source file (long) is found in bytes 1-8.
            byte[] fileSizeBytes = Arrays.copyOfRange(packet.getData(), 1,9);
            fileSize = bytesToLong(fileSizeBytes);

            ServerPort = packet.getPort();

            // TODO: Breakout into different functions and add additional measures for order/duplicate checking.

            // packet number is at byte 0
            if(expectedPacketNumber == packet.getData()[0]){
                // the payload size is found in bytes 9-12. Bytes 13-end are payload.
                byte [] payloadSizeBytes = Arrays.copyOfRange(packet.getData(), 9, 13);
                byte[] messageData = Arrays.copyOfRange(packet.getData(), 13, bytesToInt(payloadSizeBytes) + 13);
                numOfBytesReceived = numOfBytesReceived + messageData.length;
                String message = new String(messageData);
                System.out.print(message);
                orderedPackets.add(messageData);
                expectedPacketNumber++;

                for (int i = 0; i < outOfOrderPackets.size(); i++){
                    if(outOfOrderPackets.get(i)[0] == expectedPacketNumber){
                        byte[] messagedata = Arrays.copyOfRange(packet.getData(), 13, packet.getData().length);
                        numOfBytesReceived = numOfBytesReceived + messagedata.length;
                        String m = new String(messagedata, 0, messagedata.length);
                        System.out.print(m);
                        orderedPackets.add(messagedata);
                        expectedPacketNumber++;
                    }
                }
                buff = new byte[1500];
            }

            else{
                outOfOrderPackets.add(packet.getData());
            }

            if(fileSize == numOfBytesReceived){
                String ok = "ok";
                InetAddress IP = InetAddress.getLocalHost();
                DatagramPacket okPacket = new DatagramPacket(ok.getBytes(), ok.getBytes().length, IP , ServerPort);
                uSocket.send(okPacket);
                System.out.print("\n" + port + " ACK Sent...");
                //uSocket.close();
                writeToFile(orderedPackets);
            }

        }

    }

    /**
     * Simple function for converting a byte array to a long
     * @param bytes
     * @return long
     */
    private long bytesToLong(byte[] bytes) {
        long value = 0;
        for (int i = 0; i < 8; i++) {
            value <<= 8;
            value |= (bytes[i] & 0xFF);
        }
        return value;
    }

    /**
     * Simple function for converting a byte array to an int
     * @param bytes
     * @return int
     */
    private int bytesToInt(byte[] bytes) {
        int value = 0;
        for (int i = 0; i < 4; i++) {
            value <<= 8;
            value |= (bytes[i] & 0xFF);
        }
        return value;
    }

    /**
     * This function writes the data from the server to a file.
     * @param bytes
     */
    private void writeToFile(ArrayList<byte[]> bytes){
        File file = new File("C:\\c650projs19\\ctestfile");
        try {
            OutputStream os = new FileOutputStream(file);

            for (int i = 0; i < bytes.size(); i++){
               os.write(bytes.get(i));
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
