package main.algorithms;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface PiecesDownloader {
    Mono<Integer> downloadPieceMono(int pieceIndex);

    Flux<Integer> getDownloadedPiecesFlux();
}
