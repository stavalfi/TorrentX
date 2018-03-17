package main.peer.peerMessages;

import main.peer.Peer;

import java.nio.ByteBuffer;

public class UnchokeMessage extends PeerMessage {
    private static final int length = 1;
    private static final byte messageId = 1;

    /**
     * The unchoke message is fixed-length and has no payload.
     */
    public UnchokeMessage(Peer from, Peer to) {
        super(to, from, length, messageId, ByteBuffer.allocate(0).array());
    }

    public UnchokeMessage(Peer from, Peer to, byte[] peerMessage) {
        super(to, peerMessage, from);
    }

    @Override
    public String toString() {
        return "UnchokeMessage{} " + super.toString();
    }
}