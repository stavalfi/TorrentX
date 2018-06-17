package main.torrent.status.side.effects;

import main.TorrentInfo;
import main.torrent.status.TorrentStatusAction;
import main.torrent.status.state.tree.TorrentStatusState;
import reactor.core.publisher.Flux;
import redux.store.Store;

import java.util.function.BiPredicate;

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
		// TODO: complete all methods:
		BiPredicate<TorrentStatusAction, TorrentStatusState> shouldCancelStartDownload = (torrentStatusAction, torrentStatusState) -> true;

		BiPredicate<TorrentStatusAction, TorrentStatusState> shouldCancelResumeDownload = (torrentStatusAction, torrentStatusState) -> true;

		BiPredicate<TorrentStatusAction, TorrentStatusState> shouldCancelCompleteDownload = (torrentStatusAction, torrentStatusState) -> true;

		BiPredicate<TorrentStatusAction, TorrentStatusState> shouldCancelStartUpload = (torrentStatusAction, torrentStatusState) -> true;

		BiPredicate<TorrentStatusAction, TorrentStatusState> shouldCancelResumeUpload = (torrentStatusAction, torrentStatusState) -> true;

		this.startDownload$ = store.statesByAction(TorrentStatusAction.START_DOWNLOAD_IN_PROGRESS)
				.concatMap(__ -> store.tryDispatchUntil(TorrentStatusAction.START_DOWNLOAD_WIND_UP, shouldCancelStartDownload))
				.concatMap(__ -> store.dispatch(TorrentStatusAction.RESUME_DOWNLOAD_IN_PROGRESS))
				.publish()
				.autoConnect(0);

		this.resumeDownload$ = store.statesByAction(TorrentStatusAction.RESUME_DOWNLOAD_IN_PROGRESS)
				.concatMap(__ -> store.tryDispatchUntil(TorrentStatusAction.RESUME_DOWNLOAD_WIND_UP, shouldCancelResumeDownload))
				.publish()
				.autoConnect(0);

		this.pauseDownload$ = store.statesByAction(TorrentStatusAction.PAUSE_DOWNLOAD_IN_PROGRESS)
				.concatMap(__ -> store.notifyWhen(TorrentStatusAction.PAUSE_DOWNLOAD_SELF_RESOLVED))
				.concatMap(__ -> store.dispatch(TorrentStatusAction.PAUSE_DOWNLOAD_WIND_UP))
				.publish()
				.autoConnect(0);

		this.completeDownload$ = store.statesByAction(TorrentStatusAction.COMPLETED_DOWNLOADING_IN_PROGRESS)
				.concatMap(__ -> store.dispatch(TorrentStatusAction.PAUSE_DOWNLOAD_IN_PROGRESS))
				.concatMap(__ -> store.tryDispatchUntil(TorrentStatusAction.COMPLETED_DOWNLOADING_WIND_UP, shouldCancelCompleteDownload))
				.publish()
				.autoConnect(0);

		this.startUpload$ = store.statesByAction(TorrentStatusAction.START_UPLOAD_IN_PROGRESS)
				.concatMap(__ -> store.tryDispatchUntil(TorrentStatusAction.START_UPLOAD_WIND_UP, shouldCancelStartUpload))
				.concatMap(__ -> store.dispatch(TorrentStatusAction.RESUME_UPLOAD_IN_PROGRESS))
				.publish()
				.autoConnect(0);

		this.resumeUpload$ = store.statesByAction(TorrentStatusAction.RESUME_UPLOAD_IN_PROGRESS)
				.concatMap(__ -> store.tryDispatchUntil(TorrentStatusAction.RESUME_UPLOAD_WIND_UP, shouldCancelResumeUpload))
				.publish()
				.autoConnect(0);

		this.pauseUpload$ = store.statesByAction(TorrentStatusAction.PAUSE_UPLOAD_IN_PROGRESS)
				.concatMap(__ -> store.notifyWhen(TorrentStatusAction.PAUSE_UPLOAD_SELF_RESOLVED))
				.concatMap(__ -> store.dispatch(TorrentStatusAction.PAUSE_UPLOAD_WIND_UP))
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
