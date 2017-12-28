package main.peer;

import java.nio.ByteBuffer;

public class NotInterestedMessage extends Message {
    private static int length=1;
    private static byte messageId=3;
    /**
     * The not interested message is fixed-length and has no payload.
     */
    public NotInterestedMessage() {
        super(length,messageId, ByteBuffer.allocate(0));
    }
}