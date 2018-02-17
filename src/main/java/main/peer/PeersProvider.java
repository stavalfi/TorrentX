package main.peer;

import lombok.SneakyThrows;
import main.tracker.TrackerConnection;
import main.tracker.response.AnnounceResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SignalType;
import reactor.core.scheduler.Schedulers;

import java.util.logging.Level;

public class PeersProvider {
    private static Logger logger = LoggerFactory.getLogger(PeersProvider.class);

    public static Flux<Peer> getPeers(TrackerConnection trackerConnection,
                                      String torrentInfoHash) {
        return trackerConnection.announce(torrentInfoHash)
                .flux()
                .flatMap(AnnounceResponse::getPeers)
                .log(null, Level.INFO, true, SignalType.ON_NEXT);
    }

    static int x = 0;

    @SneakyThrows
    public static Mono<PeersCommunicator> connectToPeer(String torrentInfoHash, Peer peer) {
        return InitializePeersCommunication.getInstance()
                .connectToPeer(torrentInfoHash, peer)
                .doOnError(PeerExceptions.communicationErrors, error ->
                        logger.warn("error signal: (the application failed to connect to a peer." +
                                " the application will try to connect to the next available peer).\n" +
                                "peer: " + peer.toString() + "\n" +
                                "error message: " + error.getMessage() + ".\n" +
                                "error type: " + error.getClass().getName()))
                .onErrorResume(PeerExceptions.communicationErrors, error -> Mono.empty());
    }

    public static Flux<PeersCommunicator> connectToPeers(TrackerConnection trackerConnection,
                                                         String torrentInfoHash) {
        return trackerConnection.announce(torrentInfoHash)
                .flux()
                .flatMap(AnnounceResponse::getPeers)
                .distinct()
                .flatMap((Peer peer) ->
                        connectToPeer(torrentInfoHash, peer)
                                .subscribeOn(Schedulers.elastic()));
    }
}
