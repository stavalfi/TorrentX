package main.peer.peerMessages;

import main.peer.Peer;
import main.peer.PeersCommunicator;

import java.nio.ByteBuffer;

public class KeepAliveMessage extends PeerMessage {
    private static final int length = 0;
    private static final byte messageId = 10;

    public KeepAliveMessage(PeersCommunicator peersCommunicator,Peer from, Peer to) {
        super(peersCommunicator, to, from, length, messageId, ByteBuffer.allocate(0).array());
    }

    public KeepAliveMessage(PeersCommunicator peersCommunicator, Peer from, Peer to, byte[] peerMessage) {
        super(peersCommunicator, to, peerMessage, from);
    }
    @Override
    public String toString() {
        return "KeepAliveMessage{} " + super.toString();
    }
}