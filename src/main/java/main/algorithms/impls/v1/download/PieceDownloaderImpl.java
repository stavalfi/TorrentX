package main.algorithms.impls.v1.download;

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

import java.time.Duration;
import java.util.concurrent.TimeoutException;

public class PieceDownloaderImpl implements PieceDownloader {
    private static Logger logger = LoggerFactory.getLogger(PieceDownloaderImpl.class);

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
        final int maxRequestBlockLength = 17_000;

        return Flux.<Integer>generate(sink -> sink.next(this.fileSystemLink.getDownloadedBytesInPieces()[pieceIndex]))
                .concatMap(requestFrom ->
                        links$.filter(link -> !link.getPeerCurrentStatus().getIsHeChokingMe())
                                .concatMap(link ->
                                        this.allocatorStore.createRequestMessage(link.getMe(), link.getPeer(), pieceIndex, requestFrom, maxRequestBlockLength, pieceLength)
                                                .doOnNext(requestMessage -> logger.debug("start downloading block: " + requestMessage))
                                                .flatMap(requestMessage -> blockDownloader.downloadBlock(link, requestMessage))
                                                .doOnError(TimeoutException.class, throwable -> link.getPeerCurrentStatus().setIsHeChokingMe(true))
                                                .doOnNext(pieceEvent -> logger.debug("ended downloading block: " + pieceEvent)))
                                .onErrorResume(PeerExceptions.peerNotResponding, throwable -> Mono.empty())
                                .limitRequest(1))
                .filter(pieceEvent -> pieceEvent.getTorrentPieceStatus().equals(TorrentPieceStatus.COMPLETED))
                .doOnNext(__ -> logger.info("finished to download piece: " + pieceIndex))
                .limitRequest(1)
                .single()
                .map(__ -> pieceIndex)
                .timeout(Duration.ofSeconds(30));
    }
}
