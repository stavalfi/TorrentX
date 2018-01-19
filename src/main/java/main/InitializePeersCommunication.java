package main;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

public class InitializePeersCommunication {
    private static Logger logger = LoggerFactory.getLogger(PeersCommunicator.class);

    private Flux<PeersCommunicator> allPeersCommunicatorFlux;

    public static Mono<PeersCommunicator> initialize(Peer peer) {
        return Mono.error(new Exception());
    }

    public static void closeAllPeersCommunicator() {

    }

    public static Flux<PeersCommunicator> listen() {
        return Flux.error(new Exception());
    }

    public static void stopListenForNewPeers() {

    }

}