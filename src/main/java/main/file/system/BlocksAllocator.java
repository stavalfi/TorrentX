package main.file.system;

import main.peer.Peer;
import main.peer.peerMessages.PieceMessage;
import main.peer.peerMessages.RequestMessage;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface BlocksAllocator {

    Mono<AllocatorState> updateAllocations(int amountOfBlocks, int blockLength);

    Mono<PieceMessage> createPieceMessage(Peer from, Peer to, int index, int begin, int blockLength, int pieceLength);

    Mono<RequestMessage> createRequestMessage(Peer from, Peer to, int index, int begin, int blockLength, int pieceLength);

    Mono<AllocatorState> free(AllocatedBlock allocatedBlock);

    Mono<AllocatorState> freeAll();

    Mono<AllocatorState> getLatestState$();

    Flux<AllocatorState> getState$();

    Flux<AllocatorState> getStateHistory$();
}
