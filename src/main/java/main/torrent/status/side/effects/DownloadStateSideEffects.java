package main.torrent.status.side.effects;

import main.TorrentInfo;
import main.torrent.status.Action;
import main.torrent.status.TorrentStatusStore;
import main.torrent.status.state.tree.TorrentStatusState;
import reactor.core.publisher.Flux;

public class DownloadStateSideEffects {

    private Flux<TorrentStatusState> startDownload$;
    private Flux<TorrentStatusState> resumeDownload$;
    private Flux<TorrentStatusState> pauseDownload$;
    private Flux<TorrentStatusState> startUpload$;
    private Flux<TorrentStatusState> resumeUpload$;
    private Flux<TorrentStatusState> pauseUpload$;
    private Flux<TorrentStatusState> completeDownload$;

    public DownloadStateSideEffects(TorrentInfo torrentInfo,
                                    TorrentStatusStore store) {
        this.startDownload$ = store.getAction$(torrentInfo)
                .filter(Action.START_DOWNLOAD_IN_PROGRESS::equals)
                .flatMap(__ -> store.dispatchAsLongNoCancel(torrentInfo, Action.START_DOWNLOAD_WIND_UP))
                .flatMap(__ -> store.dispatch(torrentInfo, Action.RESUME_DOWNLOAD_IN_PROGRESS))
                .publish()
                .autoConnect(0);

        this.resumeDownload$ = store.getAction$(torrentInfo)
                .filter(Action.RESUME_DOWNLOAD_IN_PROGRESS::equals)
                .flatMap(__ -> store.dispatchAsLongNoCancel(torrentInfo, Action.RESUME_DOWNLOAD_WIND_UP))
                .publish()
                .autoConnect(0);

        this.pauseDownload$ = store.getAction$(torrentInfo)
                .filter(Action.PAUSE_DOWNLOAD_IN_PROGRESS::equals)
                .flatMap(__ -> store.dispatchAsLongNoCancel(torrentInfo, Action.PAUSE_DOWNLOAD_WIND_UP))
                .publish()
                .autoConnect(0);

        this.startUpload$ = store.getAction$(torrentInfo)
                .filter(Action.START_UPLOAD_IN_PROGRESS::equals)
                .flatMap(__ -> store.dispatchAsLongNoCancel(torrentInfo, Action.START_UPLOAD_WIND_UP))
                .flatMap(__ -> store.dispatch(torrentInfo, Action.RESUME_UPLOAD_IN_PROGRESS))
                .publish()
                .autoConnect(0);

        this.resumeUpload$ = store.getAction$(torrentInfo)
                .filter(Action.RESUME_UPLOAD_IN_PROGRESS::equals)
                .flatMap(__ -> store.dispatchAsLongNoCancel(torrentInfo, Action.RESUME_UPLOAD_WIND_UP))
                .publish()
                .autoConnect(0);

        this.pauseUpload$ = store.getAction$(torrentInfo)
                .filter(Action.PAUSE_UPLOAD_IN_PROGRESS::equals)
                .flatMap(__ -> store.dispatchAsLongNoCancel(torrentInfo, Action.PAUSE_UPLOAD_WIND_UP))
                .publish()
                .autoConnect(0);

        this.completeDownload$ = store.getAction$(torrentInfo)
                .filter(Action.COMPLETED_DOWNLOADING_IN_PROGRESS::equals)
                .flatMap(__ -> store.dispatch(torrentInfo, Action.PAUSE_DOWNLOAD_IN_PROGRESS))
                .flatMap(__ -> store.dispatchAsLongNoCancel(torrentInfo, Action.PAUSE_UPLOAD_WIND_UP))
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
