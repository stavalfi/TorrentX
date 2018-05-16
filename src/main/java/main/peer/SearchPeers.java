package main.peer;

import main.TorrentInfo;
import main.torrent.status.TorrentStatusStore;
import main.tracker.TrackerConnection;
import main.tracker.TrackerProvider;
import reactor.core.publisher.Flux;

public class SearchPeers {
    private TorrentInfo torrentInfo;
    private TrackerProvider trackerProvider;
    private PeersProvider peersProvider;

    private Flux<Link> peers$;

    public SearchPeers(TorrentInfo torrentInfo, TorrentStatusStore torrentStatusStore) {
        this(torrentInfo, torrentStatusStore, new TrackerProvider(torrentInfo), new PeersProvider(torrentInfo));
    }

    public SearchPeers(TorrentInfo torrentInfo, TorrentStatusStore torrentStatusStore,
                       TrackerProvider trackerProvider, PeersProvider peersProvider) {
        this.torrentInfo = torrentInfo;
        this.trackerProvider = trackerProvider;
        this.peersProvider = peersProvider;

        Flux<TrackerConnection> trackerConnectionConnectableFlux =
                trackerProvider.connectToTrackersFlux()
                        .autoConnect();

        this.peers$ = searchPeers(torrentStatusStore,
                peersProvider.getPeersCommunicatorFromTrackerFlux(trackerConnectionConnectableFlux));
    }

    public SearchPeers(TorrentInfo torrentInfo, TorrentStatusStore torrentStatusStore,
                       TrackerProvider trackerProvider, PeersProvider peersProvider, Flux<Link> peers$) {
        this.torrentInfo = torrentInfo;
        this.trackerProvider = trackerProvider;
        this.peersProvider = peersProvider;
        this.peers$ = searchPeers(torrentStatusStore, peers$);
    }

    private Flux<Link> searchPeers(TorrentStatusStore torrentStatusStore, Flux<Link> peers$) {
        return Flux.empty();
        // TODO: uncomment
//        return torrentStatusStore.getAction$()
//                .filter(Action.RESUME_SEARCHING_PEERS_IN_PROGRESS::equals)
//                .take(1)
//                .flatMap(__ -> torrentStatusStore.dispatch(Action.RESUME_SEARCHING_PEERS_WIND_UP))
//                .flatMap(__ -> peers$)
//                .flatMap(link -> torrentStatusStore.getLatestState$()
//                        .map(TorrentStatusState::getPeersState)
//                        .flatMap(peersState -> {
//                            if (peersState.fromAction(Action.PAUSE_SEARCHING_PEERS_IN_PROGRESS))
//                                return torrentStatusStore.dispatch(Action.PAUSE_SEARCHING_PEERS_WIND_UP)
//                                        .map(torrentStatusState -> link)
//                                        .ignoreElement();
//                            return Mono.just(link);
//                        }))
//                .publish()
//                .autoConnect(0);
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
