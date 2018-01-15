package main.peer;

import java.nio.ByteBuffer;

public abstract class PeerMessage {

    private final int length; // 4 bytes - the length in bytes of byte-size(messageId) + byte-size(payload)
    private final byte messageId; // 1 byte
    private final byte[] payload;

    public PeerMessage(int length, byte messageId, byte[] payload) {
        this.length = length;
        this.messageId = messageId;
        this.payload = payload;
    }

    public byte[] createPacketFromObject() {
        ByteBuffer buffer = ByteBuffer.allocate(4 + this.length);

        buffer.putInt(this.length);
        buffer.put(this.messageId);
        buffer.put(this.payload);

        return buffer.array();
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
