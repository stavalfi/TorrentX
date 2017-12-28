package main.peer;

import java.nio.ByteBuffer;

import static org.joou.Unsigned.uint;
import static org.joou.Unsigned.ushort;

public class PortMessage extends Message {
    private static int length=3;
    private static byte messageId=9;
    public PortMessage(short listenPort) {
        super(length, messageId, (ByteBuffer) ByteBuffer.allocate(2).putShort(listenPort));
    }
}
