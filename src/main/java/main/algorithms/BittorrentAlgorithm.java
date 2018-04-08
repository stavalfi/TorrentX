package main.algorithms;

import main.downloader.TorrentPieceChanged;
import main.peer.peerMessages.RequestMessage;
import reactor.core.publisher.Flux;

public interface BittorrentAlgorithm {
    Flux<RequestMessage> startDownloadFlux();

    Flux<TorrentPieceChanged> startUploadingFlux();
}
