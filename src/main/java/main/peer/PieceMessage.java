package main.peer;

import main.NaturalX;

import java.nio.ByteBuffer;

public class PieceMessage extends Message {
    /**
     * The payload contains the following information: (by this order)
     *
     * @param index integer specifying the zero-based piece index
     * @param begin integer specifying the zero-based byte offset within the piece
     * @param block block of data, which is a subset of the piece specified by index.
     */
    public PieceMessage(int index, int begin, byte[] block) {
        super(9 + block.length, 7,
                ByteBuffer.allocate(4 + 4 + block.length)
                        .put(new NaturalX.Natural4(index).buffer())
                        .put(new NaturalX.Natural4(begin).buffer())
                        .put(block)
        );
    }
}
