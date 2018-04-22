package main.algorithms.impls.v1.download;

import main.TorrentInfo;
import main.algorithms.BlockDownloader;
import main.downloader.PieceEvent;
import main.file.system.TorrentFileSystemManager;
import main.peer.Link;
import main.peer.peerMessages.RequestMessage;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;

public class BlockDownloaderImpl implements BlockDownloader {
    private TorrentInfo torrentInfo;
    private TorrentFileSystemManager torrentFileSystemManager;

    private Flux<PieceEvent> recordedSavedBlockFlux;

    public BlockDownloaderImpl(TorrentInfo torrentInfo,
                               TorrentFileSystemManager torrentFileSystemManager) {
        this.torrentInfo = torrentInfo;
        this.torrentFileSystemManager = torrentFileSystemManager;

        this.recordedSavedBlockFlux = this.torrentFileSystemManager
                .savedBlockFlux()
                .replay()
                .autoConnect(0);
    }

    /**
     * send the request and check that we received the correct block.
     *
     * @param link
     * @param requestMessage
     * @return
     */
    @Override
    public Mono<PieceEvent> downloadBlock(Link link,
                                          RequestMessage requestMessage) {
        return link.sendMessages().sendRequestMessage(requestMessage)
                .flatMapMany(__ -> this.recordedSavedBlockFlux)
                .filter(torrentPieceChanged -> requestMessage.getIndex() == torrentPieceChanged.getReceivedPiece().getIndex())
                .filter(torrentPieceChanged -> requestMessage.getBegin() == torrentPieceChanged.getReceivedPiece().getBegin())
                // max wait to the correct block back from peer.
                //TODO: in some operating systems, the IO operations are extremely slow.
                // for example the first use of randomAccessFile object. in linux all good.
                // we need to remmber to change back 20->2.
                .timeout(Duration.ofMillis(2 * 1000))
                .take(1)
                .single();
    }
}
