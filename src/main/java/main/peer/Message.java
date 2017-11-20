package main.peer;

import main.NaturalX.*;

import java.nio.ByteBuffer;


public abstract class Message {

    private Natural4 length; // 4 bytes - the length in bytes of byte-size(massageId) + byte-size(payload)
    private Natural1 massageId; // 1 byte
    private ByteBuffer payload; // length - 1 bytes

    public Message(int length, int massageId, ByteBuffer payload) {
        this.length = new Natural4(length);
        this.massageId = new Natural1(massageId);
        this.payload = ByteBuffer.allocate(length - 1);
    }

    public Message(int length, int massageId, byte[] bytes) {
        this.length = new Natural4(length);
        this.massageId = new Natural1(massageId);
        this.payload = ByteBuffer.wrap(bytes);

        assert bytes.length == length - 1;
    }

    public int getLength() {
        return (int) this.length.getNumber();
    }

    public int getMassageId() {
        return (int) this.massageId.getNumber();
    }

    public byte[] getPayload() {
        return this.payload.asReadOnlyBuffer().array();
    }
}
