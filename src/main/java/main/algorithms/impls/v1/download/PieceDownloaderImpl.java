package main.algorithms.impls.v1.download;

import main.App;
import main.TorrentInfo;
import main.algorithms.BlockDownloader;
import main.algorithms.PieceDownloader;
import main.algorithms.impls.TorrentPieceStatus;
import main.allocator.AllocatorStore;
import main.file.system.FileSystemLink;
import main.peer.Link;
import main.peer.exceptions.PeerExceptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.concurrent.TimeoutException;
import java.util.function.Predicate;
import java.util.stream.IntStream;

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
        final int maxRequestBlockLength = 16_384;
//        final int maxRequestBlockLength = pieceLength;

        Predicate<Object> didISavedPieceAlready = __ -> fileSystemLink.havePiece(pieceIndex);

        return Mono.just(new Object())
                // I want it to run only when someone subscribe to this stream.
                .filter(didISavedPieceAlready.negate())
                .doOnNext(__ -> logger.debug("start downloading piece: " + pieceIndex))
                .flatMap(__ -> downloadPieceFromAvailablePeers(pieceIndex, links$, pieceLength, maxRequestBlockLength, didISavedPieceAlready))
                .subscribeOn(downloadPieceScheduler)
                .timeout(Duration.ofSeconds(30), Mono.empty(), App.timeoutFallbackScheduler)
                .onErrorResume(PeerExceptions.isTimeoutException.or(throwable -> throwable instanceof DownloadingSavedPieceException), throwable -> Mono.empty());
    }

    private Mono<Integer> downloadPieceFromAvailablePeers(int pieceIndex, Flux<Link> links$, int pieceLength, int maxRequestBlockLength, Predicate<Object> didISavedPieceAlready) {
        return links$.takeWhile(didISavedPieceAlready.negate())
                .filter(link -> !link.getPeerCurrentStatus().getIsHeChokingMe())
                .concatMap(link -> {
                    // TODO: I may send multiple interested messages if I will download from him concurrently multiple pieces.
                    if (!link.getPeerCurrentStatus().getAmIInterestedInHim())
                        return link.sendMessages().sendInterestedMessage()
                                .map(sendPeerMessages -> link);
                    return Mono.just(link);
                }, 1)
                .concatMap(link ->
                                Flux.<Boolean>generate(sink -> sink.next(link.getPeerCurrentStatus().getIsHeChokingMe()))
                                        // keep downloading blocks while he doesn't choke me and I didn't completely download the piece yet
                                        .takeWhile(isHeChokeMe -> !isHeChokeMe)
                                        .takeWhile(didISavedPieceAlready.negate())
                                        .concatMap(___ ->
                                                        Mono.just(this.fileSystemLink.getDownloadedBytesInPieces()[pieceIndex])
                                                                .flatMap(requestFrom ->
                                                                        this.allocatorStore.createRequestMessage(link.getMe(), link.getPeer(), pieceIndex, requestFrom, maxRequestBlockLength, pieceLength)
                                                                                .doOnNext(requestMessage -> logger.debug("start downloading block: " + requestMessage))
                                                                                .flatMap(requestMessage -> this.blockDownloader.downloadBlock(link, requestMessage).doOnError(TimeoutException.class, throwable -> logger.debug("peer: " + link.getPeer() + " not responding to my request: " + requestMessage)))
                                                                                .doOnNext(pieceEvent -> logger.debug("ended downloading block: " + pieceEvent)))
                                                , 1)
                                        // if the peer is down then I will get another peer to download from the rest of the piece.
                                        .onErrorResume(PeerExceptions.peerNotResponding, throwable -> Mono.empty())
                        , 1)
                .filter(pieceEvent -> pieceEvent.getTorrentPieceStatus().equals(TorrentPieceStatus.COMPLETED))
                .map(pieceEvent -> pieceIndex)
                .doOnNext(___ -> logger.debug("finished to download piece: " + pieceIndex))
                // its important to limit the requests upstream because i don't want to try to download the same block or piece more then once.
                .limitRequest(1)
                .single();
    }


    public static void main(String[] args) throws InterruptedException {
        Flux.fromStream(IntStream.range(1, 3).boxed())
                .publishOn(Schedulers.single())
                .takeWhile(i -> i <= 2)
                .doOnNext(i -> logger.info("hi: " + i))
                .concatMap(i -> Mono.just(i).subscribeOn(Schedulers.elastic()).delayElement(Duration.ofSeconds(2)), 1)
                .subscribe(i -> logger.info(i + ""));

        Thread.sleep(100000);
    }
}
