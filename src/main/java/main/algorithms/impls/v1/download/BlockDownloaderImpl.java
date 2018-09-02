package main.algorithms.impls.v1.download;

import main.TorrentInfo;
import main.algorithms.BlockDownloader;
import main.downloader.PieceEvent;
import main.file.system.FileSystemLink;
import main.peer.Link;
import main.peer.SendMessagesNotifications;
import main.peer.peerMessages.RequestMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.TimeoutException;

public class BlockDownloaderImpl implements BlockDownloader {
    private static Logger logger = LoggerFactory.getLogger(BlockDownloaderImpl.class);

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

        Mono<SendMessagesNotifications> sendRequestMessage$ = link.sendMessages()
                .sendRequestMessage(requestMessage.getIndex(), requestMessage.getBegin(), requestMessage.getBlockLength());

        return Mono.zip(savedPiece$, sendRequestMessage$, (pieceEvent, sendMessagesNotifications) -> pieceEvent)
                .doOnSubscribe(__ -> logger.debug(this.identifier + " - start sending request message: " + requestMessage))
                .timeout(Duration.ofMillis(2500))
                .doOnError(TimeoutException.class, throwable -> logger.debug(this.identifier + " - no response to the request: " + requestMessage))
                .doOnNext(__ -> logger.debug(this.identifier + " - end sending request message: " + requestMessage));
    }
}
