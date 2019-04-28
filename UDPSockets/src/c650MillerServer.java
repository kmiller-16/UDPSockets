import java.io.*;
import java.net.*;
import java.util.ArrayList;

public class c650MillerServer {

    private int mTimeout;
    private int mBytesPerPacket = 0;
    private static c650MillerServer mServer;
    private File mTestFile = new File("C:\\c650projs19\\stestfile");
    private long mFileSize = mTestFile.length();
    ArrayList<byte[]> mListofPackets;

    public static void main(String[] args) throws IOException {
       mServer = new c650MillerServer();
       mServer.configureServer();
        int numOfFullPackets = (int) (mServer.mTestFile.length() / mServer.mBytesPerPacket);
        mServer.mListofPackets = new ArrayList<>();
        for (int i = 0; i < numOfFullPackets + 1; i++) {
            byte[] bytestoSend = mServer.readBytesFromFile(mServer.mBytesPerPacket * i);
            mServer.mListofPackets.add(bytestoSend);
        }
       ServerSocket sockServ = new ServerSocket(21111);
        // 2.2 listen on port 21111 for a client
       while (true) {

           try {
               Socket socket = sockServ.accept();
               new Thread(new Runnable(){
                   @Override
                   public void run(){
                       int port;
                       try {
                           port = mServer.getPortFromClient(socket);
                           mServer.sendData(mServer.mListofPackets, port, mServer.mTimeout);
                       } catch (IOException e) {
                           e.printStackTrace();
                       }
                   }
               }).start();

           } catch (IOException e) {
               e.printStackTrace();
           }

           }
    }

    /**
     * 2.1 get config parameters from the user.
     */
    private void configureServer() {
        System.out.print("Enter an integer timeout T in ms: ");
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        try {
            mTimeout = Integer.parseInt(in.readLine());

        } catch (IOException e) {
            e.printStackTrace();
        }

        while (mBytesPerPacket == 0) {
            System.out.print("Enter the size M of all packets (except the last) in bytes: ");
            try {
                int enteredPacketSize = Integer.parseInt(in.readLine());
                if (enteredPacketSize <= mTestFile.length()){
                    mBytesPerPacket = enteredPacketSize;
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * This function gets the UDP port number from the client
     * @param socket the TCP socket used to connect to client
     * @return port that will be used to send data over UDP
     * @throws IOException
     */
    private int getPortFromClient(Socket socket) throws IOException{
        BufferedReader cin = new BufferedReader (new InputStreamReader(socket.getInputStream()));
        String message = cin.readLine();
        if(message.equals("hello")){
            System.out.print("\n"+message + "\n");
        }

        int port = Integer.parseInt(cin.readLine());
        System.out.print(port);
        socket.close();
        return port;
    }

    /**
     * Read bytes from file and write to a byte array that will later
     * be transmitted over UDP to a client.
     * @param startByte which byte of the file to start reading
     * @return byte array
     */
    private byte[] readBytesFromFile(int startByte) {

        int numberOfBytes;

        if (mTestFile.length() - startByte < mBytesPerPacket){
            numberOfBytes = (int)(mTestFile.length() - startByte);
        }
        else{
            numberOfBytes = mBytesPerPacket;
        }

        byte[] bytesRead = new byte[numberOfBytes];

        try {
            InputStream fileInputStream = new FileInputStream(mTestFile);
            try {
                fileInputStream.skip(startByte);
            } catch (IOException e) {
                e.printStackTrace();
            }

            try {
                fileInputStream.read(bytesRead, 0, numberOfBytes);
            } catch (IOException e) {
                e.printStackTrace();
            }

        } catch (FileNotFoundException e){
            e.printStackTrace();
        }

        return bytesRead;
    }

    /**
     * Send data to the client over UDP
     * 2.3 The server sends the bytes of the file
     * 2.4 packets are numbered and carry the file size
     * @param data data to be sent
     * @param port
     */
    private void sendData(ArrayList<byte[]> data, int port, int timeout){
        try {

            DatagramSocket dSocket;
            dSocket = new DatagramSocket();
            dSocket.setSoTimeout(timeout);

            try {
                InetAddress IP = InetAddress.getLocalHost();
                for (int i = 0; i < data.size(); i++){
                    byte[] payloadData = data.get(i);

                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                    outputStream.write(i);
                    try {
                        outputStream.write(longToBytes(mFileSize));
                        outputStream.write(intToBytes(payloadData.length));
                        outputStream.write(payloadData);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    byte[] payload = outputStream.toByteArray();

                    DatagramPacket packet = new DatagramPacket(payload, payload.length, IP, port);

                    try {
                        dSocket.send(packet);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                byte[] buff = new byte[300];
                DatagramPacket packet = new DatagramPacket(buff, buff.length);

            while(true){
                try {
                    dSocket.receive(packet);
                    String message = new String(packet.getData(), 0, packet.getLength());
                    if (message.equals("ok")){
                        System.out.print("\n" + port + " OK");
                        dSocket.close();
                        break;
                    }
                } catch (SocketTimeoutException e){
                    System.out.print("\n" + port + " resending...");

                    new Thread(new Runnable(){
                       @Override
                       public void run(){
                           sendData(mListofPackets, port, timeout *2);
                       }
                    }).start();
                    break;
                } catch (IOException e){
                    e.printStackTrace();
                }

            }

            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    /**
     * Simple function for converting a long to a byte array
     * @param num
     * @return byte array
     */
    private byte[] longToBytes(long num) {
        byte[] value = new byte[8];
        for (int i = 7; i >= 0; i--) {
            value[i] = (byte)(num & 0xFF);
            num >>= 8;
        }
        return value;
    }

    /**
     * Simple function for converting an int to a byte array
     * @param num
     * @return byte array
     */
    private byte[] intToBytes(int num) {
        byte[] value = new byte[4];
        for (int i = 3; i >= 0; i--) {
            value[i] = (byte)(num & 0xFF);
            num >>= 8;
        }
        return value;
    }



}
