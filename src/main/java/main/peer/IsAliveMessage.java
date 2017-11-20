package main.peer;

import java.nio.ByteBuffer;

public class IsAliveMessage extends Message {
    public IsAliveMessage() {
        super(0, 0, ByteBuffer.allocate(0));
    }
}
