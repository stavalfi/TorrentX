package main.peer;

import java.nio.ByteBuffer;

public class InterestedMessage extends PeerMessage {
    private static final int length=1;
    private static final byte messageId=2;
    /**
     * The interested message is fixed-length and has no payload.
     */
    public InterestedMessage() {
        super(length,messageId, ByteBuffer.allocate(0).array());
    }
}
