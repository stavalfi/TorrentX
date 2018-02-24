package main.peer;

import lombok.SneakyThrows;
import main.TorrentInfo;
import main.tracker.TrackerConnection;
import main.tracker.TrackerProvider;
import main.tracker.response.AnnounceResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.ConnectableFlux;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

public class PeersProvider {
    private static Logger logger = LoggerFactory.getLogger(PeersProvider.class);

    public static Flux<Peer> getPeers(TrackerConnection trackerConnection,
                                      String torrentInfoHash) {
        return trackerConnection.announce(torrentInfoHash)
                .flux()
                .flatMap(AnnounceResponse::getPeers);
    }

    @SneakyThrows
    public static Mono<PeersCommunicator> connectToPeer(String torrentInfoHash, Peer peer) {
        return InitializePeersCommunication.getInstance()
                .connectToPeer(torrentInfoHash, peer)
                .subscribeOn(Schedulers.elastic())
                .doOnError(PeerExceptions.communicationErrors, error ->
                        logger.warn("error signal: (the application failed to connect to a peer." +
                                " the application will try to connect to the next available peer).\n" +
                                "peer: " + peer.toString() + "\n" +
                                "error message: " + error.getMessage() + ".\n" +
                                "error type: " + error.getClass().getName()))
                .onErrorResume(PeerExceptions.communicationErrors, error -> Mono.empty());
    }

    public static Flux<PeersCommunicator> connectToPeers(String torrentInfoHash, TrackerConnection trackerConnection) {
        return trackerConnection.announce(torrentInfoHash)
                .flux()
                .flatMap(AnnounceResponse::getPeers)
                .distinct()
                .flatMap((Peer peer) -> connectToPeer(torrentInfoHash, peer));
    }

    public static ConnectableFlux<PeersCommunicator> connectToPeers(String torrentInfoHash, Flux<TrackerConnection> trackerConnectionFlux) {
        return trackerConnectionFlux
                .flatMap(trackerConnection -> connectToPeers(torrentInfoHash, trackerConnection))
                .publish();
    }

    public static ConnectableFlux<PeersCommunicator> connectToPeers(TorrentInfo torrentInfo) {
        return connectToPeers(torrentInfo.getTorrentInfoHash(),
                TrackerProvider.connectToTrackers(torrentInfo));
    }
}
