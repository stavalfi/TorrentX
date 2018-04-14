package main.algorithms;

import main.TorrentInfo;
import main.downloader.TorrentPieceChanged;
import main.downloader.TorrentPieceStatus;
import main.file.system.PieceDownloadedAlreadyException;
import main.file.system.TorrentFileSystemManager;
import main.peer.Peer;
import main.peer.PeersCommunicator;
import main.peer.peerMessages.PieceMessage;
import main.peer.peerMessages.RequestMessage;
import main.torrent.status.TorrentStatus;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Random;
import java.util.concurrent.TimeoutException;
import java.util.function.BiPredicate;

public class DownloadBittorrentAlgorithmImpl {
    private TorrentInfo torrentInfo;
    private TorrentStatus torrentStatus;
    private TorrentFileSystemManager torrentFileSystemManager;
    private Flux<PeersCommunicator> recordedFreePeerFlux;
    private Flux<TorrentPieceChanged> recordedReceivedBlockFlux;

    private Flux<Integer> startDownloadFlux;

    DownloadBittorrentAlgorithmImpl(TorrentInfo torrentInfo,
                                    TorrentStatus torrentStatus,
                                    TorrentFileSystemManager torrentFileSystemManager,
                                    Flux<PeersCommunicator> recordedFreePeerFlux) {
        this.torrentInfo = torrentInfo;
        this.torrentStatus = torrentStatus;
        this.torrentFileSystemManager = torrentFileSystemManager;
        this.recordedFreePeerFlux = recordedFreePeerFlux
                .filter(peersCommunicator -> !peersCommunicator.getPeerCurrentStatus().getIsHeChokingMe())
                .filter(peersCommunicator -> peersCommunicator.getPeerCurrentStatus().getAmIInterestedInHim())
                .filter(peersCommunicator -> !peersCommunicator.getPeerCurrentStatus().getAmIDownloadingFromHim());

        // I'm recording this because I'm sending requests and
        // I need to assert that I received what I asked for.
        this.recordedReceivedBlockFlux = torrentFileSystemManager.savedBlockFlux()
                .replay()
                .autoConnect(0);

        this.startDownloadFlux = downloadFlux()
                .publish()
                .autoConnect(0);
    }

    private Flux<Integer> downloadFlux() {
        // I'm recording this because I need to assert that
        // we downloaded a piece **after** we requested it
        // so I can't miss the signal which indicate that
        // the piece was downloaded completely.
        Flux<Integer> recordedCompletedPieceFlux =
                torrentStatus.isStartedDownloadingFlux()
                        .filter(isDownloadStarted -> isDownloadStarted)
                        .flatMap(__ -> this.torrentFileSystemManager.savedBlockFlux())
                        .filter(torrentPieceChanged -> torrentPieceChanged.getTorrentPieceStatus().equals(TorrentPieceStatus.COMPLETED))
                        .map(TorrentPieceChanged::getPieceIndex)
                        .replay()
                        .autoConnect(0);

        return torrentStatus.notifyWhenStartedDownloading()
                .flatMapMany(__ -> {
                    Random random = new Random();
                    return Flux.<Integer>generate(sink -> {
                        int minMissingPieceIndex = this.torrentFileSystemManager.minMissingPieceIndex();
                        int maxMissingPieceIndex = this.torrentFileSystemManager.maxMissingPieceIndex();
                        if (minMissingPieceIndex == -1 || maxMissingPieceIndex == -1)
                            sink.complete();
                        else {
                            // get random number between inclusive minMissingPieceIndex and
                            // inclusive maxMissingPieceIndex. [minMissingPieceIndex,maxMissingPieceIndex].
                            int randomBetween = random.nextInt(maxMissingPieceIndex - minMissingPieceIndex + 1) +
                                    minMissingPieceIndex;
                            sink.next(randomBetween);
                        }
                    });
                })
                .distinct()
                // wait until we are in downloading state.
                .flatMap(missingPieceIndex ->
                        this.torrentStatus.notifyWhenResumeDownload()
                                .flatMap(__ -> downloadPiece(missingPieceIndex)), 1)
                // wait until we finished to download this piece.
                .flatMap(downloadingPieceIndex -> recordedCompletedPieceFlux
                        .filter(completedPieceIndex -> completedPieceIndex == downloadingPieceIndex), 1);
    }


    private Mono<Integer> downloadPiece(int missingPieceIndex) {
        // Note: it must be one only! because if not, I may download
        // multiple blocks of same piece but I'm not keeping track of
        // missing spaces in a piece. I'm only keeping track of how
        // much bytes I downloaded in this piece.
        final int CONCURRENT_BLOCK_REQUESTS_FROM_PEERS = 1;

        BiPredicate<PeersCommunicator, Integer> doesPeerHavePiece = (peer, pieceIndex) ->
                peer.getPeerCurrentStatus().getPiecesStatus().get(pieceIndex);

        // Keep creating and sending requests to this piece until
        // "create" will throw an error because the piece download
        // is completed.
        return this.torrentStatus.notifyWhenResumeDownload()
                // if the peer connection is closed then I will download again with different peer.
                .flatMap(__ -> this.recordedFreePeerFlux
                        .filter(peersCommunicator -> doesPeerHavePiece.test(peersCommunicator, missingPieceIndex))
                        .flatMap(peersCommunicator -> requestAndWait(peersCommunicator, missingPieceIndex), 1), 1)
                // all piece-messages will be collected to a list.
                .filter(torrentPieceChanged -> torrentPieceChanged.getTorrentPieceStatus().equals(TorrentPieceStatus.COMPLETED))
                .map(TorrentPieceChanged::getReceivedPiece)
                .map(PieceMessage::getIndex)
                .take(1) // there won't be no more.
                .single()
                .onErrorResume(PieceDownloadedAlreadyException.class, throwable -> Mono.just(missingPieceIndex));
    }

    private Flux<TorrentPieceChanged> requestAndWait(PeersCommunicator peersCommunicator,
                                                     int missingPieceIndex) {
        final Duration minWaitForBlock = Duration.ofMillis(500);

        return Flux.<RequestMessage>generate(sink -> {
            try {
                RequestMessage requestToPiece =
                        createRequestToPiece(peersCommunicator.getMe(), peersCommunicator.getPeer(), missingPieceIndex);
                sink.next(requestToPiece);
            } catch (PieceDownloadedAlreadyException e) {
                sink.complete();
            }
        }).flatMap(requestMessage ->
                peersCommunicator.sendMessages()
                        .sendRequestMessage(requestMessage)
                        .map(sendPeerMessages -> requestMessage), 1)
                .doOnNext(requestMessage -> blockThread(minWaitForBlock))
                .flatMap(requestMessage -> waitForPiece(requestMessage), 1);
    }

    private Mono<TorrentPieceChanged> waitForPiece(RequestMessage requestMessage) {
        final Duration maxWaitForBlock = Duration.ofSeconds(7);
        return this.recordedReceivedBlockFlux
                .filter(torrentPieceChanged -> requestMessage.getIndex() == torrentPieceChanged.getReceivedPiece().getIndex())
                .filter(torrentPieceChanged -> requestMessage.getBegin() == torrentPieceChanged.getReceivedPiece().getBegin())
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

    private RequestMessage createRequestToPiece(Peer me, Peer to, int missingPieceIndex) throws PieceDownloadedAlreadyException {
        final int REQUEST_BLOCK_SIZE = 16_384;
        int pieceLength = this.torrentInfo.getPieceLength(missingPieceIndex);
        // how much bytes did we download to this piece.
        int bytesDownloadedInPieceUntilNow = this.torrentFileSystemManager.getPiecesEstimatedStatus()[missingPieceIndex];

        // check if the download of this piece has completed already.
        if (pieceLength == bytesDownloadedInPieceUntilNow)
            throw new PieceDownloadedAlreadyException(this.torrentInfo, missingPieceIndex);

        return new RequestMessage(me, to, missingPieceIndex, bytesDownloadedInPieceUntilNow, REQUEST_BLOCK_SIZE);
    }

    private void blockThread(Duration minWaitForBlock) {
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
