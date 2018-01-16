package main;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

public class InitializePeersCommunication {
    private static Logger logger = LoggerFactory.getLogger(PeersCommunicator.class);

    public static Mono<PeersCommunicator> initialize(String peerIp, int peerPort) {
        return Mono.error(new NotImplementedException());
    }

    public static Flux<PeersCommunicator> listen() {
        return Flux.error(new NotImplementedException());
    }
}