package main.peer;

import main.Peer;

import java.nio.ByteBuffer;

// The request message is fixed length, and is used to request a block.
public class RequestMessage extends PeerMessage {
    private static int length=13;
    private static final byte messageId=6;
    /**
     * The payload contains the following information: (in this order)
     *
     * @param index  integer (4 bytes) specifying the zero-based piece index.
     * @param begin  integer (4 bytes) specifying the zero-based byte offset within the piece.
     * @param length integer (4 bytes) specifying the requested length.
     */
    public RequestMessage(Peer from, Peer to, int index, int begin, int length) {
        super(from, to, length, messageId,ByteBuffer.allocate(12)
                .putInt(index)
                .putInt(begin)
                .putInt(length).array());
    }
    public RequestMessage(Peer from, Peer to,byte[] peerMessage) {
        super(from, to, peerMessage);
    }
}
