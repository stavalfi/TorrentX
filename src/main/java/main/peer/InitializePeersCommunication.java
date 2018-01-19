package main.peer;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.ServerSocket;

public class InitializePeersCommunication {

    private ServerSocket listenToPeerConnection;
    private Flux<PeersCommunicator> allPeersCommunicatorFlux;

    private static InitializePeersCommunication instance = new InitializePeersCommunication();

    public static InitializePeersCommunication getInstance() {
        return instance;
    }

    public Mono<PeersCommunicator> initialize(Peer peer) {
        return Mono.error(new Exception());
    }

    public void closeAllPeersCommunicator() {

    }

    public Flux<PeersCommunicator> listen() {
        return Flux.error(new Exception());
    }

    public void stopListenForNewPeers() {

    }

}