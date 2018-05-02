package main.peer;

import main.file.system.AllocatedBlock;
import main.peer.peerMessages.PeerMessage;
import main.peer.peerMessages.RequestMessage;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.BitSet;

public interface SendMessagesNotifications {
    Mono<SendMessagesNotifications> sendBitFieldMessage(BitSet peaces);

    Mono<SendMessagesNotifications> sendCancelMessage(int index, int begin, int length);

    Mono<SendMessagesNotifications> sendChokeMessage();

    Mono<SendMessagesNotifications> sendHaveMessage(int pieceIndex);

    Mono<SendMessagesNotifications> sendInterestedMessage();

    Mono<SendMessagesNotifications> sendKeepAliveMessage();

    Mono<SendMessagesNotifications> sendNotInterestedMessage();

    Mono<SendMessagesNotifications> sendPieceMessage(int index, int begin,
                                                     int blockLength, AllocatedBlock allocatedBlock);

    Mono<SendMessagesNotifications> sendPortMessage(short listenPort);

    Mono<SendMessagesNotifications> sendRequestMessage(int index, int begin, int length);

    Mono<SendMessagesNotifications> sendRequestMessage(RequestMessage requestMessage);

    Mono<SendMessagesNotifications> sendUnchokeMessage();

    Flux<PeerMessage> sentPeerMessagesFlux();
}
