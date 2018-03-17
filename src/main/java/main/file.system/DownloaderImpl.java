package main.file.system;

import main.downloader.TorrentPiece;
import main.peer.peerMessages.PieceMessage;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class DownloaderImpl implements Downloader {
    private Mono<ActiveTorrents.ActiveTorrent> activeTorrentMono;
    private final Flux<PieceMessage> peerResponsesFlux;

    public DownloaderImpl(Mono<ActiveTorrents.ActiveTorrent> activeTorrentMono,
                          Flux<PieceMessage> peerResponsesFlux) {
        this.activeTorrentMono = activeTorrentMono;
        this.peerResponsesFlux = peerResponsesFlux;
    }

    public Mono<ActiveTorrents.ActiveTorrent> getActiveTorrentMono() {
        return activeTorrentMono;
    }

    public Flux<TorrentPiece> downloadAsync(Flux<PieceMessage> peerResponsesFlux) {
        return Flux.never();
    }
}
