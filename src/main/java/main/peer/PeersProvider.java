package main.peer;

import main.tracker.BadResponseException;
import main.tracker.TrackerConnection;
import main.tracker.response.AnnounceResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SignalType;

import java.net.SocketTimeoutException;
import java.util.function.Predicate;
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

    public static Flux<PeersCommunicator> connectToPeers(TrackerConnection trackerConnection,
                                                         String torrentInfoHash) {
        Predicate<Throwable> communicationErrorsToIgnore = (Throwable error) ->
                error instanceof SocketTimeoutException ||
                        error instanceof BadResponseException;

        return trackerConnection.announce(torrentInfoHash)
                .flux()
                .flatMap(AnnounceResponse::getPeers)
                .log(null, Level.INFO, true, SignalType.ON_NEXT)
                .flatMap((Peer peer) ->
                        InitializePeersCommunication.getInstance().connectToPeer(torrentInfoHash, peer) // send him Handshake request.
                                .doOnError(communicationErrorsToIgnore, error ->
                                        logger.warn("error signal: (the application failed to connect to a peer." +
                                                " the application will try to connect to the next available peer)." +
                                                "\nerror message: " + error.getMessage() + ".\n" +
                                                "error type: " + error.getClass().getName()))
                                .onErrorResume(communicationErrorsToIgnore, error -> Mono.empty()))
                .log(null, Level.INFO, true, SignalType.ON_NEXT);

    }
}
