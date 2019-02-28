/*
 */
package udpsockets;
//imports
import java.io.IOException; 
import java.net.DatagramPacket; 
import java.net.DatagramSocket; 
import java.net.InetAddress; 
import java.util.Scanner; 
/**

 */

public class UDPClient {
    public static void main(String args[]) throws IOException{
        Scanner scan = new Scanner(System.in);
        
        DatagramSocket dSocket = new DatagramSocket();
        InetAddress ip = InetAddress.getLocalHost();
        byte buffer[] = null;
        while(true){
            String input = scan.nextLine();
            buffer = input.getBytes();
            DatagramPacket sendMe =  new DatagramPacket(
                    buffer, buffer.length, ip, 5432);
            dSocket.send(sendMe);
            if(input.equals("bye")) break;
        }
    }
}
