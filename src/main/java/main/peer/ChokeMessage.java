package main.peer;

import java.nio.ByteBuffer;

public class ChokeMessage extends Message {
    /**
     * The choke message is fixed-length and has no payload.
     */
    public ChokeMessage() {
        super(1,0,ByteBuffer.allocate(0));
    }
}
