package main.peer;


import main.peer.peerMessages.PeerMessage;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.scheduler.Schedulers;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class PeersCommunicator {
    private Peer peer;
    private final Socket peerSocket;
    private final DataOutputStream dataOutputStream;
    private final Flux<PeerMessage> responses;

    public PeersCommunicator(Peer peer, Socket peerSocket) throws Exception {
        assert peerSocket != null;
        this.peer = peer;
        this.peerSocket = peerSocket;
        this.dataOutputStream = new DataOutputStream(this.peerSocket.getOutputStream());
        this.responses = waitForResponses(new DataInputStream(this.peerSocket.getInputStream()));
    }

    private Flux<PeerMessage> waitForResponses(DataInputStream dataInputStream) {
        return Flux.create((FluxSink<PeerMessage> sink) -> {
            while (true) {
                byte[] data = new byte[1000];
                try {
                    dataInputStream.read(data);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).publishOn(Schedulers.single());
    }

    public Flux<PeerMessage> send(PeerMessage peerMessage) {
        try {
            this.dataOutputStream.write(peerMessage.createPacketFromObject());
            return receive();
        } catch (IOException e) {
            try {
                closeConnection();
            } catch (IOException e1) {
                // TODO: do something better... it's a fatal problem with my design!!!S
                e1.printStackTrace();
            }
            return Flux.error(e);
        }
    }

    /**
     * @return an hot flux!
     */
    public Flux<PeerMessage> receive() {
        return this.responses;
    }

    public Peer getPeer() {
        return peer;
    }

    public void closeConnection() throws IOException {
        this.dataOutputStream.close();
        this.peerSocket.close();
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
