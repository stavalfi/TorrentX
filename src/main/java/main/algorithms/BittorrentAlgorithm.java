package main.algorithms;

import main.downloader.TorrentPieceChanged;
import reactor.core.publisher.Flux;

public interface BittorrentAlgorithm {
    Flux<TorrentPieceChanged> startDownloadFlux();

    Flux<TorrentPieceChanged> startUploadingFlux();
}
