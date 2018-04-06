package com.utils;

import main.peer.PeersCommunicator;
import reactor.core.Disposable;

import java.util.concurrent.atomic.AtomicBoolean;

public class RemoteFakePeerForRequestingPieces {

    private Disposable subscribe1;
    private Disposable subscribe2;
    private Disposable subscribe3;
    private PeersCommunicator peersCommunicator;
    private AtomicBoolean isInterestedInMe = new AtomicBoolean(false);

    public RemoteFakePeerForRequestingPieces(PeersCommunicator peersCommunicator) {
        this.peersCommunicator = peersCommunicator;
        this.subscribe1 = this.peersCommunicator.receivePeerMessages()
                .getInterestedMessageResponseFlux()
                .subscribe(interestedMessage -> this.isInterestedInMe.set(true));

        this.subscribe2 = this.peersCommunicator.receivePeerMessages()
                .getNotInterestedMessageResponseFlux()
                .subscribe(interestedMessage -> this.isInterestedInMe.set(false));

        this.subscribe3 = this.peersCommunicator.receivePeerMessages()
                .getRequestMessageResponseFlux()
                .filter(requestMessage -> this.isInterestedInMe.get())
                .flatMap(requestMessage ->
                        this.peersCommunicator.sendPieceMessage(requestMessage.getIndex(),
                                requestMessage.getBegin(),
                                toRandomByteArray(requestMessage.getBlockLength())))
                .subscribe();
    }

    private byte[] toRandomByteArray(Integer length) {
        byte[] bytes = new byte[length];
        byte content = 0;
        for (int i = 0; i < length; i++, content++)
            bytes[i] = content;
        return bytes;
    }

    public synchronized void closeConnection() {
        this.peersCommunicator.closeConnection();
        this.subscribe1.dispose();
        this.subscribe2.dispose();
        this.subscribe3.dispose();
    }
}
