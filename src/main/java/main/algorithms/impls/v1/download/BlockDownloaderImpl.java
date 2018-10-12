package main.algorithms.impls.v1.download;

import main.TorrentInfo;
import main.algorithms.BlockDownloader;
import main.algorithms.PieceEvent;
import main.file.system.FileSystemLink;
import main.peer.Link;
import main.peer.algorithms.SendMessagesNotifications;
import main.peer.exceptions.PeerExceptions;
import main.peer.peerMessages.RequestMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.concurrent.TimeoutException;

public class BlockDownloaderImpl implements BlockDownloader {
    private static Logger logger = LoggerFactory.getLogger(BlockDownloaderImpl.class);
    private static Scheduler downloadBlockScheduler = Schedulers.newParallel("DOWNLOAD-BLOCK", 1);

    private TorrentInfo torrentInfo;
    private FileSystemLink fileSystemLink;
    private String identifier;

    public BlockDownloaderImpl(TorrentInfo torrentInfo,
                               FileSystemLink fileSystemLink,
                               String identifier) {
        this.torrentInfo = torrentInfo;
        this.fileSystemLink = fileSystemLink;
        this.identifier = identifier;
    }

    /**
     * send the request and check that we received the correct block.
     *
     * @param link
     * @param requestMessage
     * @return
     */
    @Override
    public Mono<PieceEvent> downloadBlock(Link link, RequestMessage requestMessage) {
        Mono<PieceEvent> savedPiece$ = this.fileSystemLink.savedBlocks$()
                .filter(torrentPieceChanged -> requestMessage.getIndex() == torrentPieceChanged.getReceivedPiece().getIndex())
                .filter(torrentPieceChanged -> requestMessage.getBegin() == torrentPieceChanged.getReceivedPiece().getBegin())
                .limitRequest(1)
                .single();

        Mono<SendMessagesNotifications> sendRequestMessage$ = link.sendMessages().sendRequestMessage(requestMessage);

        return Mono.zip(savedPiece$, sendRequestMessage$, (pieceEvent, sendMessagesNotifications) -> pieceEvent)
                .doOnSubscribe(__ -> logger.debug(this.identifier + " - start sending request message: " + requestMessage))
                .subscribeOn(downloadBlockScheduler)
                // TODO: there maybe a situation where we already got the piece but after the timeout so in the next time we request it, we won't see it in this implementation because we don't replay saved pieces from FS.
                .timeout(Duration.ofMillis(2500))
                .onErrorResume(PeerExceptions.isTimeoutException.and(__ -> fileSystemLink.havePiece(requestMessage.getIndex())), throwable -> Mono.error(new DownloadingSavedPieceException(requestMessage.getIndex())))
                .doOnError(TimeoutException.class, throwable -> logger.debug(this.identifier + " - no response to the request: " + requestMessage))
                .doOnNext(__ -> logger.debug(this.identifier + " - received block for request: " + requestMessage));
    }
}
