package main.torrent.status.side.effects;

import main.TorrentInfo;
import main.redux.store.Store;
import main.torrent.status.TorrentStatusAction;
import main.torrent.status.state.tree.TorrentStatusState;
import reactor.core.publisher.Flux;

import java.util.function.BiPredicate;

public class SearchPeersStateSideEffects {
    private Flux<TorrentStatusState> startSearchPeers$;
    private Flux<TorrentStatusState> resumeSearchPeers$;
    private Flux<TorrentStatusState> pauseSearchPeers$;

    public SearchPeersStateSideEffects(TorrentInfo torrentInfo,
                                       Store<TorrentStatusState, TorrentStatusAction> store) {

        BiPredicate<TorrentStatusAction, TorrentStatusState> tryDispatchStartWindupUntil =
                (torrentStatusAction, torrentStatusState) ->
                        torrentStatusState.getTorrentFileSystemState().isNotInAnyRemovingState() &&
                                torrentStatusState.getDownloadState().isNotInAnyCompleteState();

        BiPredicate<TorrentStatusAction, TorrentStatusState> tryDispatchResumeWindupUntil =
                (torrentStatusAction, torrentStatusState) ->
                        torrentStatusState.getTorrentFileSystemState().isNotInAnyRemovingState() &&
                                torrentStatusState.getDownloadState().isNotInAnyCompleteState();


        this.startSearchPeers$ = store.statesByAction(TorrentStatusAction.START_SEARCHING_PEERS_IN_PROGRESS)
                .concatMap(__ -> store.tryDispatchUntil(TorrentStatusAction.START_SEARCHING_PEERS_WIND_UP, tryDispatchStartWindupUntil),1)
                .concatMap(__ -> store.dispatch(TorrentStatusAction.RESUME_SEARCHING_PEERS_IN_PROGRESS),1)
                .publish()
                .autoConnect(0);

        this.resumeSearchPeers$ = store.statesByAction(TorrentStatusAction.RESUME_SEARCHING_PEERS_IN_PROGRESS)
                .concatMap(__ -> store.tryDispatchUntil(TorrentStatusAction.RESUME_SEARCHING_PEERS_WIND_UP, tryDispatchResumeWindupUntil),1)
                .publish()
                .autoConnect(0);

        this.pauseSearchPeers$ = store.statesByAction(TorrentStatusAction.PAUSE_SEARCHING_PEERS_IN_PROGRESS)
                .concatMap(__ -> store.notifyWhen(TorrentStatusAction.PAUSE_SEARCHING_PEERS_SELF_RESOLVED),1)
                .concatMap(__ -> store.dispatch(TorrentStatusAction.PAUSE_SEARCHING_PEERS_WIND_UP),1)
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
