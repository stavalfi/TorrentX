package main.peer.peerMessages;

import main.peer.Peer;

import java.nio.ByteBuffer;

public class InterestedMessage extends PeerMessage {
    private static final int length=1;
    private static final byte messageId=2;
    /**
     * The interested message is fixed-length and has no payload.
     */
    public InterestedMessage(Peer from, Peer to) {
        super(to, from, length,messageId, ByteBuffer.allocate(0).array());
    }
    public InterestedMessage(Peer from, Peer to,byte[] peerMessage) {
        super(to, peerMessage, from);
    }
    @Override
    public String toString() {
        return "InterestedMessage{} " + super.toString();
    }
}