package main.peer;

import java.nio.ByteBuffer;

public class UnchokeMessage extends PeerMessage {
    private static final int length=1;
    private static final byte messageId=1;
    /**
     * The unchoke message is fixed-length and has no payload.
     */
    public UnchokeMessage() {
        super(length,messageId,ByteBuffer.allocate(0).array());
    }
}
