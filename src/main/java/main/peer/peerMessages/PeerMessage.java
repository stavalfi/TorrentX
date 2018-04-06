package main.peer.peerMessages;

import main.peer.Peer;

import java.nio.ByteBuffer;

public abstract class PeerMessage implements Comparable<PeerMessage> {

    private final Peer from;
    private final Peer to;
    private final int length; // 4 bytes - the length in bytes of sizeof(messageId) + sizeof(payload)
    private final byte messageId; // 1 byte
    private final byte[] payload;

    PeerMessage(Peer to, Peer from, int length, byte messageId, byte[] payload) {
        this.length = length;
        this.messageId = messageId;
        this.payload = payload;
        this.from = from;
        this.to = to;
    }

    PeerMessage(Peer to, byte[] peerMessage, Peer from) {
        this.from = from;
        this.to = to;
        ByteBuffer buffer = ByteBuffer.wrap(peerMessage);
        this.length = buffer.getInt();
        if (this.length > 0) {
            this.messageId = buffer.get();
            int sizeOfPayload = this.length - 1;// this.length - sizeof(messageId)==this.length - 1
            this.payload = new byte[sizeOfPayload];
            buffer.get(this.payload);
        } else {
            this.messageId = 10; // it's KeepAlive message
            this.payload = new byte[0];
        }
    }

    public int getMessageLength() {
        // int length; // 4 bytes - the length in bytes of sizeof(messageId) + sizeof(payload)
        // byte messageId; // 1 byte
        // byte[] payload;
        return 5 + this.payload.length;
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
        // when receiving a peerMessage,
        // I first check what is the value of "length".
        // if length == 0 then I don't read any more bytes.
        // so there is no reason to send dummy bytes.
        if (this.length > 0) {
            buffer.put(this.messageId);
            buffer.put(this.payload);
        }

        return buffer.array();
    }

    public static int getMessageId(byte[] peerMessage) {
        ByteBuffer buffer = ByteBuffer.wrap(peerMessage);
        int length = buffer.getInt();
        if (length == 0)
            return 10; // it's KeepAlive message;
        return buffer.get();
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

    public Peer getFrom() {
        return from;
    }

    public Peer getTo() {
        return to;
    }

    @Override
    public String toString() {
        return "PeerMessage{" +
                "from=" + from +
                ", to=" + to +
                ", messageId=" + messageId +
                '}';
    }
}
