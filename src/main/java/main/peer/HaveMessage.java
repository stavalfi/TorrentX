package main.peer;

import java.nio.ByteBuffer;

public class HaveMessage extends Message {
    private static int length=5;
    private static byte messageId=4;
    /**
     * The payload is the zero-based index
     * of a piece that has just been successfully
     * downloaded and verified via the hash.
     * @param pieceIndex
     */
    public HaveMessage(int pieceIndex) {
        super(length, messageId, ByteBuffer.allocate(4).putInt(pieceIndex));
    }
}
