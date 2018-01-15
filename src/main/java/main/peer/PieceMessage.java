package main.peer;

import java.nio.ByteBuffer;

public class PieceMessage extends PeerMessage {
    private static final byte messageId=7;
    /**
     * The payload contains the following information: (by this order)
     *
     * @param index integer specifying the zero-based piece index
     * @param begin integer specifying the zero-based byte offset within the piece
     * @param block block of data, which is a subset of the piece specified by index.
     */
    public PieceMessage(int index, int begin, byte[] block) {
        super(9 + block.length, messageId, ByteBuffer.allocate(4 + 4 + block.length)
                                                            .putInt(index)
                                                            .putInt(begin)
                                                            .put(block).array());
    }
}
