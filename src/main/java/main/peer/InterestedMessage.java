package main.peer;

import main.Peer;

import java.nio.ByteBuffer;

public class InterestedMessage extends PeerMessage {
    private static final int length=1;
    private static final byte messageId=2;
    /**
     * The interested message is fixed-length and has no payload.
     */
    public InterestedMessage(Peer from, Peer to) {
        super(from, to, length,messageId, ByteBuffer.allocate(0).array());
    }
    public InterestedMessage(Peer from, Peer to,byte[] peerMessage) {
        super(from, to, peerMessage);
    }
}
