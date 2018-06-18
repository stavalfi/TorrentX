package main.file.system.allocator;

import main.file.system.allocator.Results.CreatePieceMessageResult;
import main.file.system.allocator.Results.CreateRequestMessageResult;
import main.file.system.allocator.requests.*;
import main.peer.Peer;
import main.peer.peerMessages.PieceMessage;
import main.peer.peerMessages.RequestMessage;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import redux.store.Result;
import redux.store.Store;

public class AllocatorStore {

    private Store<AllocatorState, AllocatorAction> allocatorStore = new Store<>(new AllocatorReducer(),
            AllocatorReducer.defaultAllocatorState);

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
                .map(AllocatorState::getFreeBlocksStatus)
                .filter(freeBlocksStatus -> freeBlocksStatus.nextSetBit(0) >= 0)
                .concatMap(__ -> this.allocatorStore.dispatch(request))
                .filter(Result::isNewState)
                .take(1)
                .single()
                .cast(CreatePieceMessageResult.class)
                .map(CreatePieceMessageResult::getPieceMessage);
    }

    public Mono<RequestMessage> createRequestMessage(Peer from, Peer to, int index, int begin, int blockLength, int pieceLength) {
        assert begin >= 0;
        assert blockLength > 0;
        assert pieceLength > 0;

        CreateRequestMessageRequest request = new CreateRequestMessageRequest(from, to, index, begin, blockLength, pieceLength);
        return this.allocatorStore.dispatch(request)
                .cast(CreateRequestMessageResult.class)
                .map(CreateRequestMessageResult::getRequestMessage);
    }

    public Mono<AllocatorState> free(AllocatedBlock allocatedBlock) {
        FreeAllocationRequest request = new FreeAllocationRequest(allocatedBlock);
        return this.allocatorStore.dispatch(request)
                .map(Result::getState);
    }

    public void freeNonBlocking(AllocatedBlock allocatedBlock) {
        FreeAllocationRequest request = new FreeAllocationRequest(allocatedBlock);
        this.allocatorStore.dispatchNonBlocking(request);
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

