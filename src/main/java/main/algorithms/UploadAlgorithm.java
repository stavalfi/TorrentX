package main.algorithms;

import main.downloader.PieceEvent;
import reactor.core.publisher.Flux;

public interface UploadAlgorithm {
    Flux<PieceEvent> getUploadedBlocksFlux();
}
