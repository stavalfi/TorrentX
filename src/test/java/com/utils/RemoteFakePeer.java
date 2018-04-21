package com.utils;

import main.peer.Link;

public class RemoteFakePeer extends Link {
    public RemoteFakePeer(Link link, FakePeerType fakePeerType) {
        super(link);
        this.receivePeerMessages()
                .getRequestMessageResponseFlux()
                .doOnNext(requestMessage -> {
                    switch (fakePeerType) {
                        case CLOSE_IN_FIRST_REQUEST:
                            closeConnection();
                            return;
                        case RESPOND_WITH_DELAY_100:
                            blockThread(100);
                            return;
                        case RESPOND_WITH_DELAY_3000:
                            blockThread(3000);
                            return;
                    }
                })
                .flatMap(requestMessage -> {
                    switch (fakePeerType) {
                        case VALID:
                        case RESPOND_WITH_DELAY_100:
                        case RESPOND_WITH_DELAY_3000:
                            return this.sendMessages()
                                    .sendPieceMessage(requestMessage.getIndex(), requestMessage.getBegin(),
                                            new byte[requestMessage.getBlockLength()]);
                    }
                    return null;
                }).publish()
                .autoConnect(0);
    }

    private void blockThread(int durationInMillis) {
        try {
            Thread.sleep(durationInMillis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
