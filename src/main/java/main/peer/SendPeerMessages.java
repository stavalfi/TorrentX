package main.peer;

import main.peer.peerMessages.PeerMessage;
import main.peer.peerMessages.RequestMessage;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.BitSet;

public interface SendPeerMessages {
    Mono<SendPeerMessages> sendBitFieldMessage(BitSet peaces);

    Mono<SendPeerMessages> sendCancelMessage(int index, int begin, int length);

    Mono<SendPeerMessages> sendChokeMessage();

    Mono<SendPeerMessages> sendHaveMessage(int pieceIndex);

    Mono<SendPeerMessages> sendInterestedMessage();

    Mono<SendPeerMessages> sendKeepAliveMessage();

    Mono<SendPeerMessages> sendNotInterestedMessage();

    Mono<SendPeerMessages> sendPieceMessage(int index, int begin, byte[] block);

    Mono<SendPeerMessages> sendPortMessage(short listenPort);

    Mono<SendPeerMessages> sendRequestMessage(int index, int begin, int length);

    Mono<SendPeerMessages> sendRequestMessage(RequestMessage requestMessage);

    Mono<SendPeerMessages> sendUnchokeMessage();

    Flux<PeerMessage> sentPeerMessagesFlux();
}
