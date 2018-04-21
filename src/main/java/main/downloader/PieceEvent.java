package main.downloader;

import main.peer.peerMessages.PieceMessage;

public class PieceEvent {
    private PieceMessage receivedPiece;
    private TorrentPieceStatus torrentPieceStatus;

    public PieceEvent(TorrentPieceStatus torrentPieceStatus,
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
        return "PieceEvent{" +
                " receivedPiece=" + receivedPiece +
                ", torrentPieceStatus=" + torrentPieceStatus +
                '}';
    }
}
