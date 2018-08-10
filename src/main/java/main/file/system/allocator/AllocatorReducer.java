package main.file.system.allocator;

import main.file.system.allocator.Results.CreatePieceMessageResult;
import main.file.system.allocator.Results.CreateRequestMessageResult;
import main.file.system.allocator.requests.*;
import main.peer.peerMessages.PieceMessage;
import main.peer.peerMessages.RequestMessage;
import redux.reducer.Reducer;
import redux.store.Request;
import redux.store.Result;

import java.util.BitSet;
import java.util.stream.IntStream;

public class AllocatorReducer implements Reducer<AllocatorState, AllocatorAction> {
    public static AllocatorState defaultAllocatorState = createInitialState(1, 1);

    @Override
    public Result<AllocatorState, AllocatorAction> reducer(AllocatorState lastState, Request<AllocatorAction> request) {
        switch (request.getAction()) {
            case INITIALIZE:
                return new Result<>(request, lastState, false);
            case CREATE_PIECE_MESSAGE:
                return createPieceMessage(lastState, (CreatePieceMessageRequest) request);
            case CREATE_REQUEST_MESSAGE:
                return createRequestMessage(lastState, (CreateRequestMessageRequest) request);
            case UPDATE_ALLOCATIONS_ARRAY:
                return createNewStateToUpdateAllocationArray(lastState, (UpdateAllocationArrayRequest) request);
            case FREE_ALL_ALLOCATIONS:
                return createNewStateToFreeAllAllocations(lastState, (FreeAllAllocationsRequest) request);
            case FREE_ALLOCATION:
                return createNewStateToFreeAllocation(lastState, (FreeAllocationRequest) request);
            default:
                return new Result<>(request, lastState, false);
        }
    }

    private Result<AllocatorState, AllocatorAction> createNewStateToUpdateAllocationArray(AllocatorState lastState, UpdateAllocationArrayRequest request) {
//        if (lastState.getAmountOfBlocks() == request.getAmountOfBlocks() &&
//                lastState.getBlockLength() == request.getBlockLength())
//            return new Result<>(request, lastState, false);

        BitSet freeBlocksStatus = new BitSet(request.getAmountOfBlocks());
        AllocatedBlock[] allocatedBlocks = new AllocatedBlock[request.getAmountOfBlocks()];
        int min = Math.min(lastState.getAmountOfBlocks(), request.getAmountOfBlocks());
        int max = Math.max(lastState.getAmountOfBlocks(), request.getAmountOfBlocks());

        IntStream.range(0, min)
                .peek(index -> freeBlocksStatus.set(index, lastState.getFreeBlocksStatus().get(index)))
                .forEach(index -> allocatedBlocks[index] = new AllocatedBlockImpl(
                        lastState.getAllocatedBlocks()[index].getAllocationId(),
                        index,
                        request.getBlockLength()));
        if (lastState.getAmountOfBlocks() < request.getAmountOfBlocks()) {
            freeBlocksStatus.set(min, max, true);
            IntStream.range(min, max)
                    .forEach(index -> allocatedBlocks[index] = new AllocatedBlockImpl(index, request.getBlockLength()));
        }
        AllocatorState newState = new AllocatorState(request.getId(),
                request.getAction(),
                request.getBlockLength(),
                request.getAmountOfBlocks(),
                freeBlocksStatus,
                allocatedBlocks);
        return new Result<>(request, newState, true);
    }

    private Result<AllocatorState, AllocatorAction> createNewStateToFreeAllAllocations(AllocatorState lastState, FreeAllAllocationsRequest request) {
//        if (lastState.getFreeBlocksStatus().nextSetBit(0) == -1)
//            return new Result<>(request, lastState, false);

        BitSet freeBlocksStatus = new BitSet(lastState.getAmountOfBlocks());
        freeBlocksStatus.set(0, lastState.getAmountOfBlocks());
        AllocatorState newState = new AllocatorState(request.getId(),
                request.getAction(),
                lastState.getBlockLength(),
                lastState.getAmountOfBlocks(),
                freeBlocksStatus,
                lastState.getAllocatedBlocks());
        return new Result<>(request, newState, true);
    }

    private Result<AllocatorState, AllocatorAction> createNewStateToFreeAllocation(AllocatorState lastState, FreeAllocationRequest request) {
        if (lastState.getFreeBlocksStatus().get(request.getAllocatedBlock().getBlockIndex()))
            return new Result<>(request, lastState, false);

        BitSet freeBlocksStatus = new BitSet(lastState.getAmountOfBlocks());
        freeBlocksStatus.set(request.getAllocatedBlock().getBlockIndex(), true);
        for (int i = 0; i < lastState.getAmountOfBlocks(); i++)
            if (i != request.getAllocatedBlock().getBlockIndex()) {
                freeBlocksStatus.set(i, lastState.getFreeBlocksStatus().get(i));
            }
        AllocatorState newState = new AllocatorState(request.getId(),
                request.getAction(),
                lastState.getBlockLength(),
                lastState.getAmountOfBlocks(),
                freeBlocksStatus,
                lastState.getAllocatedBlocks());
        return new Result<>(request, newState, true);
    }

    private Result<AllocatorState, AllocatorAction> createRequestMessage(AllocatorState lastState, CreateRequestMessageRequest request) {
        int fixedBegin = fixBlockBegin(request.getPieceLength(), request.getBegin());
        int fixedBlockLength = fixBlockLength(request.getPieceLength(), fixedBegin, request.getBlockLength(), lastState.getBlockLength());

        AllocatorState newState = new AllocatorState(request.getId(),
                request.getAction(),
                lastState.getBlockLength(),
                lastState.getAmountOfBlocks(),
                lastState.getFreeBlocksStatus(),
                lastState.getAllocatedBlocks());

        RequestMessage requestMessage = new RequestMessage(request.getFrom(), request.getTo(),
                request.getIndex(), fixedBegin, fixedBlockLength);

        return new CreateRequestMessageResult(request, newState, true, requestMessage);
    }

    private Result<AllocatorState, AllocatorAction> createPieceMessage(AllocatorState lastState, CreatePieceMessageRequest request) {
        int freeIndex = lastState.getFreeBlocksStatus().nextSetBit(0);
        if (freeIndex < 0)
            return new Result<AllocatorState, AllocatorAction>(request, lastState, false);
        int fixedBegin = fixBlockBegin(request.getPieceLength(), request.getBegin());
        int fixedBlockLength = fixBlockLength(request.getPieceLength(), fixedBegin, request.getBlockLength(), lastState.getBlockLength());

        BitSet freeBlocksStatus = new BitSet(lastState.getAmountOfBlocks());
        freeBlocksStatus.set(freeIndex, false);
        AllocatedBlock[] allocatedBlocks = new AllocatedBlock[lastState.getAmountOfBlocks()];
        allocatedBlocks[freeIndex] = new AllocatedBlockImpl(freeIndex,
                lastState.getAllocatedBlocks()[freeIndex].getBlock(),
                0,
                fixedBlockLength);
        for (int i = 0; i < allocatedBlocks.length; i++)
            if (i != freeIndex) {
                allocatedBlocks[i] = lastState.getAllocatedBlocks()[i];
                freeBlocksStatus.set(i, lastState.getFreeBlocksStatus().get(i));
            }
        AllocatorState newState = new AllocatorState(request.getId(),
                request.getAction(),
                lastState.getBlockLength(),
                lastState.getAmountOfBlocks(),
                freeBlocksStatus,
                allocatedBlocks);

        PieceMessage pieceMessage = new PieceMessage(request.getFrom(), request.getTo(),
                request.getIndex(), fixedBegin, newState.getAllocatedBlocks()[freeIndex]);

        return new CreatePieceMessageResult(request, newState, true, pieceMessage);
    }

    private static int fixBlockBegin(int pieceLength, int oldBegin) {
        return Math.min(oldBegin, pieceLength - 1);
    }

    private static int fixBlockLength(int pieceLength, int begin, int blockLength, int allocatedBlockLength) {
        assert fixBlockBegin(pieceLength, begin) == begin;

        // the block size we determined for the test may be bigger then the
        // length of this hole piece so we can't represent a block which is
        // bigger then his corresponding piece.
        int fixedAllocatedBlockLength = Math.min(allocatedBlockLength, pieceLength);

        int newBlockLength = Math.min(fixedAllocatedBlockLength, blockLength);

        // is pieceMessage.getBegin() + newBlockLength overlaps with the range of this piece?
        if (pieceLength < begin + newBlockLength) {
            // (1) newBlockLength <= allocatedBlockLength
            // (1) -> (2) pieceLength - pieceMessage.getBegin() < newBlockLength <= allocatedBlockLength <= Integer.MAX_VALUE
            // (2) -> (3) pieceLength - pieceMessage.getBegin() < Integer.MAX_VALUE
            newBlockLength = pieceLength - begin;
            // (4) ->  newBlockLength = (pieceLength - pieceMessage.getBegin()) <= pieceLength
            // (4) -> (5) -> newBlockLength <= pieceLength
        }
        return newBlockLength;
    }

    private static AllocatorState createInitialState(int amountOfBlocks, int blockLength) {
        BitSet freeBlocksStatus = new BitSet(amountOfBlocks);
        freeBlocksStatus.set(0, amountOfBlocks, true);
        AllocatedBlock[] allocatedBlocks = IntStream.range(0, amountOfBlocks)
                .mapToObj(index -> new AllocatedBlockImpl(index, blockLength))
                .toArray(AllocatedBlock[]::new);

        return new AllocatorState("INITIALIZE-id", AllocatorAction.INITIALIZE,
                blockLength, amountOfBlocks, freeBlocksStatus, allocatedBlocks);
    }
}
