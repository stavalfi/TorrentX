package main.peer;

import java.nio.ByteBuffer;

public class UnchokeMessage extends Message {
    private static int length=1;
    private static byte messageId=1;
    /**
     * The unchoke message is fixed-length and has no payload.
     */
    public UnchokeMessage() {
        super(length,messageId,ByteBuffer.allocate(0).array());
    }
}
