package com.utils;

import main.peer.PeersCommunicator;
import main.peer.peerMessages.PeerMessage;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class RemoteFakePeerForRequestingPieces {

    private PeersCommunicator peersCommunicator;

    public RemoteFakePeerForRequestingPieces(PeersCommunicator peersCommunicator) {
        this.peersCommunicator = peersCommunicator;

        this.peersCommunicator.receivePeerMessages()
                .getRequestMessageResponseFlux()
                .flatMap(requestMessage ->
                        this.peersCommunicator.sendPieceMessage(requestMessage.getIndex(),
                                requestMessage.getBegin(),
                                toRandomByteArray(requestMessage.getBlockLength())))
                .subscribe();
    }

    public Mono<RemoteFakePeerForRequestingPieces> sendInterestedMessage() {
        return this.peersCommunicator.sendInterestedMessage()
                .map(peersCommunicator -> this);
    }

    public Mono<RemoteFakePeerForRequestingPieces> sendRequestMessage(int index, int begin, int length) {
        return this.peersCommunicator.sendRequestMessage(index, begin, length)
                .map(peersCommunicator -> this);
    }

    public Flux<PeerMessage> sentPeerMessagesFlux() {
        return this.peersCommunicator.sentPeerMessagesFlux();
    }

    private byte[] toRandomByteArray(Integer length) {
        byte[] bytes = new byte[length];
        byte content = 0;
        for (int i = 0; i < length; i++, content++)
            bytes[i] = content;
        return bytes;
    }
}
