package main.peer;

import java.nio.ByteBuffer;

public class ChokeMessage extends Message {
    private static int length=1;
    private static byte messageId=0;
    /**
     * The choke message is fixed-length and has no payload.
     */
    public ChokeMessage() {
        super(length,messageId,ByteBuffer.allocate(0));
    }
}
