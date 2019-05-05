import java.io.*;
import java.io.IOException;
import java.net.*;
import java.util.*;

public class c650MillerClient {
    private static c650MillerClient mClient;
    private int PORT_UPPER_BOUND = 65535;
    private int PORT_LOWER_BOUND = 16384;
    // the LinkedHashSet data provides a convenient (although heavy weight) solution to only keep unique packets
    // while still maintaining order.
    private LinkedHashSet<byte[]> mOrderedPackets = new LinkedHashSet<>();
    private LinkedHashSet<byte[]> mOutOfOrderPackets = new LinkedHashSet<>();
    private long mNumOfBytesReceived = 0;
    private int mExpectedPacketNumber = 0;


    public static void main(String[] args) throws IOException {
        mClient = new c650MillerClient();
        int port = mClient.generatePortNumber();
        DatagramSocket uSocket = new DatagramSocket(port);
        // start a thread to receive data on the generated port number
        new Thread(new Runnable(){
            @Override
            public void run(){
                try {
                    mClient.receiveData(port, uSocket);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();

        // give the server the port number to start receiving data
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
     * @param uSocket socket to receive data on
     * @throws IOException
     */
    private void receiveData(int port, DatagramSocket uSocket) throws IOException{
        System.out.print("Listening on " + port);
        // set buff size to make UDP packet size in bytes
        byte[] buff = new byte[65535];
        long fileSize;
        DatagramPacket packet = null;
        int ServerPort = 0;
        boolean ackSent = false;

        while(true){

            packet = new DatagramPacket(buff, buff.length);

            try {
                uSocket.receive(packet);
            } catch (SocketException e) {
                break;
            }

            ServerPort = packet.getPort();

            // if an ack was previously sent and we get another packet, we'll check to see if it's the okay message
            // and close the socket if that is the case. If the message contains anything else we'll drop the packet
            // and resend the ack.
            if (ackSent){
                String message = new String(packet.getData(), 0, packet.getLength());
                if (message.equals("OK")){
                    System.out.print("\n" + port + " done");
                    uSocket.close();
                    break;
                }
                else{
                    String ack = "received";
                    InetAddress IP = InetAddress.getLocalHost();
                    DatagramPacket ackPacket = new DatagramPacket(ack.getBytes(), ack.getBytes().length, IP , ServerPort);
                    uSocket.send(ackPacket);
                    System.out.print("\n" + port + " ACK resent...");
                }
            }

            else{

                // packet number is found in bytes 0-3
                int packetNumber = bytesToInt(Arrays.copyOfRange(packet.getData(), 0, 4));

                // the size of the source file (long) is found in bytes 4-12.
                byte[] fileSizeBytes = Arrays.copyOfRange(packet.getData(), 4,12);
                fileSize = bytesToLong(fileSizeBytes);

                if(mExpectedPacketNumber == packetNumber){
                    processOrderedPacket(packet);
                    checkOutofOrderPackets();
                }

                else{
                    mOutOfOrderPackets.add(packet.getData());
                }

                //reset buffer
                buff = new byte[65535];

                if(fileSize == mNumOfBytesReceived){
                    String ack = "received";
                    InetAddress IP = InetAddress.getLocalHost();
                    DatagramPacket ackPacket = new DatagramPacket(ack.getBytes(), ack.getBytes().length, IP , ServerPort);
                    uSocket.send(ackPacket);
                    ackSent = true;
                    System.out.print("\n" + port + " ACK Sent...");
                    writeToFile(mOrderedPackets);
                }

                }
            }
    }

    /**
     * strip the data from a packet and insert it into the ordered list
     * @param packet packet to process
     */
    private void processOrderedPacket(DatagramPacket packet){
        // the payload size is found in bytes 12-16. Bytes 16-end are payload.
        byte[] payloadSizeBytes = Arrays.copyOfRange(packet.getData(), 12, 16);
        byte[] messageData = Arrays.copyOfRange(packet.getData(), 16, bytesToInt(payloadSizeBytes) + 16);
        mNumOfBytesReceived = mNumOfBytesReceived + messageData.length;
        mOrderedPackets.add(messageData);
        mExpectedPacketNumber++;
    }

    /**
     * Check the out of ordered packets to see if a any can be added to the ordered list
     */
    private void checkOutofOrderPackets(){
        boolean matchFound = false;

        for (byte[] Pkt : mOutOfOrderPackets){
            int packNum = bytesToInt(Arrays.copyOfRange(Pkt, 0, 4));
            if(packNum == mExpectedPacketNumber){
                matchFound = true;
                byte[] payloadSize = Arrays.copyOfRange(Pkt, 12, 16);
                byte[] messagedata = Arrays.copyOfRange(Pkt, 16, bytesToInt(payloadSize) + 16);
                mNumOfBytesReceived = mNumOfBytesReceived + messagedata.length;
                mOrderedPackets.add(messagedata);
                mOutOfOrderPackets.remove(Pkt);
                mExpectedPacketNumber++;
            }
        }
        // reRun the function to check for more matches
        if(matchFound){
            checkOutofOrderPackets();
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
    private void writeToFile(LinkedHashSet<byte[]> bytes){
        File file = new File("C:\\c650projs19\\ctestfile");
        try {
            OutputStream os = new FileOutputStream(file);
            for (byte[] chunk : bytes){
                // uncomment lines to see output in terminal
                // String m = new String(chunk, 0, chunk.length);
                // System.out.print(m);
               os.write(chunk);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
