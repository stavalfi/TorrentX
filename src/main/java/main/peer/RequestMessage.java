package main.peer;

import java.nio.ByteBuffer;

import static org.joou.Unsigned.ubyte;
import static org.joou.Unsigned.uint;

// The request message is fixed length, and is used to request a block.
public class RequestMessage extends Message {
    private static int length=13;
    private static byte messageId=6;
    /**
     * The payload contains the following information: (in this order)
     *
     * @param index  integer (4 bytes) specifying the zero-based piece index.
     * @param begin  integer (4 bytes) specifying the zero-based byte offset within the piece.
     * @param length integer (4 bytes) specifying the requested length.
     */
    public RequestMessage(int index, int begin, int length) {
        super(length, messageId,ByteBuffer.allocate(12)
                .putInt(index)
                .putInt(begin)
                .putInt(length));
    }
}
