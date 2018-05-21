package main.torrent.status.side.effects;

import main.TorrentInfo;
import main.torrent.status.TorrentStatusAction;
import main.torrent.status.state.tree.TorrentStatusState;
import reactor.core.publisher.Flux;
import redux.store.Store;

public class PeersStateSideEffects {
    private Flux<TorrentStatusState> startSearchPeers$;
    private Flux<TorrentStatusState> resumeSearchPeers$;
    private Flux<TorrentStatusState> pauseSearchPeers$;

    public PeersStateSideEffects(TorrentInfo torrentInfo,
                                 Store<TorrentStatusState, TorrentStatusAction> store) {
        this.startSearchPeers$ = store.getByAction$(TorrentStatusAction.START_SEARCHING_PEERS_IN_PROGRESS)
                .flatMap(__ -> store.dispatchAsLongNoCancel(TorrentStatusAction.START_SEARCHING_PEERS_WIND_UP))
                .flatMap(__ -> store.dispatch(TorrentStatusAction.RESUME_SEARCHING_PEERS_IN_PROGRESS))
                .publish()
                .autoConnect(0);

        this.resumeSearchPeers$ = store.getByAction$(TorrentStatusAction.RESUME_SEARCHING_PEERS_IN_PROGRESS)
                .flatMap(__ -> store.dispatchAsLongNoCancel(TorrentStatusAction.RESUME_SEARCHING_PEERS_WIND_UP))
                .publish()
                .autoConnect(0);

        this.pauseSearchPeers$ = store.getByAction$(TorrentStatusAction.PAUSE_SEARCHING_PEERS_IN_PROGRESS)
                .flatMap(__ -> store.dispatchAsLongNoCancel(TorrentStatusAction.PAUSE_SEARCHING_PEERS_WIND_UP))
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
