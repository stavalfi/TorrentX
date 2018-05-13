package main.peer.peerMessages;

import main.peer.Peer;

import java.nio.ByteBuffer;

// The request message is fixed length, and is used to request a block.
public class RequestMessage extends PeerMessage {
    private static int length = 13;
    private static final byte messageId = 6;
    private int index;
    private int begin;
    private int blockLength;

    /**
     * The payload contains the following information: (in this order)
     *
     * @param index       integer (4 bytes) specifying the zero-based piece index.
     * @param begin       integer (4 bytes) specifying the zero-based byte offset within the piece.
     * @param blockLength integer (4 bytes) specifying the requested length.
     */
    public RequestMessage(Peer from, Peer to, int index, int begin, int blockLength) {
        super(to, from);

        assert 0 <= index;
        assert 0 <= begin;
        assert 0 <= blockLength;

        this.index = index;
        this.begin = begin;
        this.blockLength = blockLength;
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
        return ByteBuffer.allocate(12)
                .putInt(this.index)
                .putInt(this.begin)
                .putInt(this.blockLength)
                .array();
    }

    public int getIndex() {
        return this.index;
    }

    public int getBegin() {
        return this.begin;
    }

    public int getBlockLength() {
        return this.blockLength;
    }

    @Override
    public String toString() {
        return "RequestMessage{" +
                "index=" + index +
                ", begin=" + begin +
                ", blockLength=" + blockLength +
                "} " + super.toString();
    }
}