package main.torrent.status.side.effects;

import main.TorrentInfo;
import main.torrent.status.Action;
import main.torrent.status.TorrentStatusStore;
import main.torrent.status.state.tree.TorrentStatusState;
import reactor.core.publisher.Flux;

public class PeersStateSideEffects {
    private Flux<TorrentStatusState> startSearchPeers$;
    private Flux<TorrentStatusState> resumeSearchPeers$;
    private Flux<TorrentStatusState> pauseSearchPeers$;

    public PeersStateSideEffects(TorrentInfo torrentInfo,
                                 TorrentStatusStore store) {
        this.startSearchPeers$ = store.getAction$(torrentInfo)
                .filter(Action.START_SEARCHING_PEERS_IN_PROGRESS::equals)
                .flatMap(__ -> store.dispatchAsLongNoCancel(torrentInfo, Action.START_SEARCHING_PEERS_WIND_UP))
                .flatMap(__ -> store.dispatch(torrentInfo, Action.RESUME_SEARCHING_PEERS_IN_PROGRESS))
                .publish()
                .autoConnect(0);

        this.resumeSearchPeers$ = store.getAction$(torrentInfo)
                .filter(Action.RESUME_SEARCHING_PEERS_IN_PROGRESS::equals)
                .flatMap(__ -> store.dispatchAsLongNoCancel(torrentInfo, Action.RESUME_SEARCHING_PEERS_WIND_UP))
                .publish()
                .autoConnect(0);

        this.pauseSearchPeers$ = store.getAction$(torrentInfo)
                .filter(Action.PAUSE_SEARCHING_PEERS_IN_PROGRESS::equals)
                .flatMap(__ -> store.dispatchAsLongNoCancel(torrentInfo, Action.PAUSE_SEARCHING_PEERS_WIND_UP))
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
