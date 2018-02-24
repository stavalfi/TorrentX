package main.downloader;

public class TorrentPiece {
    private final String pieceHashCode;
    private final TorrentPieceStatus torrentPieceStatus;

    public TorrentPiece(String pieceHashCode, TorrentPieceStatus torrentPieceStatus) {
        this.pieceHashCode = pieceHashCode;
        this.torrentPieceStatus = torrentPieceStatus;
    }

    public String getPieceHashCode() {
        return pieceHashCode;
    }

    public TorrentPieceStatus getTorrentPieceStatus() {
        return torrentPieceStatus;
    }
}
