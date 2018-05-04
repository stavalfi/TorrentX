package main.peer.peerMessages;

import main.file.system.AllocatedBlock;
import main.file.system.BlocksAllocatorImpl;
import main.peer.Peer;
import main.peer.SendMessages;
import reactor.core.publisher.Mono;

import java.util.Objects;

public class PieceMessage extends PeerMessage {
    private static final byte messageId = 7;
    private int index;
    private int begin;
    // TODO: remove this property because we can't update this property in an object of AllocatedBlock.
    private int blockLength;
    private AllocatedBlock allocatedBlock;

    /**
     * The payload contains the following information: (by this order)
     *
     * @param index          integer specifying the zero-based piece index
     * @param begin          integer specifying the zero-based byte offset within the piece
     * @param allocatedBlock allocatedBlock of data, which is a subset of the piece specified by index.
     */
    public PieceMessage(Peer from, Peer to, int index, int begin,
                        int blockLength, AllocatedBlock allocatedBlock) {
        super(to, from);
        this.index = index;
        this.begin = begin;
        this.blockLength = blockLength;
        this.allocatedBlock = allocatedBlock;
    }


    // TODO: call this method from the constructor of PieceMessage class.
    public static PieceMessage fixPieceMessage(PieceMessage pieceMessage, long pieceLength) {
        // TODO: piece length is integer and not long.
        // Notes:
        // (1) piece size can't be more than Integer.MAX_VALUE because the protocol allow me to request
        // even the last byte of a piece and if the piece size is more than Integer.MAX_VALUE,
        // than I can't spacify the "begin" of that request using 4 bytes.
        // (2) in the tests, a block size is bounded to the allocatedBlock size and it's integer so
        // we can't create a request for a block larger than Integer.MAX_VALUE. so for both reasons,
        // a request of a block can't be more than Integer.MAX_VALUE.
        // TODO: uncomment the following line and fix this compilation error because the above TODO.
//        int newBegin = Math.min(pieceMessage.getBegin(), pieceLength);
        int maxAllocatedBlockSize = BlocksAllocatorImpl.getInstance().getBlockLength();
        int newBlockLength = Math.min(maxAllocatedBlockSize, pieceMessage.getBlockLength());

        // is pieceMessage.getBegin() + newBlockLength overlaps with the range of this piece?
        if (pieceLength < pieceMessage.getBegin() + newBlockLength) {
            // (1) newBlockLength <= maxAllocatedBlockSize
            // (1) -> (2) pieceLength - pieceMessage.getBegin() < newBlockLength <= maxAllocatedBlockSize <= Integer.MAX_VALUE
            // (2) -> (3) pieceLength - pieceMessage.getBegin() < Integer.MAX_VALUE
            newBlockLength = (int) (pieceLength - pieceMessage.getBegin());
            // (4) ->  newBlockLength = (pieceLength - pieceMessage.getBegin()) <= pieceLength
            // (4) -> (5) -> newBlockLength <= pieceLength
        }

        AllocatedBlock newAllocatedBlock = BlocksAllocatorImpl.getInstance()
                .updateLength(pieceMessage.getAllocatedBlock(), newBlockLength);
        return new PieceMessage(pieceMessage.getFrom(), pieceMessage.getTo(),
                pieceMessage.getIndex(), pieceMessage.getBegin(), newBlockLength, newAllocatedBlock);
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
        return messageIdLength + indexLength + beginLength + allocatedBlock.getActualLength() - allocatedBlock.getOffset();
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
                getBegin() == that.getBegin();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getIndex(), getBegin());
    }

    @Override
    public String toString() {
        return "PieceMessage{" +
                "index=" + index +
                ", begin=" + begin +
                ", allocatedBlock=" + allocatedBlock +
                "} " + super.toString();
    }

    public int getBlockLength() {
        return blockLength;
    }
}