package main.file.system;

import main.TorrentInfo;
import main.downloader.TorrentPieceChanged;
import main.peer.peerMessages.PieceMessage;
import reactor.core.publisher.Flux;

public class DownloaderImpl implements Downloader {
    private ActiveTorrent activeTorrent;
    private Flux<PieceMessage> peerResponsesFlux;
    private Flux<TorrentPieceChanged> torrentPieceChangedFlux;

    public DownloaderImpl(ActiveTorrent activeTorrent,
                          Flux<PieceMessage> peerResponsesFlux) {
        this.activeTorrent = activeTorrent;
        this.peerResponsesFlux = peerResponsesFlux;
        this.torrentPieceChangedFlux = this.peerResponsesFlux
                .flatMap(pieceMessage -> this.activeTorrent.writeBlock(pieceMessage));
    }

    public TorrentInfo getTorrentInfo() {
        return this.activeTorrent;
    }

    public Flux<TorrentPieceChanged> downloadAsync() {
        return this.torrentPieceChangedFlux;
    }
}
