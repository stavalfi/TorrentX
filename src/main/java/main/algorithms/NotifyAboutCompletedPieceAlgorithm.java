package main.algorithms;

import reactor.core.publisher.Flux;

public interface NotifyAboutCompletedPieceAlgorithm {
    Flux<Integer> getNotifiedCompletedPiecesFlux();
}
