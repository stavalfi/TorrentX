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
    private LinkForPiecesProvider linkForPiecesProvider;
    private requestsCreator requestsCreator;

    private Flux<TorrentPieceChanged> recordedSavedBlockFlux;

    private Flux<Integer> startDownloadFlux;

    public DownloadAlgorithmImplV3(TorrentInfo torrentInfo, TorrentStatus torrentStatus,
                                   TorrentFileSystemManager torrentFileSystemManager,
                                   LinkForPiecesProvider linkForPiecesProvider,
                                   requestsCreator requestsCreator) {
        this.torrentInfo = torrentInfo;
        this.torrentStatus = torrentStatus;
        this.torrentFileSystemManager = torrentFileSystemManager;
        this.linkForPiecesProvider = linkForPiecesProvider;
        this.requestsCreator = requestsCreator;

        this.recordedSavedBlockFlux = this.torrentFileSystemManager
                .savedBlockFlux()
                .replay()
                .autoConnect(0);

        this.startDownloadFlux = downloadFlux()
                .publish()
                .autoConnect(0);
    }

    private Flux<Integer> downloadFlux2() {
        return this.torrentStatus.notifyWhenStartedDownloading()
                .map(__ -> this.linkForPiecesProvider)
                .flatMapMany(linkForPiecesProvider -> linkForPiecesProvider.pieceToRequestFlux())
                .flatMap(pieceIndex -> {
                            // I'm trying to download the following
                            // piece - block by block (one at a time).
                            // If I'm failing to download from a peer,
                            // then I will download the failed block
                            // from the next peer.
                            System.out.println("start downloading piece: " + pieceIndex);
                            return Flux.empty();
                        }
                        , 1, 1);
    }

    private Flux<Integer> downloadFlux() {
        return this.torrentStatus.notifyWhenStartedDownloading()
                .map(__ -> this.linkForPiecesProvider)
                .flatMapMany(linkForPiecesProvider -> linkForPiecesProvider.pieceToRequestFlux())
                .flatMap(pieceIndex -> {
                            // I'm trying to download the following
                            // piece - block by block (one at a time).
                            // If I'm failing to download from a peer,
                            // then I will download the failed block
                            // from the next peer.
                            System.out.println("start downloading piece: " + pieceIndex);
                            // TODO: bug -
                            // when I finish to download all the blocks from a given piece, then "request(link, pieceIndex)"
                            // will send complete signal. then I will ask the upstream for more signals.
                            // upstream == "this.linkForPiecesProvider.peerSupplierFlux(pieceIndex)"
                            // and the upstream will give me more peers for the same piece I already downloaded.
                            // and I won't try to download that piece so "request(link, pieceIndex)" will send complete signal
                            // and all over again forever.
                            return this.linkForPiecesProvider.peerSupplierFlux(pieceIndex)
                                    .doOnNext(link -> System.out.println("trying to download piece: " + pieceIndex + " from: " + link.getPeer()))
                                    //.log("", Level.INFO, true)
                                    // request -> block 2 sec -> waitUntilCorrectResponse for piece response
                                    .flatMap(link -> request(link, pieceIndex)
                                                    .doOnNext(requestMessage -> blockThread())
                                                    .flatMap(requestMessage -> waitUntilCorrectResponse(requestMessage), 1, 1)
                                                    .doOnError(throwable -> System.out.println(throwable))
                                                    // max wait to the correct block back from peer.
                                                    .timeout(Duration.ofSeconds(2))
                                                    // If I don't get the correct block from the peer in
                                                    // the specified time, then I will download
                                                    // the same block again because the next operation
                                                    // after "onErrorResume" will request more signals
                                                    // from upstream.
                                                    .onErrorResume(TimeoutException.class, throwable -> Mono.empty())
                                                    // If there is an error then I will communicate with the next peer.
                                                    .onErrorResume(PeerExceptions.communicationErrors, throwable -> Mono.empty())
                                            , 1, 1);
                        }
                        , 1, 1);
    }

    private Flux<RequestMessage> request(Link link, int pieceIndex) {
        return this.requestsCreator.createRequestFlux(link.getMe(), link.getPeer(), pieceIndex)
                .flatMap(requestMessage ->
                        link.sendMessages().sendRequestMessage(requestMessage)
                                .map(sendPeerMessages -> requestMessage), 1, 1);
    }

    private Mono<Integer> waitUntilCorrectResponse(RequestMessage requestMessage) {
        return this.recordedSavedBlockFlux
                .filter(torrentPieceChanged -> requestMessage.getIndex() == torrentPieceChanged.getReceivedPiece().getIndex())
                .filter(torrentPieceChanged -> requestMessage.getBegin() == torrentPieceChanged.getReceivedPiece().getBegin())
                .map(TorrentPieceChanged::getReceivedPiece)
                .map(PieceMessage::getIndex)
                .take(1)
                .single();
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
