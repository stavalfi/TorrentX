package main.peer;

import main.Peer;

import java.nio.ByteBuffer;

public class ChokeMessage extends PeerMessage {
    private static final int length = 1;
    private static final byte messageId = 0;

    /**
     * The choke message is fixed-length and has no payload.
     */
    public ChokeMessage(Peer from, Peer to) {
        super(from, to, length, messageId, ByteBuffer.allocate(0).array());
    }

    public ChokeMessage(Peer from, Peer to, byte[] peerMessage) {
        super(from, to, peerMessage);
    }
}
