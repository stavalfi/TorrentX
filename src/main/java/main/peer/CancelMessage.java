package main.peer;

import main.NaturalX;

import java.nio.ByteBuffer;

public class CancelMessage extends Message {
    public CancelMessage(int index, int begin, int length) {
        super(13, 7,
                ByteBuffer.allocate(12)
                        .put(new NaturalX.Natural4(index).buffer())
                        .put(new NaturalX.Natural4(begin).buffer())
                        .put(new NaturalX.Natural4(length).buffer())
        );
    }
}
