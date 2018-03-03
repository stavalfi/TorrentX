package main.downloader;

import main.file.ActiveTorrent;
import main.peer.peerMessages.PieceMessage;
import reactor.core.publisher.Flux;

public class Downloader {

    private ActiveTorrent activeTorrent;

    public Downloader(ActiveTorrent activeTorrent) {
        this.activeTorrent = activeTorrent;
    }

    public ActiveTorrent getActiveTorrent() {
        return activeTorrent;
    }

    public Flux<Integer> downloadAsync(Flux<PieceMessage> peerResponsesFlux) {
        return Flux.never();
    }
}
