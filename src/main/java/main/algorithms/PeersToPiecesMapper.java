package main.algorithms;

import main.peer.Link;
import reactor.core.publisher.Flux;

public interface PeersToPiecesMapper {
    Flux<Integer> pieceToRequestFlux();

    Flux<Link> peerSupplierFlux(int pieceIndex);
}
