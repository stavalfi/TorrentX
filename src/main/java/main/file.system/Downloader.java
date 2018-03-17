package main.file.system;

import main.downloader.TorrentPiece;
import main.peer.peerMessages.PieceMessage;
import reactor.core.publisher.Flux;

public interface Downloader {
    Flux<TorrentPiece> downloadAsync(Flux<PieceMessage> peerResponsesFlux);
}
