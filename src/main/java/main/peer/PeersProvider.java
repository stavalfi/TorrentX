package main.peer;

import main.TorrentInfo;
import main.tracker.TrackerConnection;
import main.tracker.TrackerProvider;
import main.tracker.response.AnnounceResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

public class PeersProvider {
    private static Logger logger = LoggerFactory.getLogger(PeersProvider.class);

    private TorrentInfo torrentInfo;
    private TrackerProvider trackerProvider;
    private InitializePeersCommunication initializePeersCommunication;

    public PeersProvider(TorrentInfo torrentInfo, TrackerProvider trackerProvider,
                         InitializePeersCommunication initializePeersCommunication) {
        this.torrentInfo = torrentInfo;
        this.trackerProvider = trackerProvider;
        this.initializePeersCommunication = initializePeersCommunication;
    }

    public Mono<PeersCommunicator> connectToPeer(Peer peer) {
        return initializePeersCommunication
                .connectToPeer(peer)
                .subscribeOn(Schedulers.elastic())
                .doOnError(PeerExceptions.communicationErrors, error ->
                        logger.warn("error signal: (the application failed to connect to a peer." +
                                " the application will try to connect to the next available peer).\n" +
                                "peer: " + peer.toString() + "\n" +
                                "error message: " + error.getMessage() + ".\n" +
                                "error type: " + error.getClass().getName()))
                .onErrorResume(PeerExceptions.communicationErrors, error -> Mono.empty());
    }

    public Flux<PeersCommunicator> connectToPeers(TrackerConnection trackerConnection) {
        return trackerConnection.announce(torrentInfo.getTorrentInfoHash(), this.initializePeersCommunication.getTcpPort())
                .flux()
                .flatMap(AnnounceResponse::getPeers)
                .distinct()
                .flatMap((Peer peer) -> connectToPeer(peer));
    }

    public Flux<PeersCommunicator> connectToPeers(Flux<TrackerConnection> trackerConnectionFlux) {
        return trackerConnectionFlux
                .flatMap(trackerConnection -> connectToPeers(trackerConnection));
    }

    public Flux<PeersCommunicator> connectToPeers() {
        return connectToPeers(this.trackerProvider.connectToTrackers());
    }

    public InitializePeersCommunication getInitializePeersCommunication() {
        return initializePeersCommunication;
    }

    public TorrentInfo getTorrentInfo() {
        return torrentInfo;
    }

    public TrackerProvider getTrackerProvider() {
        return trackerProvider;
    }
}
