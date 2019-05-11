import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.ArrayList;


public class c650MillerServer {

    // initial timeout value in milliseconds, set by user
    private int mTimeout;
    // bytes per packet, set by user
    private int mBytesPerPacket = 0;
    // server class
    private static c650MillerServer mServer;
    // test file location per the spec sheet
    private File mTestFile = new File("C:\\c650projs19\\stestfile");
    // test file size in bytes
    private long mFileSize = mTestFile.length();
    // all packets to be sent
    private ArrayList<byte[]> mData;

    public static void main(String[] args) throws IOException {
       mServer = new c650MillerServer();
       mServer.configureServer();

        // find the number of full packets and parse the file to be sent into individual packets

        int numOfFullPackets = (int) (mServer.mTestFile.length() / mServer.mBytesPerPacket);
        mServer.mData = new ArrayList<>();
        for (int i = 0; i < numOfFullPackets + 1; i++) {
            byte[] bytestoSend = mServer.readBytesFromFile(mServer.mBytesPerPacket * i);
            mServer.mData.add(bytestoSend);
        }

        // 2.2 listen on port 21111 for a client
       ServerSocket sockServ = new ServerSocket(21111);

       while (true) {
           try {

               // Accept the client connection
               Socket socket = sockServ.accept();
               DatagramSocket dSocket;
               dSocket = new DatagramSocket();

               // Handle the client in a different thread
               new Thread(new Runnable(){
                   @Override
                   public void run(){
                       int port;
                       try {
                           port = mServer.getPortFromClient(socket);

                           // Start the data timer
                           new Thread(new Runnable() {
                               @Override
                               public void run() {
                                   mServer.dataTimer(port, mServer.mTimeout, dSocket);
                               }
                           }).start();

                           // Try to send the data
                           new Thread(new Runnable(){
                               @Override
                               public void run(){
                                   mServer.sendData(port, dSocket);
                               }
                           }).start();

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
        int port = -1;
        BufferedReader cin = new BufferedReader (new InputStreamReader(socket.getInputStream()));
        String message = cin.readLine();
        if(message.equals("hello")){
            port = Integer.parseInt(cin.readLine());
            System.out.print("\n"+ port + " hello received");
        }

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
     * @param port port to send data to
     * @param dSocket socket to send data on
     */
    private void sendData(int port, DatagramSocket dSocket){

            try {
                InetAddress IP = InetAddress.getLocalHost();
                for (int i = 0; i < mData.size(); i++){
                    byte[] payloadData = mData.get(i);

                    // create a new output stream to write packet contents to
                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

                    try {
                        // write the packet number (bytes 0-3)
                        outputStream.write(intToBytes(i));
                        // write the total file size (bytes 4-11)
                        outputStream.write(longToBytes(mFileSize));
                        // write the payload size (bytes 12-15)
                        outputStream.write(intToBytes(payloadData.length));
                        // write the payload (bytes 16-payload size)
                        outputStream.write(payloadData);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    byte[] payload = outputStream.toByteArray();

                    DatagramPacket packet = new DatagramPacket(payload, payload.length, IP, port);

                    try {
                        // send the packet
                        dSocket.send(packet);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
    }

    /**
     * Set the data timer to an intial timeout value. If a timeout is reached before an ack is received,
     * reset the timer to double the timeout value, then resend all contents of the file. If an ack is received before
     * the timeout is reached, set an OK message timer and send an OK message to the client. If the OK timer times out
     * close the connection with the client. If a duplicate ack is received before the OK timer expires, double OK timeout
     * and resend the message.
     * @param port the port to communicate with the client on
     * @param timeout the timeout value to set the timer to in milliseconds
     * @param dSocket the datagram socket used to communicate with the client
     */
    private void dataTimer(int port, int timeout, DatagramSocket dSocket){
        byte[] buff = new byte[100];
        DatagramPacket packet = new DatagramPacket(buff, buff.length);
        try {
            // the socket timeout will function as our "data" timer
            dSocket.setSoTimeout(timeout);
        } catch (SocketException e) {
            e.printStackTrace();
        }
        while(true){
            try {
                try {
                    dSocket.receive(packet);
                } catch (SocketException e) {
                    break;
                }
                String message = new String(packet.getData(), 0, packet.getLength());

                // we got an ack from a client!
                if (message.equals("received")){
                    String ok = "OK";
                    System.out.print("\n" + port + " " + ok);
                    // start the ok timer
                    new Thread(new Runnable(){
                        @Override
                        public void run(){
                            okTimer(port, timeout, dSocket);
                        }
                    }).start();

                    // send the ok message
                    new Thread(new Runnable(){
                        @Override
                        public void run(){
                            sendOKmessage(port, dSocket);
                        }
                    }).start();
                    break;
                }
            } catch (SocketTimeoutException e){

                // The ack was not received before the data timer expired.
                // Double the timeout value and send the data again

                System.out.print("\n" + port + " resending...");

                // set the "data" timer
                new Thread(new Runnable(){
                    @Override
                    public void run(){
                        dataTimer(port, timeout*2, dSocket);
                    }
                }).start();

                // send the data
                new Thread(new Runnable(){
                    @Override
                    public void run(){
                        sendData(port, dSocket);
                    }
                }).start();
                break;
            } catch (IOException e){
                e.printStackTrace();
            }
        }
    }

    /**
     * Simple function for converting a long to a byte array
     * @param num
     * @return byte array
     */
    private byte[] longToBytes(long num) {
        ByteBuffer buffer = ByteBuffer.allocate(8);
        buffer.putLong(num);
        return buffer.array();
    }

    /**
     * Simple function for converting an int to a byte array
     * @param num
     * @return byte array
     */
    private byte[] intToBytes(int num) {
        ByteBuffer buffer = ByteBuffer.allocate(4);
        buffer.putInt(num);
        return buffer.array();
    }


    /**
     * Send the OK message to a client
     * @param port to reach the client on
     * @param socket to reach the client on
     */
    private void sendOKmessage(int port, DatagramSocket socket) {
        try {
            InetAddress IP = InetAddress.getLocalHost();
            String ok = "OK";
            DatagramPacket okPacket = new DatagramPacket(ok.getBytes(), ok.getBytes().length, IP, port);
            try {
                socket.send(okPacket);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    /**
     * Set the OK timer and send the OK message again if a duplicate ack is received
     * @param port to reach the client on
     * @param timeout to set the timer to in milliseconds
     * @param socket to reach the client on
     */
    private void okTimer(int port, int timeout, DatagramSocket socket){

        byte[] buff = new byte[100];
        DatagramPacket packet = new DatagramPacket(buff, buff.length);

        try {
            // set the "OK" timer
            socket.setSoTimeout(timeout);
        } catch (SocketException e) {
            e.printStackTrace();
        }

        while(true){
            try {
                socket.receive(packet);
                String message = new String(packet.getData(), 0, packet.getLength());
                // duplicate ack received
                if (message.equals("received")){
                    System.out.print("\n" + port + " duplicate ack received");
                    // set the "OK" timer to twice the previous timeout value
                    new Thread(new Runnable(){
                        @Override
                        public void run(){
                            okTimer(port, timeout*2, socket);
                        }
                    }).start();
                    // send the "OK message again"
                    new Thread(new Runnable(){
                        @Override
                        public void run(){
                            sendOKmessage(port, socket);
                        }
                    }).start();
                    break;
                }
            } catch (SocketTimeoutException e){
                // no duplicate acks! Close the socket, we're done servicing this client
                System.out.print("\n" + port + " done");
                socket.close();
                break;
            } catch (IOException e){
                e.printStackTrace();
            }
        }
    }

}
