package main.peer.peerMessages;

import main.peer.Peer;

import java.nio.ByteBuffer;

public class ChokeMessage extends PeerMessage {
    private static final int length = 1;
    private static final byte messageId = 0;

    /**
     * The choke message is fixed-length and has no payload.
     */
    public ChokeMessage(Peer from, Peer to) {
        super(to, from, length, messageId, ByteBuffer.allocate(0).array());
    }

    public ChokeMessage(Peer from, Peer to, byte[] peerMessage) {
        super(to, peerMessage, from);
    }
    @Override
    public String toString() {
        return "ChokeMessage{} " + super.toString();
    }
}