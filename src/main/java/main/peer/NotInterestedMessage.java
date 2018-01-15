package main.peer;

import java.nio.ByteBuffer;

public class NotInterestedMessage extends PeerMessage {
    private static final int length=1;
    private static final byte messageId=3;
    /**
     * The not interested message is fixed-length and has no payload.
     */
    public NotInterestedMessage() {
        super(length,messageId, ByteBuffer.allocate(0).array());
    }
}