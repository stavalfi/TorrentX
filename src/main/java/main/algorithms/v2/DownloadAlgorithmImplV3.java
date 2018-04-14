package main.algorithms.v2;

import main.TorrentInfo;
import main.downloader.TorrentPieceChanged;
import main.file.system.TorrentFileSystemManager;
import main.peer.Link;
import main.peer.PeerExceptions;
import main.peer.peerMessages.PieceMessage;
import main.peer.peerMessages.RequestMessage;
import main.torrent.status.TorrentStatus;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.TimeoutException;

public class DownloadAlgorithmImplV3 {
    private TorrentInfo torrentInfo;
    private TorrentStatus torrentStatus;
    private TorrentFileSystemManager torrentFileSystemManager;
    private PeersPiecesStatusOrganizer peersPiecesStatusOrganizer;
    private PieceRequestsSupplier pieceRequestsSupplier;

    private Flux<TorrentPieceChanged> recordedSavedBlockFlux;

    private Flux<Integer> startDownloadFlux;

    public DownloadAlgorithmImplV3(TorrentInfo torrentInfo, TorrentStatus torrentStatus,
                                   TorrentFileSystemManager torrentFileSystemManager,
                                   PeersPiecesStatusOrganizer peersPiecesStatusOrganizer,
                                   PieceRequestsSupplier pieceRequestsSupplier) {
        this.torrentInfo = torrentInfo;
        this.torrentStatus = torrentStatus;
        this.torrentFileSystemManager = torrentFileSystemManager;
        this.peersPiecesStatusOrganizer = peersPiecesStatusOrganizer;
        this.pieceRequestsSupplier = pieceRequestsSupplier;

        this.recordedSavedBlockFlux = this.torrentFileSystemManager
                .savedBlockFlux()
                .doOnNext(x -> System.out.println("4: " + Thread.currentThread().getName()))
                .replay()
                .autoConnect(0)
                .doOnNext(x -> System.out.println("5: " + Thread.currentThread().getName()));

        this.startDownloadFlux = downloadFlux()
                .publish()
                .autoConnect(0);
    }

    private Flux<Integer> downloadFlux() {
        return this.torrentStatus.notifyWhenStartedDownloading()
                .map(__ -> this.peersPiecesStatusOrganizer)
                .flatMapMany(peersPiecesStatusOrganizer -> peersPiecesStatusOrganizer.pieceToRequestFlux())
                .flatMap(pieceIndex -> this.peersPiecesStatusOrganizer.pieceSupplier(pieceIndex)
                                // request -> block 2 sec -> wait for piece response
                                .flatMap(link -> request(link, pieceIndex)
                                                .doOnNext(x -> System.out.println("1: " + Thread.currentThread().getName()))
                                                .doOnNext(requestMessage -> blockThread())
                                                .doOnNext(x -> System.out.println("2: " + Thread.currentThread().getName()))
                                                .flatMap(requestMessage -> wait(requestMessage), 1, 1)
                                                .doOnNext(x -> System.out.println("3: " + Thread.currentThread().getName()))
                                                .onErrorResume(PeerExceptions.communicationErrors, throwable -> Mono.empty())
                                        , 1,1)
                                .doOnNext(x -> System.out.println("6: " + Thread.currentThread().getName()))
                        , 1,1)
                .doOnNext(x -> System.out.println("7: " + Thread.currentThread().getName()));
    }

    private Flux<RequestMessage> request(Link link, int pieceIndex) {
        return this.pieceRequestsSupplier.createRequestFlux(link.getMe(), link.getPeer(), pieceIndex)
                .flatMap(requestMessage ->
                        link.sendMessages()
                                .sendRequestMessage(requestMessage)
                                .map(sendPeerMessages -> requestMessage), 1,1);
    }

    private Mono<Integer> wait(RequestMessage requestMessage) {
        final Duration maxWaitForBlock = Duration.ofSeconds(700000);
        return this.recordedSavedBlockFlux
                .filter(torrentPieceChanged -> requestMessage.getIndex() == torrentPieceChanged.getReceivedPiece().getIndex())
                .filter(torrentPieceChanged -> requestMessage.getBegin() == torrentPieceChanged.getReceivedPiece().getBegin())
                .map(TorrentPieceChanged::getReceivedPiece)
                .map(PieceMessage::getIndex)
                .take(1)
                .single()
                .timeout(maxWaitForBlock)
                // If I don't get the block from the peer in
                // the specified time, then I will download
                // the same block again because the next operation
                // after "onErrorResume" will request more signals
                // from upstream.
                .doOnError(TimeoutException.class,
                        throwable -> System.out.println("timeout - requesting again: " + requestMessage))
                .onErrorResume(TimeoutException.class, throwable -> Mono.empty());
    }

    private void blockThread() {
        final Duration minWaitForBlock = Duration.ofMillis(500);
        try {
            Thread.sleep(minWaitForBlock.toMillis());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public Flux<Integer> startDownloadFlux() {
        return this.startDownloadFlux;
    }

}
