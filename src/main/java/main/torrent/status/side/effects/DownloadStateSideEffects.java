package main.torrent.status.side.effects;

import main.TorrentInfo;
import main.torrent.status.TorrentStatusAction;
import main.torrent.status.state.tree.TorrentStatusState;
import reactor.core.publisher.Flux;
import redux.store.Store;

public class DownloadStateSideEffects {

    private Flux<TorrentStatusState> startDownload$;
    private Flux<TorrentStatusState> resumeDownload$;
    private Flux<TorrentStatusState> pauseDownload$;
    private Flux<TorrentStatusState> startUpload$;
    private Flux<TorrentStatusState> resumeUpload$;
    private Flux<TorrentStatusState> pauseUpload$;
    private Flux<TorrentStatusState> completeDownload$;

    public DownloadStateSideEffects(TorrentInfo torrentInfo,
                                    Store<TorrentStatusState, TorrentStatusAction> store) {
        this.startDownload$ = store.getByAction$(TorrentStatusAction.START_DOWNLOAD_IN_PROGRESS)
                .flatMap(__ -> store.dispatchAsLongNoCancel(TorrentStatusAction.START_DOWNLOAD_WIND_UP))
                .flatMap(__ -> store.dispatch(TorrentStatusAction.RESUME_DOWNLOAD_IN_PROGRESS))
                .publish()
                .autoConnect(0);

        this.resumeDownload$ = store.getByAction$(TorrentStatusAction.RESUME_DOWNLOAD_IN_PROGRESS)
                .flatMap(__ -> store.dispatchAsLongNoCancel(TorrentStatusAction.RESUME_DOWNLOAD_WIND_UP))
                .publish()
                .autoConnect(0);

        this.pauseDownload$ = store.getByAction$(TorrentStatusAction.PAUSE_DOWNLOAD_IN_PROGRESS)
                .flatMap(__ -> store.dispatchAsLongNoCancel(TorrentStatusAction.PAUSE_DOWNLOAD_WIND_UP))
                .publish()
                .autoConnect(0);

        this.startUpload$ = store.getByAction$(TorrentStatusAction.START_UPLOAD_IN_PROGRESS)
                .flatMap(__ -> store.dispatchAsLongNoCancel(TorrentStatusAction.START_UPLOAD_WIND_UP))
                .flatMap(__ -> store.dispatch(TorrentStatusAction.RESUME_UPLOAD_IN_PROGRESS))
                .publish()
                .autoConnect(0);

        this.resumeUpload$ = store.getByAction$(TorrentStatusAction.RESUME_UPLOAD_IN_PROGRESS)
                .flatMap(__ -> store.dispatchAsLongNoCancel(TorrentStatusAction.RESUME_UPLOAD_WIND_UP))
                .publish()
                .autoConnect(0);

        this.pauseUpload$ = store.getByAction$(TorrentStatusAction.PAUSE_UPLOAD_IN_PROGRESS)
                .flatMap(__ -> store.dispatchAsLongNoCancel(TorrentStatusAction.PAUSE_UPLOAD_WIND_UP))
                .publish()
                .autoConnect(0);

        this.completeDownload$ = store.getByAction$(TorrentStatusAction.COMPLETED_DOWNLOADING_IN_PROGRESS)
                .flatMap(__ -> store.dispatch(TorrentStatusAction.PAUSE_DOWNLOAD_IN_PROGRESS))
                .flatMap(__ -> store.dispatchAsLongNoCancel(TorrentStatusAction.PAUSE_UPLOAD_WIND_UP))
                .publish()
                .autoConnect(0);
    }

    public Flux<TorrentStatusState> getStartDownload$() {
        return startDownload$;
    }

    public Flux<TorrentStatusState> getResumeDownload$() {
        return resumeDownload$;
    }

    public Flux<TorrentStatusState> getPauseDownload$() {
        return pauseDownload$;
    }

    public Flux<TorrentStatusState> getStartUpload$() {
        return startUpload$;
    }

    public Flux<TorrentStatusState> getResumeUpload$() {
        return resumeUpload$;
    }

    public Flux<TorrentStatusState> getPauseUpload$() {
        return pauseUpload$;
    }

    public Flux<TorrentStatusState> getCompleteDownload$() {
        return completeDownload$;
    }
}
