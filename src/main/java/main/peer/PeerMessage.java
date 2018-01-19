package main.peer;

import main.Peer;

import java.nio.ByteBuffer;

public abstract class PeerMessage implements Comparable<PeerMessage> {

    private final Peer from;
    private final Peer to;
    private final int length; // 4 bytes - the length in bytes of sizeof(messageId) + sizeof(payload)
    private final byte messageId; // 1 byte
    private final byte[] payload;

    public PeerMessage(Peer from, Peer to, int length, byte messageId, byte[] payload) {
        this.length = length;
        this.messageId = messageId;
        this.payload = payload;
        this.from = from;
        this.to = to;
    }

    public PeerMessage(Peer from, Peer to, byte[] peerMessage) {
        this.from = from;
        this.to = to;
        ByteBuffer buffer = ByteBuffer.wrap(peerMessage);
        this.length = buffer.getInt();
        this.messageId = buffer.get();
        int sizeOfPayload = this.length - 4;// this.length - sizeof(messageId)==this.length - 4
        this.payload = new byte[sizeOfPayload];
        buffer.get(this.payload);
    }

    @Override
    public int compareTo(PeerMessage peerMessage) {
        if (this.messageId > peerMessage.getMessageId())
            return 1;
        else if (this.messageId < peerMessage.getMessageId())
            return -1;
        return 0;
    }

    public byte[] createPacketFromObject() {
        ByteBuffer buffer = ByteBuffer.allocate(4 + this.length);

        buffer.putInt(this.length);
        buffer.put(this.messageId);
        buffer.put(this.payload);

        return buffer.array();
    }

    public static int getMessageId(byte[] peerMessage) {
        ByteBuffer buffer = ByteBuffer.wrap(peerMessage);
        int messageIdPosition = 4;
        return buffer.get(messageIdPosition);
    }

    public int getLength() {
        return length;
    }

    public byte getMessageId() {
        return messageId;
    }

    public byte[] getPayload() {
        return payload;
    }
}
