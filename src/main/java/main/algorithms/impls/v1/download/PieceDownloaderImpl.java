package main.algorithms.impls.v1.download;

import main.App;
import main.TorrentInfo;
import main.algorithms.BlockDownloader;
import main.algorithms.PieceDownloader;
import main.downloader.TorrentPieceStatus;
import main.file.system.FileSystemLink;
import main.file.system.allocator.AllocatorStore;
import main.peer.Link;
import main.peer.PeerExceptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.concurrent.TimeoutException;

public class PieceDownloaderImpl implements PieceDownloader {
    private static Logger logger = LoggerFactory.getLogger(PieceDownloaderImpl.class);
    private static Scheduler downloadPieceScheduler = Schedulers.newParallel("DOWNLOAD-PIECE", 1);

    private TorrentInfo torrentInfo;
    private FileSystemLink fileSystemLink;
    private BlockDownloader blockDownloader;
    private AllocatorStore allocatorStore;

    public PieceDownloaderImpl(AllocatorStore allocatorStore,
                               TorrentInfo torrentInfo,
                               FileSystemLink fileSystemLink,
                               BlockDownloader blockDownloader) {
        this.allocatorStore = allocatorStore;
        this.torrentInfo = torrentInfo;
        this.fileSystemLink = fileSystemLink;
        this.blockDownloader = blockDownloader;
    }

    @Override
    public Mono<Integer> downloadPiece$(int pieceIndex, Flux<Link> links$) {
        final int pieceLength = this.torrentInfo.getPieceLength(pieceIndex);
        final int maxRequestBlockLength = pieceLength;

        return links$.doOnSubscribe(__ -> logger.debug("start downloading piece: " + pieceIndex))
                .filter(link -> !link.getPeerCurrentStatus().getIsHeChokingMe())
                .concatMap(link -> {
                    if (!link.getPeerCurrentStatus().getAmIInterestedInHim())
                        return link.sendMessages().sendInterestedMessage()
                                .map(sendPeerMessages -> link);
                    return Mono.just(link);
                })
                .concatMap(link ->
                        Flux.<Integer>generate(sink -> sink.next(this.fileSystemLink.getDownloadedBytesInPieces()[pieceIndex]))
                                .concatMap(requestFrom ->
                                        this.allocatorStore.createRequestMessage(link.getMe(), link.getPeer(), pieceIndex, requestFrom, maxRequestBlockLength, pieceLength)
                                                .doOnNext(requestMessage -> logger.debug("start downloading block: " + requestMessage))
                                                .flatMap(requestMessage -> blockDownloader.downloadBlock(link, requestMessage)
                                                        .doOnError(TimeoutException.class, throwable -> logger.debug("peer: " + link.getPeer() + " not responding to my request: " + requestMessage)))
                                                .doOnNext(pieceEvent -> logger.debug("ended downloading block: " + pieceEvent)))
                                .onErrorResume(PeerExceptions.peerNotResponding, throwable -> Mono.empty()))
                .filter(pieceEvent -> pieceEvent.getTorrentPieceStatus().equals(TorrentPieceStatus.COMPLETED))
                .doOnNext(__ -> logger.info("finished to download piece: " + pieceIndex))
                // its important to limit the requests upstream because i don't want to try to download the same block or piece more then once.
                .limitRequest(1)
                .single()
                .map(__ -> pieceIndex)
                .subscribeOn(downloadPieceScheduler)
                .timeout(Duration.ofSeconds(30), Mono.empty(),App.timeoutFallbackScheduler);
    }
}
