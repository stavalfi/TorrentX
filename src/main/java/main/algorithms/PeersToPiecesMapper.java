package main.algorithms;

import main.peer.Link;
import reactor.core.publisher.Flux;

public interface PeersToPiecesMapper {
    /**
     * get available pieces that I don't have and peers can give me.
     *
     * @return flux of pieces numbers.
     */
    Flux<Integer> getAvailablePiecesFlux();

    Flux<Link> peerSupplierFlux(int pieceIndex);
}
