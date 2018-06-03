package main.torrent.status.side.effects;

import main.TorrentInfo;
import main.torrent.status.TorrentStatusAction;
import main.torrent.status.state.tree.TorrentStatusState;
import reactor.core.publisher.Flux;
import redux.store.Store;

import java.util.function.BiPredicate;

public class PeersStateSideEffects {
	private Flux<TorrentStatusState> startSearchPeers$;
	private Flux<TorrentStatusState> resumeSearchPeers$;
	private Flux<TorrentStatusState> pauseSearchPeers$;

	public PeersStateSideEffects(TorrentInfo torrentInfo,
								 Store<TorrentStatusState, TorrentStatusAction> store) {

		// TODO: fill:
		BiPredicate<TorrentStatusAction, TorrentStatusState> shouldCancelStartSearch = (torrentStatusAction, torrentStatusState) -> true;

		// TODO: fill:
		BiPredicate<TorrentStatusAction, TorrentStatusState> shouldCancelResumeSearch = (torrentStatusAction, torrentStatusState) -> true;

		this.startSearchPeers$ = store.statesByAction(TorrentStatusAction.START_SEARCHING_PEERS_IN_PROGRESS)
				.concatMap(__ -> store.dispatchAsLongNoCancel(TorrentStatusAction.START_SEARCHING_PEERS_WIND_UP, shouldCancelStartSearch))
				.concatMap(__ -> store.dispatch(TorrentStatusAction.RESUME_SEARCHING_PEERS_IN_PROGRESS))
				.publish()
				.autoConnect(0);

		this.resumeSearchPeers$ = store.statesByAction(TorrentStatusAction.RESUME_SEARCHING_PEERS_IN_PROGRESS)
				.concatMap(__ -> store.dispatchAsLongNoCancel(TorrentStatusAction.RESUME_SEARCHING_PEERS_WIND_UP, shouldCancelResumeSearch))
				.publish()
				.autoConnect(0);

		this.pauseSearchPeers$ = store.statesByAction(TorrentStatusAction.PAUSE_SEARCHING_PEERS_IN_PROGRESS)
				.concatMap(__ -> store.notifyWhen(TorrentStatusAction.PAUSE_SEARCHING_PEERS_SELF_RESOLVED))
				.concatMap(__ -> store.dispatch(TorrentStatusAction.PAUSE_SEARCHING_PEERS_WIND_UP))
				.publish()
				.autoConnect(0);
	}

	public Flux<TorrentStatusState> getStartSearchPeers$() {
		return startSearchPeers$;
	}

	public Flux<TorrentStatusState> getResumeSearchPeers$() {
		return resumeSearchPeers$;
	}

	public Flux<TorrentStatusState> getPauseSearchPeers$() {
		return pauseSearchPeers$;
	}
}
