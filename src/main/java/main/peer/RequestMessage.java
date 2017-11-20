package main.peer;

import main.NaturalX.*;

import java.nio.ByteBuffer;

// The request message is fixed length, and is used to request a block.
public class RequestMessage extends Message {
    /**
     * The payload contains the following information: (in this order)
     *
     * @param index  integer (4 bytes) specifying the zero-based piece index.
     * @param begin  integer (4 bytes) specifying the zero-based byte offset within the piece.
     * @param length integer (4 bytes) specifying the requested length.
     */
    public RequestMessage(int index, int begin, int length) {
        super(13, 6,
                ByteBuffer.allocate(12)
                        .put(new Natural4(index).buffer().array())
                        .put(new Natural4(begin).buffer().array())
                        .put(new Natural4(length).buffer().array())
        );
    }
}
