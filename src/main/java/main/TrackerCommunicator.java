package main;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;



public class TrackerConnector {

    /**
     * To connect to a tracker, it must have a UDP packet with the following structure:
     *
     * Offset  Size            Name            Value
     * 0       64-bit integer  connection_id   0x41727101980
     * 8       32-bit integer  action          0 // connect
     * 12      32-bit integer  transaction_id  random (we decide)
     * 16
     *
     * The server must return us a UDP packet with the following structure:
     *
     * Offset  Size            Name            Value
     * 0       32-bit integer  action          0 // connect
     * 4       32-bit integer  transaction_id
     * 8       64-bit integer  connection_id
     * 16
     * @param ip or url of the tracker
     * @param port of the tracker
     * @throws IOException
     */
    public static void connect(String ip,int port) throws IOException {
        DatagramSocket clientSocket = new DatagramSocket();
        InetAddress IPAddress = InetAddress.getByName(ip);
        ByteBuffer sendData = ByteBuffer.allocate(128);
        sendData.putLong(0x41727101980L); // connection_id - can't change (64 bit)
        sendData.putInt(0); // action we want to perform - connecting with the server (32 bit)
        sendData.putInt(123456); // transaction_id - random int we make (32 bit)
        DatagramPacket sendPacket = new DatagramPacket(sendData.array(), sendData.capacity(), IPAddress, port);
        clientSocket.send(sendPacket);
        byte[] receiveData = new byte[1024]; // we need to receive 128 bytes but we may get more so let's assume we get more..
        DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
        clientSocket.receive(receivePacket);

        // test we successfully connected

        String modifiedSentence = new String(receivePacket.getData());
        ByteBuffer receiveData_analyze = ByteBuffer.wrap(receiveData);
        System.out.println("To SERVER:");
        System.out.println("action id: "+0);
        System.out.println("transaction_id: "+123456);
        System.out.println("connection_id from server: "+0x41727101980L);
        System.out.println("FROM SERVER:");
        System.out.println("action id: "+receiveData_analyze.getInt());
        System.out.println("transaction_id: "+receiveData_analyze.getInt());
        System.out.println("connection_id from server: "+receiveData_analyze.getLong());
        clientSocket.close();

    }
}
