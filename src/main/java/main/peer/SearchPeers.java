package main.peer;

import main.TorrentInfo;
import main.file.system.allocator.AllocatorStore;
import main.torrent.status.TorrentStatusAction;
import main.torrent.status.state.tree.TorrentStatusState;
import main.tracker.TrackerProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import redux.store.Store;

public class SearchPeers {
    private static Logger logger = LoggerFactory.getLogger(SearchPeers.class);

    private TorrentInfo torrentInfo;
    private TrackerProvider trackerProvider;
    private PeersProvider peersProvider;
    private Flux<Link> peers$;

    public SearchPeers(AllocatorStore allocatorStore,TorrentInfo torrentInfo, Store<TorrentStatusState, TorrentStatusAction> store) {
        this(torrentInfo, store, new TrackerProvider(torrentInfo), new PeersProvider(allocatorStore,torrentInfo));
    }

    public SearchPeers(TorrentInfo torrentInfo, Store<TorrentStatusState, TorrentStatusAction> store,
                       TrackerProvider trackerProvider, PeersProvider peersProvider) {
        this.torrentInfo = torrentInfo;
        this.trackerProvider = trackerProvider;
        this.peersProvider = peersProvider;

        Flux<TorrentStatusState> startSearch$ = store.statesByAction(TorrentStatusAction.START_SEARCHING_PEERS_IN_PROGRESS)
                .concatMap(__ -> store.dispatch(TorrentStatusAction.START_SEARCHING_PEERS_SELF_RESOLVED))
                .publish()
                .autoConnect(0);

        this.peers$ = store.statesByAction(TorrentStatusAction.RESUME_SEARCHING_PEERS_IN_PROGRESS)
                .concatMap(__ -> store.dispatch(TorrentStatusAction.RESUME_SEARCHING_PEERS_SELF_RESOLVED))
                .flatMap(__ -> this.trackerProvider.connectToTrackersFlux()
                        .as(this.peersProvider::connectToPeers$))
                .doOnNext(link -> logger.info("search-peers-module connected to new peer: " + link))
                .flatMap(link -> store.notifyWhen(TorrentStatusAction.RESUME_SEARCHING_PEERS_WIND_UP, link))
                .doOnNext(link -> logger.debug("search-peers-module published new peer: " + link))
                .publish()
                .autoConnect(0);

        Flux<TorrentStatusState> pauseSearch$ = store.statesByAction(TorrentStatusAction.PAUSE_SEARCHING_PEERS_IN_PROGRESS)
                .concatMap(__ -> store.dispatch(TorrentStatusAction.PAUSE_SEARCHING_PEERS_SELF_RESOLVED))
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
