package main.algorithms.impls.v1.download;

import main.algorithms.PeersToPiecesMapper;
import main.peer.Link;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

public class PeersToPiecesMapperImpl implements PeersToPiecesMapper {
    // from outside
    private Flux<Link> linkFlux;
    private BitSet updatedPieceState;

    // inner use


    // to outside
    private Flux<Integer> availablePiecesFlux;

    public PeersToPiecesMapperImpl(Flux<Link> linkFlux, BitSet updatedPieceState) {
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

        this.availablePiecesFlux =
                Flux.merge(piecesFromHaveMessageFlux, piecesFromBitFieldMessageFlux)
                        .distinct()
                        .filter(pieceIndex -> !this.updatedPieceState.get(pieceIndex));
    }

    @Override
    public Flux<Integer> getAvailablePiecesFlux() {
        return this.availablePiecesFlux;
    }

    @Override
    public Flux<Link> peerSupplierFlux(int pieceIndex) {
        return this.linkFlux
                .filter(link -> link.getPeerCurrentStatus().getPiecesStatus().get(pieceIndex))
                .filter(link -> !link.getPeerCurrentStatus().getIsHeChokingMe());
    }
}
