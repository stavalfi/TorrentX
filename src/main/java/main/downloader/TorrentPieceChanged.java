package main.downloader;

import main.peer.peerMessages.PieceMessage;

public class TorrentPieceChanged {
    private int pieceIndex;
    private String pieceHashCode;
    private PieceMessage receivedPiece;
    private TorrentPieceStatus torrentPieceStatus;

    public TorrentPieceChanged(int pieceIndex, String pieceHashCode, TorrentPieceStatus torrentPieceStatus,
                               PieceMessage receivedPiece) {
        this.pieceIndex = pieceIndex;
        this.receivedPiece = receivedPiece;
        this.pieceHashCode = pieceHashCode;
        this.torrentPieceStatus = torrentPieceStatus;
    }

    public String getPieceHashCode() {
        return pieceHashCode;
    }

    public TorrentPieceStatus getTorrentPieceStatus() {
        return torrentPieceStatus;
    }

    public int getPieceIndex() {
        return pieceIndex;
    }

    public PieceMessage getReceivedPiece() {
        return receivedPiece;
    }

    @Override
    public String toString() {
        return "TorrentPieceChanged{" +
                "pieceIndex=" + pieceIndex +
                ", pieceHashCode='" + pieceHashCode + '\'' +
                ", receivedPiece=" + receivedPiece +
                ", torrentPieceStatus=" + torrentPieceStatus +
                '}';
    }
}
