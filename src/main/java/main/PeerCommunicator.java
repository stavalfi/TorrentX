package main;

public class PeerCommunicator {

//    private static Logger logger = LoggerFactory.getLogger(PeerCommunicator.class);
//
//    public static void sendMessage(String peerIp, int peerTCPPort, Message message) throws IOException {
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
//        os.write(messageToSend);
//
//        // receive data in tcp
//        clientSocket.getInputStream().read(messageWeReceive);
//        clientSocket.close();
//    }
}
