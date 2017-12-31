package main.peer;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.nio.ByteBuffer;

@Getter
@AllArgsConstructor
public abstract class Message {

    private final int length; // 4 bytes - the length in bytes of byte-size(messageId) + byte-size(payload)
    private final byte messageId; // 1 byte
    private final byte[] payload;

    public byte[] createPacketFromObject() {
        ByteBuffer buffer = ByteBuffer.allocate(4 + this.length);

        buffer.putInt(this.length);
        buffer.put(this.messageId);
        buffer.put(this.payload);

        return buffer.array();
    }
}
