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
        super(to, from);
        this.pieceIndex = pieceIndex;
    }

    @Override
    public byte getMessageId() {
        return messageId;
    }

    @Override
    public int getMessageLength() {
        return length;
    }

    @Override
    public byte[] getMessagePayload() {
        return ByteBuffer.allocate(4)
                .putInt(this.pieceIndex)
                .array();
    }

    public int getPieceIndex() {
        return this.pieceIndex;
    }


    @Override
    public String toString() {
        return "HaveMessage{} " + super.toString();
    }
}