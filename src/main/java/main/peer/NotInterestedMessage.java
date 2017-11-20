package main.peer;

import java.nio.ByteBuffer;

public class NotInterestedMessage extends Message {
    /**
     * The not interested message is fixed-length and has no payload.
     */
    public NotInterestedMessage() {
        super(1,3, ByteBuffer.allocate(0));
    }
}