package main.peer;

import main.tracker.BadResponseException;
import main.tracker.TrackerConnection;
import main.tracker.response.AnnounceResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SignalType;

import java.net.SocketTimeoutException;
import java.util.function.Predicate;
import java.util.logging.Level;

public class PeersProvider {

    public static Flux<Peer> getPeers(TrackerConnection trackerConnection,
                                      String torrentInfoHash) {
        return trackerConnection.announce(torrentInfoHash)
                .flux()
                .flatMap(AnnounceResponse::getPeers)
                .log(null, Level.FINE, true, SignalType.ON_NEXT);
    }

    public static Flux<PeersCommunicator> connectToPeers(TrackerConnection trackerConnection,
                                                         String torrentInfoHash) {
        Predicate<Throwable> communicationErrorsToIgnore = (Throwable error) ->
                error instanceof SocketTimeoutException ||
                        error instanceof BadResponseException;
        // || error instanceof SocketException; // if the peer do: "connection reset"

        return trackerConnection.announce(torrentInfoHash)
                .flux()
                .flatMap(AnnounceResponse::getPeers)
                .log(null, Level.INFO, true, SignalType.ON_NEXT)
                .flatMap((Peer peer) ->
                        InitializePeersCommunication.getInstance().connectToPeer(torrentInfoHash, peer) // send him Handshake request.
                                .onErrorResume(communicationErrorsToIgnore, error -> Mono.empty()))
                .log(null, Level.INFO, true, SignalType.ON_NEXT);

    }
}
