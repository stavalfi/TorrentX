package main.downloader;

import main.peer.peerMessages.PieceMessage;
import reactor.core.publisher.Flux;

public class Downloader {

    private String torrentInfoHash;

    public Downloader(String torrentInfoHash) {
        this.torrentInfoHash = torrentInfoHash;
    }

    public String getTorrentInfoHash() {
        return torrentInfoHash;
    }

    public void downloadAsync(Flux<PieceMessage> peerResponsesFlux) {

    }
}
