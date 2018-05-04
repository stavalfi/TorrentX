package main.peer.peerMessages;

import main.file.system.BlocksAllocatorImpl;
import main.peer.Peer;

import java.nio.ByteBuffer;

// The request message is fixed length, and is used to request a block.
public class RequestMessage extends PeerMessage {
    private static int length = 13;
    private static final byte messageId = 6;
    private int index;
    private int begin;
    private int blockLength;

    /**
     * The payload contains the following information: (in this order)
     *
     * @param index       integer (4 bytes) specifying the zero-based piece index.
     * @param begin       integer (4 bytes) specifying the zero-based byte offset within the piece.
     * @param blockLength integer (4 bytes) specifying the requested length.
     */
    public RequestMessage(Peer from, Peer to, int index, int begin, int blockLength) {
        super(to, from);

        assert 0 <= index;
        assert 0 <= begin;
        assert 0 <= blockLength;

        this.index = index;
        this.begin = begin;
        this.blockLength = blockLength;
    }

    // TODO: call this method from the constructor of RequestMessage class.
    public static RequestMessage fixRequestMessage(RequestMessage requestMessage, long pieceLength) {
        // TODO: piece length is integer and not long.
        // Notes:
        // (1) piece size can't be more than Integer.MAX_VALUE because the protocol allow me to request
        // even the last byte of a piece and if the piece size is more than Integer.MAX_VALUE,
        // than I can't spacify the "begin" of that request using 4 bytes.
        // (2) in the tests, a block size is bounded to the allocatedBlock size and it's integer so
        // we can't create a request for a block larger than Integer.MAX_VALUE. so for both reasons,
        // a request of a block can't be more than Integer.MAX_VALUE.
        // TODO: uncomment the following line and fix this compilation error because the above TODO.
        //int newBegin = Math.min(requestMessage.getBegin(), pieceLength);
        int maxAllocatedBlockSize = BlocksAllocatorImpl.getInstance().getBlockLength();
        int newBlockLength = Math.min(maxAllocatedBlockSize, requestMessage.getBlockLength());

        // is requestMessage.getBegin() + newBlockLength overlaps with the range of this piece?
        if (pieceLength < requestMessage.getBegin() + newBlockLength) {
            // (1) newBlockLength <= maxAllocatedBlockSize
            // (1) -> (2) pieceLength - requestMessage.getBegin() < newBlockLength <= maxAllocatedBlockSize <= Integer.MAX_VALUE
            // (2) -> (3) pieceLength - requestMessage.getBegin() < Integer.MAX_VALUE
            newBlockLength = (int) (pieceLength - requestMessage.getBegin());
            // (4) ->  newBlockLength = (pieceLength - requestMessage.getBegin()) <= pieceLength
            // (4) -> (5) -> newBlockLength <= pieceLength
        }

        return new RequestMessage(requestMessage.getFrom(), requestMessage.getTo(),
                requestMessage.getIndex(), requestMessage.getBegin(), newBlockLength);
    }

    @Override
    public byte getMessageId() {
        return messageId;
    }

    @Override
    public int getMessageLength() {
        return length;
    }

    @Override
    public byte[] getMessagePayload() {
        return ByteBuffer.allocate(12)
                .putInt(this.index)
                .putInt(this.begin)
                .putInt(this.blockLength)
                .array();
    }

    public int getIndex() {
        return this.index;
    }

    public int getBegin() {
        return this.begin;
    }

    public int getBlockLength() {
        return this.blockLength;
    }

    @Override
    public String toString() {
        return "RequestMessage{" +
                "index=" + index +
                ", begin=" + begin +
                ", blockLength=" + blockLength +
                "} " + super.toString();
    }
}