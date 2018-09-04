package main.algorithms.impls.v1.download;

import main.TorrentInfo;
import main.algorithms.PeersToPiecesMapper;
import main.file.system.FileSystemLink;
import main.peer.IncomingPeerMessagesNotifier;
import main.peer.Link;
import main.peer.peerMessages.BitFieldMessage;
import main.peer.peerMessages.HaveMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.GroupedFlux;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.stream.IntStream;

public class PeersToPiecesMapperImpl implements PeersToPiecesMapper {
    private static Logger logger = LoggerFactory.getLogger(PeersToPiecesMapperImpl.class);

    // TODO: add dispose
    private Scheduler listenToAvailablePiecesScheduler;
    private Flux<Integer> availablePieces$;
    private Flux<AbstractMap.SimpleEntry<Integer, Link>> linksForPiece$;
    private Flux<GroupedFlux<Integer, Link>> linksByAvailableMissingPiece$;
    private Set<Integer> availablePieces;

    public PeersToPiecesMapperImpl(TorrentInfo torrentInfo,
                                   FileSystemLink fileSystemLink,
                                   IncomingPeerMessagesNotifier incomingPeerMessagesNotifier,
                                   Flux<Link> link$,
                                   BitSet initialPiecesStatus) {
        this.listenToAvailablePiecesScheduler = Schedulers.newSingle("LISTEN-TO-MAPPER-" + torrentInfo.getTorrentInfoHash());
        this.availablePieces = ConcurrentHashMap.newKeySet();

        fileSystemLink.savedPieces$()
                .doOnNext(pieceIndex -> {
                    synchronized (initialPiecesStatus) {
                        initialPiecesStatus.set(pieceIndex);
                    }
                })
                .publish()
                .autoConnect(0);

        BiFunction<BitSet, BitSet, BitSet> extractMissingPieces = (peerStatus, appStatus) -> {
            BitSet missingPiecesStatus = new BitSet(torrentInfo.getPieces().size());
            IntStream.range(0, torrentInfo.getPieces().size())
                    .forEach(i -> missingPiecesStatus.set(i, peerStatus.get(i) && !appStatus.get(i)));
            return missingPiecesStatus;
        };

        BiFunction<Link, BitSet, Flux<AbstractMap.SimpleEntry<Integer, Link>>> produceMissingPieces =
                (link, missingPiecesFromPeer) -> Flux.generate(() -> 0, (searchIndexFrom, sink) -> {
                    int missingPieceFromPeer = missingPiecesFromPeer.nextSetBit(searchIndexFrom);
                    if (missingPieceFromPeer != -1)
                        sink.next(new AbstractMap.SimpleEntry<>(missingPieceFromPeer, link));
                    else
                        sink.complete();
                    return missingPieceFromPeer + 1;

                });

        Flux<AbstractMap.SimpleEntry<Integer, Link>> fromBitFieldMessages = incomingPeerMessagesNotifier.getIncomingPeerMessages$()
                .filter(peerMessage -> peerMessage.getValue() instanceof BitFieldMessage)
                .map(peerMessage -> new AbstractMap.SimpleEntry<>(peerMessage.getKey(), (BitFieldMessage) peerMessage.getValue()))
                .doOnNext(bitFieldMessage -> logger.debug("1. before filter - peer: " + bitFieldMessage.getKey().getPeer() +
                        " - published he can give me the following pieces using bitFieldMessage: " + bitFieldMessage.getValue()))
                .map(bitFieldMessage -> new AbstractMap.SimpleEntry<>(bitFieldMessage.getKey(), bitFieldMessage.getValue().getPiecesStatus()))
                .map(bitFieldMessage -> {
                    synchronized (initialPiecesStatus) {
                        return new AbstractMap.SimpleEntry<>(bitFieldMessage.getKey(), extractMissingPieces.apply(bitFieldMessage.getValue(), initialPiecesStatus));
                    }
                })
                .doOnNext(bitFieldMessage -> logger.debug("peer: " + bitFieldMessage.getKey().getPeer() +
                        " - published he can give me the following pieces using bitFieldMessage: " + bitFieldMessage.getValue()))
                .flatMap(bitFieldMessage -> produceMissingPieces.apply(bitFieldMessage.getKey(), bitFieldMessage.getValue()));

        Flux<AbstractMap.SimpleEntry<Integer, Link>> fromHaveMessages = incomingPeerMessagesNotifier.getIncomingPeerMessages$()
                .filter(peerMessage -> peerMessage.getValue() instanceof HaveMessage)
                .map(peerMessage -> new AbstractMap.SimpleEntry<>((HaveMessage) peerMessage.getValue(), peerMessage.getKey()))
                .map(haveMessage -> new AbstractMap.SimpleEntry<>(haveMessage.getKey().getPieceIndex(), haveMessage.getValue()))
                .doOnNext(haveMessage -> logger.debug("peer: " + haveMessage.getValue().getPeer() +
                        " - published he can give me this piece using bitFieldMessage: " + haveMessage.getKey()));

        Flux<AbstractMap.SimpleEntry<Integer, Link>> linksForPiece$ = Flux.merge(fromBitFieldMessages, fromHaveMessages)
                .distinct()
                .replay()
                .autoConnect(2);

        this.linksForPiece$ = linksForPiece$.doOnNext(pieceLink -> this.availablePieces.add(pieceLink.getKey()))
                .replay()
                .autoConnect(0)
                .filter(pieceLink -> {
                    synchronized (initialPiecesStatus) {
                        return !initialPiecesStatus.get(pieceLink.getKey());
                    }
                });

        // TODO: end stream when download completed and make test for it.
        this.linksByAvailableMissingPiece$ = linksForPiece$.doOnNext(piece -> logger.debug("peer: " + piece.getValue().getPeer() + " - can give me this piece: " + piece.getKey()))
                .groupBy(AbstractMap.SimpleEntry::getKey, AbstractMap.SimpleEntry::getValue)
                .publish()
                .autoConnect(0);

        this.availablePieces$ = Flux.<Integer>create(sink -> {
            while (!sink.isCancelled() && !fileSystemLink.isDownloadCompleted()) {
                // TODO: remove completed pieces from the set.
                this.availablePieces.stream().forEach(sink::next);
            }
            if (fileSystemLink.isDownloadCompleted())
                sink.complete();
        }).filter(pieceIndex -> {
            synchronized (initialPiecesStatus) {
                return !initialPiecesStatus.get(pieceIndex);
            }
        }).subscribeOn(this.listenToAvailablePiecesScheduler)
                .publish()
                .autoConnect(0);
    }

    public Flux<Link> linksForPiece$(int pieceIndex) {
        return this.linksForPiece$.filter(pieceLink -> pieceLink.getKey().equals(pieceIndex))
                .map(AbstractMap.SimpleEntry::getValue);
    }

    public Flux<Integer> availablePieces$() {
        return this.availablePieces$;
    }

    @Override
    public Flux<GroupedFlux<Integer, Link>> getLinksByAvailableMissingPiece$() {
        return this.linksByAvailableMissingPiece$;
    }
}
