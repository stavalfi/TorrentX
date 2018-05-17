package main.peer;

import main.TorrentInfo;
import main.torrent.status.Action;
import main.torrent.status.TorrentStatusStore;
import main.tracker.TrackerConnection;
import main.tracker.TrackerProvider;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.function.Function;

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

        this.peers$ = searchPeers(torrentInfo, torrentStatusStore,
                peersProvider.getPeersCommunicatorFromTrackerFlux(trackerConnectionConnectableFlux).autoConnect());
    }

    public SearchPeers(TorrentInfo torrentInfo, TorrentStatusStore torrentStatusStore,
                       TrackerProvider trackerProvider, PeersProvider peersProvider, Flux<Link> peers$) {
        this.torrentInfo = torrentInfo;
        this.trackerProvider = trackerProvider;
        this.peersProvider = peersProvider;
        this.peers$ = searchPeers(torrentInfo, torrentStatusStore, peers$);
    }

    private Flux<Link> searchPeers(TorrentInfo torrentInfo, TorrentStatusStore torrentStatusStore, Flux<Link> peers$) {

        torrentStatusStore.getAction$(torrentInfo)
                .filter(Action.START_SEARCHING_PEERS_IN_PROGRESS::equals)
                .take(1)
                .flatMap(__ -> torrentStatusStore.dispatch(torrentInfo, Action.START_SEARCHING_PEERS_SELF_RESOLVED))
                .publish()
                .autoConnect(0);

        torrentStatusStore.getAction$(torrentInfo)
                .filter(Action.RESUME_SEARCHING_PEERS_IN_PROGRESS::equals)
                .take(1)
                .flatMap(__ -> torrentStatusStore.dispatch(torrentInfo, Action.RESUME_SEARCHING_PEERS_SELF_RESOLVED))
                .publish()
                .autoConnect(0);

        torrentStatusStore.getAction$(torrentInfo)
                .filter(Action.PAUSE_SEARCHING_PEERS_IN_PROGRESS::equals)
                .take(1)
                .flatMap(__ -> torrentStatusStore.dispatch(torrentInfo, Action.PAUSE_SEARCHING_PEERS_SELF_RESOLVED))
                .publish()
                .autoConnect(0);

        Function<Link, Mono<Link>> releaseWhenResuming = link ->
                torrentStatusStore.getState$(torrentInfo)
                        .map(torrentStatusState -> torrentStatusState.fromAction(Action.RESUME_SEARCHING_PEERS_WIND_UP))
                        .filter(Boolean::booleanValue)
                        .map(__ -> link)
                        .take(1)
                        .single();

        return torrentStatusStore.getAction$(torrentInfo)
                .filter(Action.START_SEARCHING_PEERS_WIND_UP::equals)
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
