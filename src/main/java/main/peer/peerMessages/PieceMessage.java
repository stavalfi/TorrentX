package main.peer.peerMessages;

import main.file.system.AllocatedBlock;
import main.peer.Peer;
import main.peer.SendMessages;
import reactor.core.publisher.Mono;

import java.util.Objects;

public class PieceMessage extends PeerMessage {
    private static final byte messageId = 7;
    private int index;
    private int begin;
    private AllocatedBlock allocatedBlock;

    /**
     * The payload contains the following information: (by this order)
     *
     * @param index          integer specifying the zero-based piece index
     * @param begin          integer specifying the zero-based byte offset within the piece
     * @param allocatedBlock allocatedBlock of data, which is a subset of the piece specified by index.
     */
    public PieceMessage(Peer from, Peer to, int index, int begin,
                        AllocatedBlock allocatedBlock, int pieceLength) {
        super(to, from);

        assert begin == fixBlockBegin(pieceLength, begin);
        assert allocatedBlock.getLength() ==
                fixBlockLength(pieceLength, begin, allocatedBlock.getLength(), allocatedBlock.getBlock().length);

        this.index = index;
        this.begin = begin;
        this.allocatedBlock = allocatedBlock;
    }

    public static int fixBlockBegin(int pieceLength, int oldBegin) {
        return Math.min(oldBegin, pieceLength - 1);
    }

    // TODO: we didn't take in account the offset of the AllocatedBlock!
    public static int fixBlockLength(int pieceLength, int begin, int oldBlockLength, int maxAllocatedBlockSize) {
        assert fixBlockBegin(pieceLength, begin) == begin;
        int newBlockLength = Math.min(maxAllocatedBlockSize, oldBlockLength);

        // is pieceMessage.getBegin() + newBlockLength overlaps with the range of this piece?
        if (pieceLength < begin + newBlockLength) {
            // (1) newBlockLength <= maxAllocatedBlockSize
            // (1) -> (2) pieceLength - pieceMessage.getBegin() < newBlockLength <= maxAllocatedBlockSize <= Integer.MAX_VALUE
            // (2) -> (3) pieceLength - pieceMessage.getBegin() < Integer.MAX_VALUE
            newBlockLength = (int) (pieceLength - begin);
            // (4) ->  newBlockLength = (pieceLength - pieceMessage.getBegin()) <= pieceLength
            // (4) -> (5) -> newBlockLength <= pieceLength
        }
        return newBlockLength;
    }

    @Override
    public Mono<SendMessages> sendMessage(SendMessages sendMessages) {
        return sendMessages.send(this);
    }

    @Override
    public byte getMessageId() {
        return messageId;
    }

    @Override
    public int getMessageLength() {
        int messageIdLength = 1,
                indexLength = 4,
                beginLength = 4;
        return messageIdLength + indexLength + beginLength + allocatedBlock.getLength() - allocatedBlock.getOffset();
    }

    @Override
    public byte[] getMessagePayload() {
        throw new UnsupportedOperationException("this class use AllocatedBlock class to represent " +
                "a allocatedBlock instead of representing it by byte[].");
    }

    public int getIndex() {
        return index;
    }

    public int getBegin() {
        return begin;
    }

    public AllocatedBlock getAllocatedBlock() {
        return this.allocatedBlock;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PieceMessage)) return false;
        PieceMessage that = (PieceMessage) o;
        return getIndex() == that.getIndex() &&
                getBegin() == that.getBegin() &&
                getAllocatedBlock().getLength() == that.getAllocatedBlock().getLength();
    }

    @Override
    public int hashCode() {

        return Objects.hash(getIndex(), getBegin(), getAllocatedBlock().getLength());
    }

    @Override
    public String toString() {
        return "PieceMessage{" +
                "index=" + index +
                ", begin=" + begin +
                ", allocatedBlock=" + allocatedBlock +
                "} " + super.toString();
    }
}