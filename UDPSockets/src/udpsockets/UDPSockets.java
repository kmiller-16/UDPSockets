/*
 UDP Sockets Example
 */
package udpsockets;
//Imports for server side socket
import java.io.IOException; 
import java.net.DatagramPacket; 
import java.net.DatagramSocket; 
import java.net.InetAddress; 
import java.net.SocketException; 
/**
 */
public class UDPSockets {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws IOException {
        //socket will listen at port 5432
       DatagramSocket uSocket = new DatagramSocket(5432);
       byte[] receiver = new byte[65535];
       
       DatagramPacket dPacket = null;
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
    
}
