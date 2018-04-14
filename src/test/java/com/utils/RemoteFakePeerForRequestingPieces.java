package com.utils;

import main.peer.Link;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicBoolean;

public class RemoteFakePeerForRequestingPieces {

    private Disposable subscribe1;
    private Disposable subscribe2;
    private Disposable subscribe3;
    private Link link;
    private AtomicBoolean isInterestedInMe = new AtomicBoolean(false);

    public RemoteFakePeerForRequestingPieces(Link link) {
        this.link = link;
        this.subscribe1 = this.link.receivePeerMessages()
                .getInterestedMessageResponseFlux()
                .subscribe(interestedMessage -> this.isInterestedInMe.set(true));

        this.subscribe2 = this.link.receivePeerMessages()
                .getNotInterestedMessageResponseFlux()
                .subscribe(interestedMessage -> this.isInterestedInMe.set(false));

        this.subscribe3 = this.link.receivePeerMessages()
                .getRequestMessageResponseFlux()
                .filter(requestMessage -> this.isInterestedInMe.get())
                .flatMap(requestMessage ->
                        this.link.sendMessages().sendPieceMessage(requestMessage.getIndex(),
                                requestMessage.getBegin(),
                                toRandomByteArray(requestMessage.getBlockLength())))
                .subscribe();
    }

    public Mono<RemoteFakePeerForRequestingPieces> sendInterestedMessage() {
        return this.link.sendMessages().sendInterestedMessage()
                .map(peersCommunicator -> this);
    }

    public Mono<RemoteFakePeerForRequestingPieces> sendRequestMessage(int index, int begin, int length) {
        return this.link.sendMessages().sendRequestMessage(index, begin, length)
                .map(peersCommunicator -> this);
    }

    private byte[] toRandomByteArray(Integer length) {
        byte[] bytes = new byte[length];
        byte content = 0;
        for (int i = 0; i < length; i++, content++)
            bytes[i] = content;
        return bytes;
    }

    public synchronized void closeConnection() {
        this.link.closeConnection();
        this.subscribe1.dispose();
        this.subscribe2.dispose();
        this.subscribe3.dispose();
    }
}
