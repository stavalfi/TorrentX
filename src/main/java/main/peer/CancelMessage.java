package main.peer;

import java.nio.ByteBuffer;

import static org.joou.Unsigned.ubyte;
import static org.joou.Unsigned.uint;

public class CancelMessage extends Message {
    private static int length=13;
    private static byte messageId=7;
    public CancelMessage(int index, int begin, int length) {
        super(length, messageId, ByteBuffer.allocate(12).putInt(index)
                                                        .putInt(begin)
                                                        .putInt(length));
    }
}
