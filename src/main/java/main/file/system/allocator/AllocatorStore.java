package main.file.system.allocator;

import main.file.system.allocator.Results.CreatePieceMessageResult;
import main.file.system.allocator.Results.CreateRequestMessageResult;
import main.file.system.allocator.requests.*;
import main.peer.Peer;
import main.peer.peerMessages.PieceMessage;
import main.peer.peerMessages.RequestMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import redux.store.Result;
import redux.store.Store;

public class AllocatorStore {
    private static Logger logger = LoggerFactory.getLogger(AllocatorStore.class);

    private Store<AllocatorState, AllocatorAction> allocatorStore;

    public AllocatorStore(Store<AllocatorState, AllocatorAction> allocatorStore) {
        this.allocatorStore = allocatorStore;
    }

    public Mono<AllocatorState> updateAllocations(int amountOfBlocks, int blockLength) {
        UpdateAllocationArrayRequest request = new UpdateAllocationArrayRequest(amountOfBlocks, blockLength);
        return this.allocatorStore.dispatch(request)
                .map(Result::getState);
    }

    public Mono<PieceMessage> createPieceMessage(Peer from, Peer to, int index, int begin, int blockLength, int pieceLength) {
        assert begin >= 0;
        assert blockLength > 0;
        assert pieceLength > 0;

        CreatePieceMessageRequest request = new CreatePieceMessageRequest(from, to, index, begin, blockLength, pieceLength);

        return this.allocatorStore.states$()
                .doOnNext(__ -> logger.debug(this.allocatorStore.getIdentifier() + " - trying to allocating block for piece: " + request))
                .map(AllocatorState::getFreeBlocksStatus)
                .doOnNext(freeBlocksStatus -> logger.debug(this.allocatorStore.getIdentifier() + " - trying to allocating block for piece: " + request + ", is there a free block: " + (freeBlocksStatus.nextSetBit(0) >= 0) + ", status: " + freeBlocksStatus))
                .filter(freeBlocksStatus -> freeBlocksStatus.nextSetBit(0) >= 0)
                .concatMap(__ -> this.allocatorStore.dispatch(request))
                .doOnNext(allocatorStateAllocatorActionResult -> logger.debug(this.allocatorStore.getIdentifier() + " - is allocation of block ended for piece: " + request + ": " + allocatorStateAllocatorActionResult.isNewState()))
                .filter(Result::isNewState)
                .limitRequest(1)
                .single()
                .cast(CreatePieceMessageResult.class)
                .map(CreatePieceMessageResult::getPieceMessage)
                .doOnNext(pieceMessage -> logger.info(this.allocatorStore.getIdentifier() + " - allocation ended " + AllocatorAction.CREATE_PIECE_MESSAGE.toString() + pieceMessage));
    }

    public Mono<RequestMessage> createRequestMessage(Peer from, Peer to, int index, int begin, int blockLength, int pieceLength) {
        assert begin >= 0;
        assert blockLength > 0;
        assert pieceLength > 0;

        CreateRequestMessageRequest request = new CreateRequestMessageRequest(from, to, index, begin, blockLength, pieceLength);
        return this.allocatorStore.dispatch(request)
                .cast(CreateRequestMessageResult.class)
                .map(CreateRequestMessageResult::getRequestMessage)
                .doOnNext(requestMessage -> logger.info(this.allocatorStore.getIdentifier() + " - " + AllocatorAction.CREATE_REQUEST_MESSAGE.toString() + request));
    }

    public Mono<AllocatorState> free(AllocatedBlock allocatedBlock) {
        FreeAllocationRequest request = new FreeAllocationRequest(allocatedBlock);
        return this.allocatorStore.dispatch(request)
                .map(Result::getState)
                .doOnNext(requestMessage -> logger.info(this.allocatorStore.getIdentifier() + AllocatorAction.FREE_ALLOCATION.toString() + allocatedBlock));
    }

    public Mono<AllocatorState> freeAll() {
        FreeAllAllocationsRequest request = new FreeAllAllocationsRequest();
        return this.allocatorStore.dispatch(request)
                .map(Result::getState);
    }

    public Mono<AllocatorState> latestState$() {
        return this.allocatorStore.latestState$();
    }

    public Flux<AllocatorState> states$() {
        return this.allocatorStore.states$();
    }
}

