package main.algorithms;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface PiecesDownloader {
    Flux<Integer> getDownloadedPieces$();
}
