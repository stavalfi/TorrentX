package main;

import main.peer.HandShake;
import main.peer.Message;
import java.nio.ByteBuffer;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

public class PeerCommunicator {

//    public static void sendMessage(String peerIp, int peerTCPPort, Message message) throws IOException {
//        byte[] receiveData = new byte[1000];
//
//        communicate(peerIp, peerTCPPort, message., receiveData);
//    }

    public static void sendMessage(String peerIp, int peerTCPPort, HandShake handShake) throws IOException {
        byte[] receiveData = new byte[1000];
        communicate(peerIp, peerTCPPort, HandShake.createPacketFromObject(handShake), receiveData);
    }

    private static void communicate(String peerIp, int peerPort, byte[] messageToSend, byte[] messageWeReceive) throws IOException {
        // start communicating with the peer
        System.out.println("start communicating with peer");
        Socket clientSocket = new Socket(peerIp, peerPort);
        System.out.println("successfuly communicated with peer");
        //af1f3dbc5d5baeaf83f812e06aa91bb7b55cce8
        DataOutputStream os = new DataOutputStream(clientSocket.getOutputStream());

        System.out.println("sending to peer...");
        os.write(messageToSend);
        System.out.println("sent to peer...");

        // receive data in tcp
        for (int i = 0; i < 1; i++) {
            System.out.println("waiting for response from peer...");
            System.out.println(clientSocket.getInputStream().read(messageWeReceive));
            HandShake handShake=HandShake.createObjectFromPacket(messageWeReceive);
            System.out.println(handShake);
            messageWeReceive=new byte[1000];
        }
        clientSocket.close();
    }

    public static void tcp_client() throws Exception {
        String sentence;
        String modifiedSentence;
        BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));
        Socket clientSocket = new Socket("5.29.96.159", 80);
        DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
        BufferedReader inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        sentence = inFromUser.readLine();
        outToServer.writeBytes(sentence + '\n');
        modifiedSentence = inFromServer.readLine();
        System.out.println("FROM SERVER: " + modifiedSentence);
        clientSocket.close();
    }
}
