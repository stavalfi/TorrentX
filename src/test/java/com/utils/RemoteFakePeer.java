package com.utils;

import main.algorithms.impls.v1.download.BlockDownloaderImpl;
import main.file.system.allocator.AllocatorStore;
import main.peer.IncomingPeerMessagesNotifierImpl;
import main.peer.Link;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

public class RemoteFakePeer {
    private static Logger logger = LoggerFactory.getLogger(BlockDownloaderImpl.class);
    private Link link;

    public RemoteFakePeer(AllocatorStore allocatorStore, Link link, FakePeerType fakePeerType, String identifier) {
        this.link = link;

        allocatorStore.updateAllocations(10, link.getTorrentInfo().getPieceLength(0))
                .publishOn(Schedulers.elastic())
                .as(StepVerifier::create)
                .expectNextCount(1)
                .verifyComplete();

        new IncomingPeerMessagesNotifierImpl(link.getIncomingPeerMessages$())
                .getRequestMessageResponseFlux()
                .doOnNext(requestMessage -> logger.info(identifier + " - received a new request: " + requestMessage))
                .doOnNext(requestMessage -> {
                    switch (fakePeerType) {
                        case CLOSE_IN_FIRST_REQUEST:
                            link.closeConnection();
                            logger.info(identifier + " - closed the connection as it should do at the first message he receive.");
                            return;
                        case RESPOND_WITH_DELAY_100:
                            blockThread(100);
                            logger.info(identifier + " - delayed in 100 mill the response. the fake peer did not response yet");
                            return;
                        case RESPOND_WITH_DELAY_3000:
                            blockThread(3 * 1000);
                            logger.info(identifier + " - delayed in 3000 mill the response. the fake peer did not response it");
                            return;
                    }
                })
                .flatMap(requestMessage -> {
                    switch (fakePeerType) {
                        case CLOSE_IN_FIRST_REQUEST:
                            return Mono.empty();
                        default:
                            return Mono.just(requestMessage);
                    }
                })
                .flatMap(requestMessage -> {
                    boolean doesFakePeerHaveThePiece = link.getPeerCurrentStatus()
                            .getPiecesStatus()
                            .get(requestMessage.getIndex());
                    logger.info(identifier + " - does he have the piece: " + doesFakePeerHaveThePiece + " for request: " + requestMessage);
                    if (!doesFakePeerHaveThePiece)
                        return Mono.empty();

                    switch (fakePeerType) {
                        case VALID:
                        case RESPOND_WITH_DELAY_100:
                        case RESPOND_WITH_DELAY_3000:
                            int pieceLength = link.getTorrentInfo().getPieceLength(requestMessage.getIndex());
                            // create a piece message with random bytes.
                            return allocatorStore.createPieceMessage(link.getMe(), link.getPeer(), requestMessage.getIndex(), requestMessage.getBegin(), requestMessage.getBlockLength(), pieceLength)
                                    .flatMap(pieceMessage -> link.sendMessages().sendPieceMessage(pieceMessage)
                                            .doOnNext(__ -> logger.info(identifier + " - sent piece message to the app: " + pieceMessage + " for request: " + requestMessage)));
                    }
                    // we will never be here... (by the current fake-peer-types).
                    return Mono.empty();
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

    public Link getLink() {
        return link;
    }
}
