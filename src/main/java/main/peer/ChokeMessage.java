package main.peer;

import java.nio.ByteBuffer;

public class ChokeMessage extends Message {
    private static final int length=1;
    private static final byte messageId=0;
    /**
     * The choke message is fixed-length and has no payload.
     */
    public ChokeMessage() {
        super(length,messageId,ByteBuffer.allocate(0).array());
    }
}
