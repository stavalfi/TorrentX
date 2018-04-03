package main.downloader;

public class TorrentPieceChanged {
    private int pieceIndex;
    private String pieceHashCode;
    private TorrentPieceStatus torrentPieceStatus;

    public TorrentPieceChanged(int pieceIndex, String pieceHashCode, TorrentPieceStatus torrentPieceStatus) {
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

    @Override
    public String toString() {
        return "TorrentPieceChanged{" +
                "pieceIndex=" + pieceIndex +
                ", pieceHashCode='" + pieceHashCode + '\'' +
                ", torrentPieceStatus=" + torrentPieceStatus +
                '}';
    }
}
