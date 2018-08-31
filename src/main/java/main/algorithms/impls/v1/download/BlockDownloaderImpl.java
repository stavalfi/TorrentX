package main.algorithms.impls.v1.download;

import main.TorrentInfo;
import main.algorithms.BlockDownloader;
import main.downloader.PieceEvent;
import main.file.system.FileSystemLink;
import main.peer.Link;
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
        Flux<PieceEvent> pieceSavedNotifier$ = this.fileSystemLink.savedBlocks$()
                .filter(torrentPieceChanged -> requestMessage.getIndex() == torrentPieceChanged.getReceivedPiece().getIndex())
                .filter(torrentPieceChanged -> requestMessage.getBegin() == torrentPieceChanged.getReceivedPiece().getBegin())
                .replay(1)
                .autoConnect(0);

        return link.sendMessages()
                .sendRequestMessage(requestMessage.getIndex(), requestMessage.getBegin(), requestMessage.getBlockLength())
                .doOnSubscribe(__ -> logger.debug(this.identifier + " - start sending request message: " + requestMessage))
                .doOnNext(__ -> logger.debug(this.identifier + " - end sending request message: " + requestMessage))
                .flatMapMany(__ -> pieceSavedNotifier$)
                // max wait to the correct block back from peer.
                //TODO: in some operating systems, the IO operations are extremely slow.
                // for example the first use of randomAccessFile object. in linux all good.
                // we need to remember to change back 20->2.
                .timeout(Duration.ofMillis(2500))
                .doOnError(TimeoutException.class, throwable -> logger.debug(this.identifier + " - no response to the request: " + requestMessage))
                .take(1)
                .single();
    }
}
