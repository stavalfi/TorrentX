package main.peer;

import main.Peer;

import java.nio.ByteBuffer;

public class UnchokeMessage extends PeerMessage {
    private static final int length = 1;
    private static final byte messageId = 1;

    /**
     * The unchoke message is fixed-length and has no payload.
     */
    public UnchokeMessage(Peer from, Peer to) {
        super(from, to, length, messageId, ByteBuffer.allocate(0).array());
    }

    public UnchokeMessage(Peer from, Peer to, byte[] peerMessage) {
        super(from, to, peerMessage);
    }
}
