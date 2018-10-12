package main.algorithms.impls.v1.download;

import main.TorrentInfo;
import main.algorithms.PeersToPiecesMapper;
import main.algorithms.PieceDownloader;
import main.algorithms.PiecesDownloader;
import main.allocator.AllocatorStore;
import main.file.system.FileSystemLink;
import redux.store.Store;
import main.torrent.status.TorrentStatusAction;
import main.torrent.status.state.tree.TorrentStatusState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.concurrent.TimeoutException;

public class PiecesDownloaderImpl implements PiecesDownloader {
    private static Logger logger = LoggerFactory.getLogger(PiecesDownloaderImpl.class);

    private Flux<TorrentStatusState> startDownload$;
    private Flux<TorrentStatusState> resumeDownload;
    private Flux<TorrentStatusState> pauseDownload;

    private Flux<Integer> downloadedPieces$;

    public PiecesDownloaderImpl(AllocatorStore allocatorStore,
                                TorrentInfo torrentInfo,
                                Store<TorrentStatusState, TorrentStatusAction> store,
                                FileSystemLink fileSystemLink,
                                PeersToPiecesMapper peersToPiecesMapper,
                                PieceDownloader pieceDownloader) {

        // TODO: due to this implementation we need to first notify we started downloading and only then to start search for peers.
        // if not, we may lose some peers because we will listen to those new peers only after we start downloading.
        this.downloadedPieces$ = peersToPiecesMapper.availablePieces$()
                .filter(pieceIndex -> !fileSystemLink.havePiece(pieceIndex))
//                .doOnNext(pieceIndex -> logger.info("trying to download piece: " + pieceIndex))
                .flatMap(pieceIndex ->
                                store.latestState$()
                                        .filter(torrentStatusState -> torrentStatusState.fromAction(TorrentStatusAction.RESUME_DOWNLOAD_WIND_UP))
                                        .flatMap(__ -> pieceDownloader.downloadPiece$(pieceIndex, peersToPiecesMapper.linksForPiece$(pieceIndex))
                                                .onErrorResume(TimeoutException.class, throwable -> Mono.empty()))
                        , 10, 10)
                .doOnNext(pieceIndex -> logger.debug("finished to download piece: " + pieceIndex))
                .publish()
                .autoConnect(0);

        this.startDownload$ = store.statesByAction(TorrentStatusAction.START_DOWNLOAD_IN_PROGRESS)
                .concatMap(__ -> store.dispatch(TorrentStatusAction.START_DOWNLOAD_SELF_RESOLVED), 1)
                .publish()
                .autoConnect(0);

        this.resumeDownload = store.statesByAction(TorrentStatusAction.RESUME_DOWNLOAD_IN_PROGRESS)
                .concatMap(__ -> store.dispatch(TorrentStatusAction.RESUME_DOWNLOAD_SELF_RESOLVED), 1)
                .publish()
                .autoConnect(0);

        this.pauseDownload = store.statesByAction(TorrentStatusAction.PAUSE_DOWNLOAD_IN_PROGRESS)
                .concatMap(__ -> store.dispatch(TorrentStatusAction.PAUSE_DOWNLOAD_SELF_RESOLVED), 1)
                .publish()
                .autoConnect(0);
    }

    public Flux<TorrentStatusState> getStartDownload$() {
        return startDownload$;
    }

    public Flux<TorrentStatusState> getResumeDownload() {
        return resumeDownload;
    }

    public Flux<TorrentStatusState> getPauseDownload() {
        return pauseDownload;
    }

    @Override
    public Flux<Integer> getDownloadedPieces$() {
        return downloadedPieces$;
    }
}
