package main.peer;

import org.joou.UByte;
import org.joou.UInteger;

import java.nio.ByteBuffer;


public abstract class Message {

    private int length; // 4 bytes - the length in bytes of byte-size(massageId) + byte-size(payload)
    private byte massageId; // 1 byte
    private ByteBuffer payload; // length - 1 bytes

    public Message(int length, byte massageId, ByteBuffer payload) {
        this.length = length;
        this.massageId = massageId;
        this.payload = ByteBuffer.allocate(length - 1);
    }

    public Message(int length, byte massageId, byte[] payload) {
        this.length = length;
        this.massageId = massageId;
        this.payload = ByteBuffer.wrap(payload);
        assert payload.length == length - 1;
    }

    public int getLength() {
        return length;
    }

    public byte getMassageId() {
        return massageId;
    }

    public ByteBuffer getPayload() {
        return payload;
    }
}
