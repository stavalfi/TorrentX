package main.file.system;

import main.downloader.TorrentPieceChanged;
import main.peer.peerMessages.PieceMessage;
import reactor.core.publisher.Flux;

public interface Downloader {
    Flux<TorrentPieceChanged> downloadAsync(Flux<PieceMessage> peerResponsesFlux);
}
