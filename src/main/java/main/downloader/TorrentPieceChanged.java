package main.downloader;

import main.peer.peerMessages.PieceMessage;

public class TorrentPieceChanged {
    private PieceMessage receivedPiece;
    private TorrentPieceStatus torrentPieceStatus;

    public TorrentPieceChanged(TorrentPieceStatus torrentPieceStatus,
                               PieceMessage receivedPiece) {
        this.receivedPiece = receivedPiece;
        this.torrentPieceStatus = torrentPieceStatus;
    }

    public TorrentPieceStatus getTorrentPieceStatus() {
        return torrentPieceStatus;
    }


    public PieceMessage getReceivedPiece() {
        return receivedPiece;
    }

    @Override
    public String toString() {
        return "TorrentPieceChanged{" +
                " receivedPiece=" + receivedPiece +
                ", torrentPieceStatus=" + torrentPieceStatus +
                '}';
    }
}
