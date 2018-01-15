package main.peer;

import java.nio.ByteBuffer;

public class PortMessage extends PeerMessage {
    private static final int length = 3;
    private static final byte messageId = 9;

    public PortMessage(short listenPort) {
        super(length, messageId, ByteBuffer.allocate(2).putShort(listenPort).array());
    }
}
