package main.file.system;

import main.TorrentInfo;

public class PieceDownloadedAlreadyException extends Exception {
    public PieceDownloadedAlreadyException(TorrentInfo torrentInfo, int pieceIndex) {
        super("piece: " + pieceIndex + " can't be downloaded again because it already downloaded for torrent: " + torrentInfo);
    }
}
