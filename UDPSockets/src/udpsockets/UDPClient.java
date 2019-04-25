/*
Instructions - Run the UDP Sockets file first to start the server. Then
start the client. TCP will send mssgs back and forth between programs each side
must say "bye" to end the connection. Once the TCP connection ends, UDP runs.
UDP is just the client sending mssgs to the server. say "bye" from the client
to quit. This code will have to be adapted to what he wants.
 */
package udpsockets;
//imports
import java.io.BufferedReader;
import java.io.IOException; 
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.DatagramPacket; 
import java.net.DatagramSocket; 
import java.net.InetAddress; 
import java.net.Socket;


public class UDPClient {
    public static void main(String args[]) throws IOException{
        System.out.println(System.in.toString());
        runTCPCli();
        runUDPCli();
    }
    public static void runUDPCli() throws IOException{
        
        System.out.println(System.in.toString());
        BufferedReader read = new BufferedReader(new InputStreamReader(System.in));
        DatagramSocket dSocket = new DatagramSocket();
        InetAddress ip = InetAddress.getLocalHost();
        byte buffer[] = null;
        String input;
        System.out.println("UDP Client Started...");
        boolean displayed = false;
        while(true){
                displayed = false;
                input = read.readLine();
                System.out.println("Sending: " + input);
                buffer = input.getBytes();
                DatagramPacket sendMe = new DatagramPacket(
                        buffer, buffer.length, ip, 5432);
                dSocket.send(sendMe);
                if (input.equals("bye")) {
                    break;
                }
            
            else if (!displayed) {
                System.out.println("waiting");
                displayed = true;
            }
        }
        read.close();
        dSocket.close();
    }
    public static void runTCPCli() throws IOException{
        BufferedReader sin;
        PrintStream sout;
        BufferedReader stdin;
        try (Socket sk = new Socket("127.0.0.1",5000)) {
            sin = new BufferedReader (new InputStreamReader(sk.getInputStream()));
            sout = new PrintStream(sk.getOutputStream());
            stdin = new BufferedReader (new InputStreamReader(System.in));
            String s;
            while (true)
            {
                System.out.print("Client : ");
                s=stdin.readLine();
                sout.println(s);
                if (s.equalsIgnoreCase("Bye"))
                {
                    System.out.println("Connection ended by client");
                    break;
                }
                s=sin.readLine();
                System.out.print("Server : " +s+"\n");
            }
        }
        /*sin.close();
        sout.close();
        stdin.close();*/
    }
}
