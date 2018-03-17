package main.peer.peerMessages;

import main.peer.Peer;

import java.nio.ByteBuffer;

public class HaveMessage extends PeerMessage {
    private static final int length = 5;
    private static final byte messageId = 4;
    private final int pieceIndex;

    /**
     * The payload is the zero-based index
     * of a piece that has just been successfully
     * downloaded and verified via the hash.
     *
     * @param pieceIndex is the piece (not block, which is a piece inside a piece) we tell the other peers we have.
     */
    public HaveMessage(Peer from, Peer to, int pieceIndex) {
        super(to, from, length, messageId, ByteBuffer.allocate(4).putInt(pieceIndex).array());
        this.pieceIndex = pieceIndex;
    }

    public HaveMessage(Peer from, Peer to, byte[] peerMessage) {
        super(to, peerMessage, from);
        this.pieceIndex = ByteBuffer.wrap(this.getPayload()).getInt();
    }

    @Override
    public String toString() {
        return "HaveMessage{} " + super.toString();
    }

    public int getPieceIndex() {
        return pieceIndex;
    }
}