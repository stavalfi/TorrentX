package main.peer;

import java.nio.ByteBuffer;

public class IsAliveMessage extends PeerMessage {
    private static final int length=0;
    private static final byte messageId=0;
    public IsAliveMessage() {
        super(length, messageId, ByteBuffer.allocate(0).array());
    }
    public IsAliveMessage(byte[] peerMessage) {
        super(peerMessage);
    }
}
