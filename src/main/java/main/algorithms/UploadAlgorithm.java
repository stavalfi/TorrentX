package main.algorithms;

import reactor.core.publisher.Flux;

public interface UploadAlgorithm {
    Flux<PieceEvent> getUploadedBlocks$();
}
