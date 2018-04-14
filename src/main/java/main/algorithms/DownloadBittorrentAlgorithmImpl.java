package main.algorithms;

import main.TorrentInfo;
import main.downloader.TorrentPieceChanged;
import main.downloader.TorrentPieceStatus;
import main.file.system.TorrentFileSystemManager;
import main.peer.Peer;
import main.peer.PeersCommunicator;
import main.peer.peerMessages.RequestMessage;
import main.torrent.status.TorrentStatus;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Random;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class DownloadBittorrentAlgorithmImpl {
    private TorrentInfo torrentInfo;
    private TorrentStatus torrentStatus;
    private TorrentFileSystemManager torrentFileSystemManager;
    private Flux<PeersCommunicator> recordedFreePeerFlux;

    private Flux<Integer> startDownloadFlux;

    public DownloadBittorrentAlgorithmImpl(TorrentInfo torrentInfo,
                                           TorrentStatus torrentStatus,
                                           TorrentFileSystemManager torrentFileSystemManager,
                                           Flux<PeersCommunicator> recordedFreePeerFlux) {
        this.torrentInfo = torrentInfo;
        this.torrentStatus = torrentStatus;
        this.torrentFileSystemManager = torrentFileSystemManager;
        this.recordedFreePeerFlux = recordedFreePeerFlux;

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

        return torrentStatus.isStartedDownloadingFlux()
                .filter(isDownloadStarted -> isDownloadStarted)
                .flatMap(__ -> {
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
                .flatMap(missingPieceIndex ->
                        torrentStatus.isDownloadingFlux()
                                .filter(isDownloading -> isDownloading)
                                .flatMap(__ -> requestSinglePieceFlux(missingPieceIndex)))
                // wait until we finished to download this piece.
                .flatMap(downloadingPieceIndex -> recordedCompletedPieceFlux
                        .filter(completedPieceIndex -> completedPieceIndex == downloadingPieceIndex))
                .doOnNext(completedPiece -> System.out.println(completedPiece));
    }


    private Mono<Integer> requestSinglePieceFlux(int missingPieceIndex) {
        final int CONCURRENT_REQUESTS_FROM_PEER = 1;

        BiPredicate<PeersCommunicator, Integer> doesPeerHavePiece = (peer, pieceIndex) ->
                peer.getPeerCurrentStatus().getPiecesStatus().get(pieceIndex);

        return this.recordedFreePeerFlux
                .filter(peersCommunicator -> doesPeerHavePiece.test(peersCommunicator, missingPieceIndex))
                .flatMap(peersCommunicator ->
                        createRequestsToPieceFlux(peersCommunicator.getMe(), peersCommunicator.getPeer(), missingPieceIndex)
                                .flatMap(peersCommunicator.sendMessages()::sendRequestMessage, CONCURRENT_REQUESTS_FROM_PEER))
                .collectList()
                .map(requests -> missingPieceIndex);
    }

    private Flux<RequestMessage> createRequestsToPieceFlux(Peer me, Peer to,
                                                           int pieceIndex) {
        final int REQUEST_BLOCK_SIZE = 16_384;
        int pieceLength = this.torrentInfo.getPieceLength(pieceIndex);
        // how much bytes did we download to this piece.
        int bytesDownloadedInPiece = this.torrentFileSystemManager.getPiecesEstimatedStatus()[pieceIndex];

        // check if the download of this piece has completed already.
        if (pieceLength == bytesDownloadedInPiece)
            return Flux.empty();

        int bytesLeftToDownload = pieceLength - bytesDownloadedInPiece;
        int requestsAmount = bytesLeftToDownload % REQUEST_BLOCK_SIZE == 0 ?
                bytesLeftToDownload / REQUEST_BLOCK_SIZE :
                bytesLeftToDownload / REQUEST_BLOCK_SIZE + 1;

        Function<Integer, Integer> requestLength = downloadFromPosition ->
                Math.min(downloadFromPosition + REQUEST_BLOCK_SIZE, pieceLength);

        Stream<RequestMessage> requestMessageStream = IntStream.iterate(bytesDownloadedInPiece, downloadedUntilNow -> downloadedUntilNow + REQUEST_BLOCK_SIZE)
                .limit(requestsAmount)
                .mapToObj(downloadFromPosition -> new RequestMessage(me, to, pieceIndex, downloadFromPosition,
                        requestLength.apply(downloadFromPosition)));

        return Flux.fromStream(requestMessageStream);
    }

    public Flux<Integer> startDownloadFlux() {
        return this.startDownloadFlux;
    }
}
