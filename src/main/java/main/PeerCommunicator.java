package main;

import main.peer.HandShake;
import org.joou.UShort;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class PeerCommunicator {

//    public static void sendMessage(String peerIp, int peerTCPPort, Message message) throws IOException {
//        byte[] receiveData = new byte[1000];
//
//        communicate(peerIp, peerTCPPort, message., receiveData);
//    }

    public static HandShake sendMessage(String peerIp, UShort peerTCPPort, HandShake handShake) throws IOException {
        byte[] receiveData = new byte[1000];
        communicate(peerIp, peerTCPPort, HandShake.createPacketFromObject(handShake), receiveData);
        return HandShake.createObjectFromPacket(receiveData);
    }

    private static void communicate(String peerIp, UShort peerPort, byte[] messageToSend, byte[] messageWeReceive) throws IOException {
        // start communicating with the peer
        Socket clientSocket = new Socket(peerIp, peerPort.intValue());
        DataOutputStream os = new DataOutputStream(clientSocket.getOutputStream());

        os.write(messageToSend);

        // receive data in tcp
        clientSocket.getInputStream().read(messageWeReceive);
        clientSocket.close();
    }
}
