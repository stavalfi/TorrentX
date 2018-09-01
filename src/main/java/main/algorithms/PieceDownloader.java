package main.algorithms;

import main.peer.Link;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface PieceDownloader {
    Mono<Integer> downloadPiece$(int pieceIndex, Flux<Link> peers$);
}
