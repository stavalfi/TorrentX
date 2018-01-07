package main.peer;

import java.nio.ByteBuffer;

import static org.joou.Unsigned.ubyte;
import static org.joou.Unsigned.uint;

public class InterestedMessage extends Message {
    private static int length=1;
    private static byte messageId=2;
    /**
     * The interested message is fixed-length and has no payload.
     */
    public InterestedMessage() {
        super(length,messageId, ByteBuffer.allocate(0).array());
    }
}
