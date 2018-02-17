package main.peer;

import reactor.core.publisher.Mono;

import java.util.BitSet;

public interface SendPeerMessage {
    Mono<Void> sendBitFieldMessage(BitSet peaces);

    Mono<Void> sendCancelMessage(int index, int begin, int length);

    Mono<Void> sendChokeMessage();

    Mono<Void> sendHaveMessage(int pieceIndex);

    Mono<Void> sendInterestedMessage();

    Mono<Void> sendKeepAliveMessage();

    Mono<Void> sendNotInterestedMessage();

    Mono<Void> sendPieceMessage(int index, int begin, byte[] block);

    Mono<Void> sendPortMessage(short listenPort);

    Mono<Void> sendRequestMessage(int index, int begin, int length);

    Mono<Void> sendUnchokeMessage();
}
