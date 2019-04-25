/*
 UDP Sockets Example
 */
package udpsockets;
//Imports for server side socket
import java.io.BufferedReader;
import java.io.IOException; 
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.DatagramPacket; 
import java.net.DatagramSocket; 
import java.net.InetAddress; 
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException; 
import java.util.Scanner;
/**
 */
public class UDPSockets {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws IOException {
        runTCP();
        runUDP();
    }
    public static StringBuilder data(byte[] a) 
    { 
        if (a == null) 
            return null; 
        StringBuilder ret = new StringBuilder(); 
        int i = 0; 
        while (a[i] != 0) 
        { 
            ret.append((char) a[i]); 
            i++; 
        } 
        return ret; 
    } 
    public static void runUDP() throws IOException{
         //socket will listen at port 5432
       DatagramSocket uSocket = new DatagramSocket(5432);
       byte[] receiver = new byte[65535];
       
       DatagramPacket dPacket = null;
       System.out.println("UDP server starting up...");
       while(true){
           dPacket = new DatagramPacket(receiver, receiver.length);
           
           uSocket.receive(dPacket);
           
           System.out.println("Message from client: " + data(receiver));
           
           if(data(receiver).toString().equals("bye")){
               System.out.println("Client closed connection... goodbye!");
               break;
           }
           receiver = new byte[65535];
       }
       uSocket.close();
    }
    public static void runTCP() throws IOException{
        ServerSocket ss=new ServerSocket(5000);
        Socket sk=ss.accept();
        BufferedReader cin= new BufferedReader (new InputStreamReader(sk.getInputStream()));
        PrintStream cout=new PrintStream(sk.getOutputStream());
        BufferedReader stdin=new BufferedReader (new InputStreamReader(System.in));
        String s;
        Scanner sc=new Scanner(System.in);
        while (true)
        {
            s=cin.readLine();
            System.out.print("Client : "+s+"\n");
            System.out.print("Server : ");
            s=sc.nextLine();
            //s=sout.println(s);
            if (s.equalsIgnoreCase("bye"))
            {
                cout.println("Bye");
                System.out.println("Connection ended by server");
                break;
            }
            cout.println(s);
        }
        ss.close();
        sk.close();
        cin.close();
        cout.close();
        stdin.close();
    }
}
