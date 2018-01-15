package main.peer;

import java.nio.ByteBuffer;

public class CancelMessage extends PeerMessage {
    private static int length=13;
    private static final byte messageId=7;
    public CancelMessage(int index, int begin, int length) {
        super(length, messageId, ByteBuffer.allocate(12).putInt(index)
                                                        .putInt(begin)
                                                        .putInt(length).array());
    }
}
