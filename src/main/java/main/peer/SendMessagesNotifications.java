package main.peer;

import main.peer.peerMessages.PeerMessage;
import main.peer.peerMessages.PieceMessage;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.BitSet;

public interface SendMessagesNotifications {
    Mono<SendMessagesNotifications> sendBitFieldMessage(BitSet peaces);

    Mono<SendMessagesNotifications> sendCancelMessage(int index, int begin, int blockLength);

    Mono<SendMessagesNotifications> sendChokeMessage();

    Mono<SendMessagesNotifications> sendHaveMessage(int pieceIndex);

    Mono<SendMessagesNotifications> sendInterestedMessage();

    Mono<SendMessagesNotifications> sendKeepAliveMessage();

    Mono<SendMessagesNotifications> sendNotInterestedMessage();

    // I can't create another pieceMessage because it was created
    // in the FS module and it allocated allocatedBlock for me there.
    // here I need to free it.
    Mono<SendMessagesNotifications> sendPieceMessage(PieceMessage pieceMessage);

    Mono<SendMessagesNotifications> sendPortMessage(short listenPort);

    Mono<SendMessagesNotifications> sendRequestMessage(int index, int begin, int blockLength);

    Mono<SendMessagesNotifications> sendUnchokeMessage();

    Flux<PeerMessage> sentPeerMessagesFlux();
}
