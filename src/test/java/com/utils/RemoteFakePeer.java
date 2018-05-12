package com.utils;

import main.file.system.BlocksAllocatorImpl;
import main.peer.Link;
import reactor.core.publisher.Mono;

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
                            blockThread(3 * 1000);
                            return;
                    }
                })
                .flatMap(requestMessage -> {
                    boolean doesFakePeerHaveThePiece = this.getPeerCurrentStatus()
                            .getPiecesStatus()
                            .get(requestMessage.getIndex());

                    if (!doesFakePeerHaveThePiece)
                        return Mono.empty();

                    switch (fakePeerType) {
                        case VALID:
                        case RESPOND_WITH_DELAY_100:
                        case RESPOND_WITH_DELAY_3000:
                            int pieceLength = super.getTorrentInfo().getPieceLength(requestMessage.getIndex());
                            return BlocksAllocatorImpl.getInstance()
                                    // create a piece message with random bytes.
                                    .createPieceMessage(super.getMe(), super.getPeer(),
                                            requestMessage.getIndex(), requestMessage.getBegin(),
                                            requestMessage.getBlockLength(), pieceLength)
                                    .flatMap(pieceMessage -> this.sendMessages().sendPieceMessage(pieceMessage));
                    }
                    // we will never be here...
                    return Mono.empty();
                }).publish()
                .autoConnect(0);
    }

    private void blockThread(int durationInMillis) {
        try {
            Thread.sleep(durationInMillis);
        } catch (InterruptedException e) {
            //e.printStackTrace();
        }
    }
}
