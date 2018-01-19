package main;


import main.peer.PeerMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

public class PeersCommunicator {
    private static Logger logger = LoggerFactory.getLogger(PeersCommunicator.class);

    private Peer peer;
    private Socket peerSocket;

    public PeersCommunicator(Peer peer, Socket peerSocket) {
        assert peerSocket != null;

        this.peer = peer;
        this.peerSocket = peerSocket;
    }

    /**
     * @param peerMessage
     * @return an hot flux!
     */
    public Flux<PeerMessage> send(PeerMessage peerMessage) {
        return Flux.error(new Exception());
    }

    /**
     * @return an hot flux!
     */
    public Flux<PeerMessage> receive() {
        return Flux.error(new Exception());
    }

    public Peer getPeer() {
        return peer;
    }

    public void closeConnection() {

    }


    //
//        public static void sendMessage(String peerIp, int peerTCPPort, Message message) throws IOException {
//        byte[] receiveData = new byte[1000];
//
//        communicate(peerIp, peerTCPPort, message.createPacketFromObject(), receiveData);
//    }
//
//    public static HandShake sendMessage(String peerIp, int peerTCPPort, HandShake handShake) throws IOException {
//        logger.debug("sending handshake: " + handShake.toString());
//        byte[] receiveData = new byte[1000];
//        communicate(peerIp, peerTCPPort, HandShake.createPacketFromObject(handShake), receiveData);
//        return HandShake.createObjectFromPacket(receiveData);
//    }
//
//    private static void communicate(String peerIp, int peerTCPPort, byte[] messageToSend, byte[] messageWeReceive) throws IOException {
//        // start communicating with the peer
//        Socket clientSocket = new Socket(peerIp, peerTCPPort);
//        DataOutputStream os = new DataOutputStream(clientSocket.getOutputStream());
//
//        clientSocket.
//        os.write(messageToSend);
//
//        // receive data in tcp
//        clientSocket.getInputStream().read(messageWeReceive);
//        clientSocket.close();
//    }
//
    public static void main(String argv[]) throws Exception {
        String clientSentence;
        String capitalizedSentence;
        ServerSocket welcomeSocket = new ServerSocket(6789);

        while (true) {
            Socket connectionSocket = welcomeSocket.accept();
            BufferedReader inFromClient =
                    new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
            DataOutputStream outToClient = new DataOutputStream(connectionSocket.getOutputStream());
            clientSentence = inFromClient.readLine();
            System.out.println("Received: " + clientSentence);
            capitalizedSentence = clientSentence.toUpperCase() + '\n';
            outToClient.writeBytes(capitalizedSentence);
        }
    }
}
