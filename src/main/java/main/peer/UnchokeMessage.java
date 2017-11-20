package main.peer;

import java.nio.ByteBuffer;

public class UnchokeMessage extends Message {
    /**
     * The unchoke message is fixed-length and has no payload.
     */
    public UnchokeMessage() {
        super(1,1,ByteBuffer.allocate(0));
    }
}
