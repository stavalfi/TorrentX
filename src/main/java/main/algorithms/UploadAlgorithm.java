package main.algorithms;

import main.downloader.TorrentPieceChanged;
import reactor.core.publisher.Flux;

public interface UploadAlgorithm {
    Flux<TorrentPieceChanged> getUploadedBlocksFlux();
}
