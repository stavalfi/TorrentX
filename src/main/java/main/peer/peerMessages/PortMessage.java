package main.peer.peerMessages;

import main.peer.Peer;
import main.peer.PeersCommunicator;

import java.nio.ByteBuffer;

public class PortMessage extends PeerMessage {
    private static final int length = 3;
    private static final byte messageId = 9;

    public PortMessage(PeersCommunicator peersCommunicator, Peer from, Peer to, short listenPort) {
        super(peersCommunicator, to, from, length, messageId, ByteBuffer.allocate(2).putShort(listenPort).array());
    }
    public PortMessage(PeersCommunicator peersCommunicator,Peer from, Peer to,byte[] peerMessage) {
        super(peersCommunicator, to, peerMessage, from);
    }
    @Override
    public String toString() {
        return "PortMessage{} " + super.toString();
    }
}
