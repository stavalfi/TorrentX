package main.algorithms;

import main.downloader.TorrentPieceChanged;
import reactor.core.publisher.Flux;

public interface BittorrentAlgorithm {
    Flux<Integer> startDownloadFlux();

    Flux<TorrentPieceChanged> startUploadingFlux();

    Flux<Integer> getNotifyAboutCompletedPieceFlux();
}
