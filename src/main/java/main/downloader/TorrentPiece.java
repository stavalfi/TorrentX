package main.downloader;

public class TorrentPiece {
    private int pieceIndex;
    private String pieceHashCode;
    private TorrentPieceStatus torrentPieceStatus;

    public TorrentPiece(int pieceIndex, String pieceHashCode, TorrentPieceStatus torrentPieceStatus) {
        this.pieceIndex = pieceIndex;
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
}
