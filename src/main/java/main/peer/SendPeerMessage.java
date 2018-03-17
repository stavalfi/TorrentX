package main.peer;

import main.peer.peerMessages.PeerMessage;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.BitSet;

public interface SendPeerMessage {
    Mono<PeersCommunicator> sendBitFieldMessage(BitSet peaces);

    Mono<PeersCommunicator> sendCancelMessage(int index, int begin, int length);

    Mono<PeersCommunicator> sendChokeMessage();

    Mono<PeersCommunicator> sendHaveMessage(int pieceIndex);

    Mono<PeersCommunicator> sendInterestedMessage();

    Mono<PeersCommunicator> sendKeepAliveMessage();

    Mono<PeersCommunicator> sendNotInterestedMessage();

    Mono<PeersCommunicator> sendPieceMessage(int index, int begin, byte[] block);

    Mono<PeersCommunicator> sendPortMessage(short listenPort);

    Mono<PeersCommunicator> sendRequestMessage(int index, int begin, int length);

    Mono<PeersCommunicator> sendUnchokeMessage();

    Flux<PeerMessage> getSentMessagesFlux();
}
