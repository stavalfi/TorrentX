package main.peer;

import java.nio.ByteBuffer;

public class IsAliveMessage extends Message {
    private static int length=0;
    private static byte messageId=0;
    public IsAliveMessage() {
        super(length, messageId, ByteBuffer.allocate(0).array());
    }
}
