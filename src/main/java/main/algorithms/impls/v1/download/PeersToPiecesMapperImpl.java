package main.algorithms.impls.v1.download;

import main.TorrentInfo;
import main.algorithms.PeersToPiecesMapper;
import main.peer.IncomingPeerMessagesNotifier;
import main.peer.Link;
import main.peer.peerMessages.BitFieldMessage;
import main.peer.peerMessages.HaveMessage;
import main.peer.peerMessages.RequestMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.GroupedFlux;
import reactor.core.publisher.UnicastProcessor;
import reactor.core.scheduler.Schedulers;

import java.util.AbstractMap;
import java.util.BitSet;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.IntStream;

public class PeersToPiecesMapperImpl implements PeersToPiecesMapper {
    private static Logger logger = LoggerFactory.getLogger(PeersToPiecesMapperImpl.class);

    private Flux<GroupedFlux<Integer, Link>> linksByAvailableMissingPiece$;

    public PeersToPiecesMapperImpl(TorrentInfo torrentInfo,
                                   IncomingPeerMessagesNotifier incomingPeerMessagesNotifier,
                                   Flux<Link> link$,
                                   BitSet updatedPieceState) {
        BiFunction<BitSet, BitSet, BitSet> extractMissingPieces = (peerStatus, appStatus) -> {
            BitSet missingPiecesStatus = new BitSet(torrentInfo.getPieces().size());
            IntStream.range(0, torrentInfo.getPieces().size())
                    .forEach(i -> missingPiecesStatus.set(i, peerStatus.get(i) && !appStatus.get(i)));
            return missingPiecesStatus;
        };

        BiFunction<Link, BitSet, Flux<AbstractMap.SimpleEntry<Link, Integer>>> produceMissingPieces =
                (link, missingPiecesFromPeer) -> Flux.generate(() -> 0, (searchIndexFrom, sink) -> {
                    int missingPieceFromPeer = missingPiecesFromPeer.nextSetBit(searchIndexFrom);
                    if (missingPieceFromPeer != -1)
                        sink.next(new AbstractMap.SimpleEntry<>(link, missingPieceFromPeer));
                    else
                        sink.complete();
                    return missingPieceFromPeer + 1;

                });

        Flux<AbstractMap.SimpleEntry<Link, Integer>> fromBitFieldMessages = incomingPeerMessagesNotifier.getIncomingPeerMessages$()
                .filter(peerMessage -> peerMessage.getValue() instanceof BitFieldMessage)
                .map(peerMessage -> new AbstractMap.SimpleEntry<>(peerMessage.getKey(), (BitFieldMessage) peerMessage.getValue()))
                .doOnNext(bitFieldMessage -> logger.info("1. before filter - peer: " + bitFieldMessage.getKey().getPeer() +
                        " - published he can give me the following pieces using bitFieldMessage: " + bitFieldMessage.getValue()))
                .map(bitFieldMessage -> new AbstractMap.SimpleEntry<>(bitFieldMessage.getKey(), bitFieldMessage.getValue().getPiecesStatus()))
                .map(bitFieldMessage -> new AbstractMap.SimpleEntry<>(bitFieldMessage.getKey(), extractMissingPieces.apply(bitFieldMessage.getValue(), updatedPieceState)))
                .doOnNext(bitFieldMessage -> logger.info("peer: " + bitFieldMessage.getKey().getPeer() +
                        " - published he can give me the following pieces using bitFieldMessage: " + bitFieldMessage.getValue()))
                .flatMap(bitFieldMessage -> produceMissingPieces.apply(bitFieldMessage.getKey(), bitFieldMessage.getValue()));

        Flux<AbstractMap.SimpleEntry<Link, Integer>> fromHaveMessages = incomingPeerMessagesNotifier.getIncomingPeerMessages$()
                .filter(peerMessage -> peerMessage.getValue() instanceof HaveMessage)
                .map(peerMessage -> new AbstractMap.SimpleEntry<>(peerMessage.getKey(), (HaveMessage) peerMessage.getValue()))
                .map(haveMessage -> new AbstractMap.SimpleEntry<>(haveMessage.getKey(), haveMessage.getValue().getPieceIndex()))
                .doOnNext(haveMessage -> logger.info("peer: " + haveMessage.getKey().getPeer() +
                        " - published he can give me this piece using bitFieldMessage: " + haveMessage.getValue()));

        this.linksByAvailableMissingPiece$ = Flux.merge(fromBitFieldMessages, fromHaveMessages)
                .distinct()
                .doOnNext(piece -> logger.debug("peer: " + piece.getKey().getPeer() + " - can give me this piece: " + piece.getValue()))
                .groupBy(AbstractMap.SimpleEntry::getValue, AbstractMap.SimpleEntry::getKey);
    }

    @Override
    public Flux<GroupedFlux<Integer, Link>> getLinksByAvailableMissingPiece$() {
        return this.linksByAvailableMissingPiece$;
    }
}
