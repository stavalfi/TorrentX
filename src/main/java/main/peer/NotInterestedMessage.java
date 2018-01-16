package main.peer;

import main.Peer;

import java.nio.ByteBuffer;

public class NotInterestedMessage extends PeerMessage {
    private static final int length=1;
    private static final byte messageId=3;
    /**
     * The not interested message is fixed-length and has no payload.
     */
    public NotInterestedMessage(Peer from, Peer to) {
        super(from, to, length,messageId, ByteBuffer.allocate(0).array());
    }
    public NotInterestedMessage(Peer from, Peer to,byte[] peerMessage) {
        super(from, to, peerMessage);
    }
}