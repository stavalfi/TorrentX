package main.file.system;

import main.TorrentInfo;
import main.downloader.TorrentPieceChanged;
import main.peer.peerMessages.PieceMessage;
import reactor.core.publisher.Flux;

public class DownloaderImpl implements Downloader {
    private ActiveTorrent activeTorrent;
    private final Flux<PieceMessage> peerResponsesFlux;

    public DownloaderImpl(ActiveTorrent activeTorrent,
                          Flux<PieceMessage> peerResponsesFlux) {
        this.activeTorrent = activeTorrent;
        this.peerResponsesFlux = peerResponsesFlux;
    }

    public TorrentInfo getTorrentInfo() {
        return this.activeTorrent;
    }

    public Flux<TorrentPieceChanged> downloadAsync(Flux<PieceMessage> peerResponsesFlux) {
        return Flux.never();
    }
}
