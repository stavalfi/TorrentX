package main.peer;

import main.TorrentInfo;
import main.torrent.status.TorrentStatusAction;
import main.torrent.status.state.tree.TorrentStatusState;
import main.tracker.TrackerConnection;
import main.tracker.TrackerProvider;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import redux.store.Store;

import java.util.function.Function;

public class SearchPeers {
    private TorrentInfo torrentInfo;
    private TrackerProvider trackerProvider;
    private PeersProvider peersProvider;

    private Flux<Link> peers$;

    public SearchPeers(TorrentInfo torrentInfo, Store<TorrentStatusState, TorrentStatusAction> store) {
        this(torrentInfo, store, new TrackerProvider(torrentInfo), new PeersProvider(torrentInfo));
    }

    public SearchPeers(TorrentInfo torrentInfo, Store<TorrentStatusState, TorrentStatusAction> store,
                       TrackerProvider trackerProvider, PeersProvider peersProvider) {
        this.torrentInfo = torrentInfo;
        this.trackerProvider = trackerProvider;
        this.peersProvider = peersProvider;

        Flux<TrackerConnection> trackerConnectionConnectableFlux =
                trackerProvider.connectToTrackersFlux()
                        .autoConnect();

        this.peers$ = searchPeers(torrentInfo, store,
                peersProvider.getPeersCommunicatorFromTrackerFlux(trackerConnectionConnectableFlux).autoConnect());
    }

    public SearchPeers(TorrentInfo torrentInfo, Store<TorrentStatusState, TorrentStatusAction> store,
                       TrackerProvider trackerProvider, PeersProvider peersProvider, Flux<Link> peers$) {
        this.torrentInfo = torrentInfo;
        this.trackerProvider = trackerProvider;
        this.peersProvider = peersProvider;
        this.peers$ = searchPeers(torrentInfo, store, peers$);
    }

    private Flux<Link> searchPeers(TorrentInfo torrentInfo, Store<TorrentStatusState, TorrentStatusAction> store,
                                   Flux<Link> peers$) {

        store.statesByAction(TorrentStatusAction.START_SEARCHING_PEERS_IN_PROGRESS)
                .take(1)
                .flatMap(__ -> store.dispatch(TorrentStatusAction.START_SEARCHING_PEERS_SELF_RESOLVED))
                .publish()
                .autoConnect(0);

        store.statesByAction(TorrentStatusAction.RESUME_SEARCHING_PEERS_IN_PROGRESS)
                .take(1)
                .flatMap(__ -> store.dispatch(TorrentStatusAction.RESUME_SEARCHING_PEERS_SELF_RESOLVED))
                .publish()
                .autoConnect(0);

        store.statesByAction(TorrentStatusAction.PAUSE_SEARCHING_PEERS_IN_PROGRESS)
                .take(1)
                .flatMap(__ -> store.dispatch(TorrentStatusAction.PAUSE_SEARCHING_PEERS_SELF_RESOLVED))
                .publish()
                .autoConnect(0);

        Function<Link, Mono<Link>> releaseWhenResuming = link ->
                store.states$()
                        .map(torrentStatusState -> torrentStatusState.fromAction(TorrentStatusAction.RESUME_SEARCHING_PEERS_WIND_UP))
                        .filter(Boolean::booleanValue)
                        .map(__ -> link)
                        .take(1)
                        .single();

        return store.statesByAction(TorrentStatusAction.START_SEARCHING_PEERS_WIND_UP)
                .take(1)
                // TODO: when i'm out of peers, we need to ask more..
                // but maybe the tests doesn't want more so we need to investigate.
                .flatMap(__ -> peers$)
                .flatMap(releaseWhenResuming)
                .publish()
                .autoConnect(0);
    }

    public TorrentInfo getTorrentInfo() {
        return torrentInfo;
    }

    public TrackerProvider getTrackerProvider() {
        return trackerProvider;
    }

    public PeersProvider getPeersProvider() {
        return peersProvider;
    }

    public Flux<Link> getPeers$() {
        return peers$;
    }
}
