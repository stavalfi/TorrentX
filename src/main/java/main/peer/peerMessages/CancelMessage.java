package main.peer.peerMessages;

import main.peer.Peer;

import java.nio.ByteBuffer;

public class CancelMessage extends PeerMessage {
    private static int length = 13;
    private static final byte messageId = 8;

    private int index;
    private int begin;
    private int blockLength;

    public CancelMessage(Peer from, Peer to, int index, int begin, int blockLength) {
        super(to, from);
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
        return index;
    }

    public int getBegin() {
        return begin;
    }

    public int getBlockLength() {
        return blockLength;
    }

    @Override
    public String toString() {
        return "CancelMessage{} " + super.toString();
    }
}