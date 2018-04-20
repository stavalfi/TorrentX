package main.algorithms.v2;

import main.peer.Link;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

public class LinkForPiecesProvider {
    // from outside
    private Flux<Link> linkFlux;
    private BitSet updatedPieceState;

    // inner use


    // to outside
    private Flux<Integer> pieceToRequestFlux;

    public LinkForPiecesProvider(Flux<Link> linkFlux, BitSet updatedPieceState) {
        this.linkFlux = linkFlux;
        this.updatedPieceState = updatedPieceState;

        Flux<Integer> piecesFromHaveMessageFlux =
                this.linkFlux.map(link -> link.receivePeerMessages())
                        .flatMap(receivePeerMessages -> receivePeerMessages.getHaveMessageResponseFlux())
                        .map(haveMessage -> haveMessage.getPieceIndex());

        Flux<Integer> piecesFromBitFieldMessageFlux =
                this.linkFlux.map(link -> link.receivePeerMessages())
                        .flatMap(receivePeerMessages -> receivePeerMessages.getBitFieldMessageResponseFlux())
                        .map(bitFieldMessage -> bitFieldMessage.getPiecesStatus())
                        .flatMap(peerPieceStatus -> {
                            List<Integer> pieceList = new ArrayList<>();
                            for (int i = 0; i < peerPieceStatus.size(); i++)
                                if (peerPieceStatus.get(i))
                                    pieceList.add(i);
                            return Flux.fromIterable(pieceList);
                        });

        this.pieceToRequestFlux =
                Flux.merge(piecesFromHaveMessageFlux, piecesFromBitFieldMessageFlux)
                        .distinct()
                        .filter(pieceIndex -> !this.updatedPieceState.get(pieceIndex));
    }

    public Flux<Integer> pieceToRequestFlux() {
        return this.pieceToRequestFlux;
    }

    public Flux<Link> peerSupplierFlux(int pieceIndex) {
        return this.linkFlux
                .filter((Link link) -> link.getPeerCurrentStatus().getPiecesStatus().get(pieceIndex));
    }
}
