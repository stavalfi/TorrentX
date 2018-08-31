package main.file.system.allocator.requests;

import main.file.system.allocator.AllocatorAction;
import main.peer.Peer;
import redux.store.Request;

public class CreatePieceMessageRequest extends Request<AllocatorAction> {
    private Peer from;
    private Peer to;
    private int index;
    private int begin;
    private int blockLength;
    private int pieceLength;

    public CreatePieceMessageRequest(Peer from, Peer to, int index, int begin, int blockLength, int pieceLength) {
        super(AllocatorAction.CREATE_PIECE_MESSAGE);
        this.from = from;
        this.to = to;
        this.index = index;
        this.begin = begin;
        this.blockLength = blockLength;
        this.pieceLength = pieceLength;
    }

    public Peer getFrom() {
        return from;
    }

    public Peer getTo() {
        return to;
    }

    public int getIndex() {
        return index;
    }

    public int getBegin() {
        return begin;
    }

    public int getBlockLength() {
        return blockLength;
    }

    public int getPieceLength() {
        return pieceLength;
    }

    @Override
    public String toString() {
        return super.toString() + " - CreatePieceMessageRequest{" +
                "from=" + from +
                ", to=" + to +
                ", index=" + index +
                ", begin=" + begin +
                ", blockLength=" + blockLength +
                ", pieceLength=" + pieceLength +
                '}';
    }
}
