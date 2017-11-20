package main.peer;

import java.nio.ByteBuffer;

public class InterestedMessage extends Message {
    /**
     * The interested message is fixed-length and has no payload.
     */
    public InterestedMessage() {
        super(1,2, ByteBuffer.allocate(0));
    }
}
