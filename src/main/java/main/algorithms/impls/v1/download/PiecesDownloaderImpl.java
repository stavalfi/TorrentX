package main.algorithms.impls.v1.download;

import main.TorrentInfo;
import main.algorithms.PeersToPiecesMapper;
import main.algorithms.PieceDownloader;
import main.algorithms.PiecesDownloader;
import main.file.system.FileSystemLink;
import main.file.system.allocator.AllocatorStore;
import main.torrent.status.TorrentStatusAction;
import main.torrent.status.state.tree.TorrentStatusState;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import redux.store.Store;

public class PiecesDownloaderImpl implements PiecesDownloader {
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
        this.downloadedPieces$ = peersToPiecesMapper.getLinksByAvailableMissingPiece$()
                .flatMap(peersToPiece$ ->
                                store.latestState$()
                                        .filter(torrentStatusState -> torrentStatusState.fromAction(TorrentStatusAction.RESUME_DOWNLOAD_WIND_UP))
                                        .flatMap(__ -> pieceDownloader.downloadPiece$(peersToPiece$.key(), peersToPiece$.replay().autoConnect(0)))
                        , 1, 1)
                .publish()
                .autoConnect(0);

        this.startDownload$ = store.statesByAction(TorrentStatusAction.START_DOWNLOAD_IN_PROGRESS)
                .concatMap(__ -> store.dispatch(TorrentStatusAction.START_DOWNLOAD_SELF_RESOLVED))
                .publish()
                .autoConnect(0);

        this.resumeDownload = store.statesByAction(TorrentStatusAction.RESUME_DOWNLOAD_IN_PROGRESS)
                .concatMap(__ -> store.dispatch(TorrentStatusAction.RESUME_DOWNLOAD_SELF_RESOLVED))
                .publish()
                .autoConnect(0);

        this.pauseDownload = store.statesByAction(TorrentStatusAction.PAUSE_DOWNLOAD_IN_PROGRESS)
                .concatMap(__ -> store.dispatch(TorrentStatusAction.PAUSE_DOWNLOAD_SELF_RESOLVED))
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
