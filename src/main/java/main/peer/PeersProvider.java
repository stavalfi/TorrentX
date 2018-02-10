package main.peer;

import main.tracker.TrackerConnection;
import main.tracker.response.AnnounceResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SignalType;

import java.io.EOFException;
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
                error instanceof SocketTimeoutException || // the peer is not available.
                        error instanceof EOFException; // the peer closed the connection while we read/wait for data from him.

        return trackerConnection.announce(torrentInfoHash)
                .flux()
                .flatMap(AnnounceResponse::getPeers)
                .distinct()
                .flatMap((Peer peer) ->
                        InitializePeersCommunication.getInstance().connectToPeer(torrentInfoHash, peer) // send him Handshake request.
                                .doOnError(communicationErrorsToIgnore, error ->
                                        logger.warn("error signal: (the application failed to connect to a peer." +
                                                " the application will try to connect to the next available peer).\n" +
                                                "error message: " + error.getMessage() + ".\n" +
                                                "error type: " + error.getClass().getName()))
                                .onErrorResume(communicationErrorsToIgnore, error -> Mono.empty()))
                .log(null, Level.INFO, true, SignalType.ON_NEXT);

    }
}
