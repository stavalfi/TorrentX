package main.algorithms;

import main.downloader.PieceEvent;
import main.peer.Link;
import main.peer.peerMessages.RequestMessage;
import reactor.core.publisher.Mono;

/**
 * This class send request to a specific block and wait until the block is received.
 * The logic in this class is how it assert that the block is received.
 * There may multiple algorithms for asserting that.
 */
public interface BlockDownloader {

    /**
     * send the request and check that we received the correct block.
     */
    Mono<PieceEvent> downloadBlock(Link link, RequestMessage requestMessage);
}
