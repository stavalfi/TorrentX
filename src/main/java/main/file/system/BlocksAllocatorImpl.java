package main.file.system;

import main.peer.Peer;
import main.peer.peerMessages.PieceMessage;
import main.peer.peerMessages.RequestMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Arrays;
import java.util.BitSet;
import java.util.stream.IntStream;

public class BlocksAllocatorImpl implements BlocksAllocator {
    private static Logger logger = LoggerFactory.getLogger(BlocksAllocatorImpl.class);

    private FluxSink<AllocatorState> latestStateSink;
    private Flux<AllocatorState> latestState$;
    private Flux<AllocatorState> stateHistory$;

    public BlocksAllocatorImpl(int amountOfBlocks, int blockLength) {
        this.latestState$ = Flux.<AllocatorState>create(sink -> {
            this.latestStateSink = sink;
            this.latestStateSink.next(createInitialState(amountOfBlocks, blockLength));
        }).replay(1).autoConnect(0);

        this.stateHistory$ = this.latestState$.replay(10)
                .autoConnect(0);
    }

    @Override
    public Mono<RequestMessage> createRequestMessage(Peer from, Peer to, int index,
                                                     int begin, int blockLength,
                                                     int pieceLength) {
        assert begin >= 0;
        assert blockLength > 0;
        assert pieceLength > 0;

        return this.latestState$
                .publishOn(Schedulers.single())
                .take(1)
                .single()
                .map(AllocatorState::getBlockLength)
                .map(allocatedBlockLength -> {
                    int fixedBegin = fixBlockBegin(pieceLength, begin);
                    int fixedBlockLength = fixBlockLength(pieceLength, fixedBegin, blockLength, allocatedBlockLength);
                    return new RequestMessage(from, to, index, fixedBegin, fixedBlockLength);
                })
                .publishOn(Schedulers.parallel());
    }

    @Override
    public Mono<PieceMessage> createPieceMessage(Peer from, Peer to,
                                                 int pieceIndex, int begin, int blockLength,
                                                 int pieceLength) {
        assert begin >= 0;
        assert blockLength > 0;
        assert pieceLength > 0;

        return this.latestState$.publishOn(Schedulers.single())
                .filter(oldState -> isNotFull(oldState.getFreeBlocksStatus()))
                // downstream is pushing new state so I will
                // receive it here and we will have an infinite loop
                // so I must take only the first state that passed the filter.
                .take(1)
                .single()
                .doOnNext(oldState -> logger.debug("oldState: " + oldState))
                .flatMap(oldState -> {
                    int freeIndex = oldState.getFreeBlocksStatus().nextSetBit(0);
                    int fixedBegin = fixBlockBegin(pieceLength, begin);
                    int fixedBlockLength = fixBlockLength(pieceLength, fixedBegin, blockLength, oldState.getBlockLength());
                    int allocatedBlockOffset = 0;

                    return createNewStateForAllocation(oldState, freeIndex, allocatedBlockOffset, fixedBlockLength)
                            .flatMap(newState -> pushNewState(newState))
                            .map(newAllocatorState -> new PieceMessage(from, to, pieceIndex, fixedBegin, newAllocatorState.getAllocatedBlocks()[freeIndex]));
                })
                .publishOn(Schedulers.parallel());
    }

    @Override
    public Mono<AllocatorState> free(AllocatedBlock allocatedBlock) {
        return this.latestState$.publishOn(Schedulers.single())
                .take(1)
                .single()
                // check if the allocator didn't shrink the blocks length and if yes, this block is free automatically.
                .filter(oldState -> allocatedBlock.getBlockIndex() < oldState.getAmountOfBlocks())
                // check if the index is specified as not free.
                .filter(oldState -> !oldState.getFreeBlocksStatus().get(allocatedBlock.getBlockIndex()))
                // check if the given allocatedBlock is present.
                .filter(oldState -> Arrays.stream(oldState.getAllocatedBlocks())
                        .anyMatch(allocatedBlock1 -> allocatedBlock.getAllocationId()
                                .equals(allocatedBlock1.getAllocationId())))
                .doOnNext(oldState -> logger.debug("oldState: " + oldState))
                .flatMap(oldState -> createNewStateToFreeAllocation(oldState, allocatedBlock))
                .flatMap(newState -> pushNewState(newState))
                .switchIfEmpty(getLatestState$())
                .publishOn(Schedulers.parallel());
    }

    @Override
    public Mono<AllocatorState> updateAllocations(int amountOfBlocks, int blockLength) {
        assert amountOfBlocks > 0;
        assert blockLength > 0;

        return this.latestState$.publishOn(Schedulers.single())
                .take(1)
                .single()
                .doOnNext(oldState -> logger.debug("oldState: " + oldState))
                .flatMap(oldState -> createNewStateToUpdateAllocationArray(oldState, amountOfBlocks, blockLength))
                .flatMap(newState -> pushNewState(newState))
                .switchIfEmpty(getLatestState$())
                .publishOn(Schedulers.parallel());
    }

    @Override
    public Mono<AllocatorState> freeAll() {
        return this.latestState$.publishOn(Schedulers.single())
                .take(1)
                .single()
                .doOnNext(oldState -> logger.debug("oldState: " + oldState))
                .map(allocatorState -> allocatorState.getAllocatedBlocks())
                .flatMapMany(array -> Flux.fromArray(array))
                .flatMap(allocatedBlock -> free(allocatedBlock))
                .switchIfEmpty(getLatestState$())
                .last()
                .publishOn(Schedulers.parallel());
    }

    @Override
    public Mono<AllocatorState> getLatestState$() {
        return this.latestState$.take(1)
                .single();
    }

    @Override
    public Flux<AllocatorState> getState$() {
        return this.latestState$;
    }

    @Override
    public Flux<AllocatorState> getStateHistory$() {
        return this.stateHistory$;
    }

    private Mono<AllocatorState> pushNewState(AllocatorState newState) {
        // the following method won't cause any context switch.
        // if it will, this class won't be thread-safe.
        this.latestStateSink.next(newState);
        // Wait until the new status is updated.
        // Note: I can't use getState$() instead of this.latestState$
        // because getState$() contain publishOn(Scheduler.single()) and it
        // will do a context switch (even to the same thread)
        // and then other threads will execute this method pipeline
        // but they must have been blocked because they should
        // enter only when I see that the new state is entered and all
        // can consume it as the threads who wait outside of the pipeline to execute it.
        return this.latestState$.filter(o -> newState.equals(o))
                .take(1)
                .single();
    }

    private AllocatorState createInitialState(int amountOfBlocks, int blockLength) {
        BitSet freeBlocksStatus = new BitSet(amountOfBlocks);
        freeBlocksStatus.set(0, amountOfBlocks, true);
        AllocatedBlock[] allocatedBlocks = IntStream.range(0, amountOfBlocks)
                .mapToObj(index -> new AllocatedBlockImpl(index, blockLength))
                .toArray(AllocatedBlock[]::new);

        return new AllocatorState(blockLength, amountOfBlocks, freeBlocksStatus, allocatedBlocks);
    }

    private Mono<AllocatorState> createNewStateToUpdateAllocationArray(AllocatorState oldState,
                                                                       int amountOfBlocks, int blockLength) {
        if (oldState.getAmountOfBlocks() == amountOfBlocks &&
                oldState.getBlockLength() == blockLength)
            return Mono.empty();

        BitSet freeBlocksStatus = new BitSet(amountOfBlocks);
        AllocatedBlock[] allocatedBlocks = new AllocatedBlock[amountOfBlocks];
        int min = Math.min(oldState.getAmountOfBlocks(), amountOfBlocks);
        int max = Math.max(oldState.getAmountOfBlocks(), amountOfBlocks);

        IntStream.range(0, min)
                .peek(index -> freeBlocksStatus.set(index, oldState.getFreeBlocksStatus().get(index)))
                .forEach(index -> allocatedBlocks[index] = new AllocatedBlockImpl(
                        oldState.getAllocatedBlocks()[index].getAllocationId(),
                        index,
                        blockLength));
        if (oldState.getAmountOfBlocks() < amountOfBlocks) {
            freeBlocksStatus.set(min, max, true);
            IntStream.range(min, max)
                    .forEach(index -> allocatedBlocks[index] = new AllocatedBlockImpl(index, blockLength));
        }
        AllocatorState newState = new AllocatorState(blockLength,
                amountOfBlocks,
                freeBlocksStatus,
                allocatedBlocks);

        if (oldState.equals(newState))
            return Mono.error(new IllegalStateException("the state must have changed because we " +
                    "changed the blockAllocator array to different size. the new state: " +
                    newState));
        return Mono.just(newState);
    }

    private Mono<AllocatorState> createNewStateToFreeAllocation(AllocatorState oldState, AllocatedBlock allocatedBlock) {
        BitSet freeBlocksStatus = new BitSet(oldState.getAmountOfBlocks());
        freeBlocksStatus.set(allocatedBlock.getBlockIndex(), true);
        for (int i = 0; i < oldState.getAmountOfBlocks(); i++)
            if (i != allocatedBlock.getBlockIndex()) {
                freeBlocksStatus.set(i, oldState.getFreeBlocksStatus().get(i));
            }
        AllocatorState newState = new AllocatorState(oldState.getBlockLength(),
                oldState.getAmountOfBlocks(),
                freeBlocksStatus,
                oldState.getAllocatedBlocks());

        if (oldState.equals(newState))
            return Mono.error(new IllegalStateException("in the old state, the used-index was: " + allocatedBlock.getBlockIndex() +
                    " and also in the new state even when we tried to free it there. the new state: " +
                    newState));
        return Mono.just(newState);
    }

    private Mono<AllocatorState> createNewStateForAllocation(AllocatorState oldState, int freeIndex, int offset, int length) {
        BitSet freeBlocksStatus = new BitSet(oldState.getAmountOfBlocks());
        freeBlocksStatus.set(freeIndex, false);
        AllocatedBlock[] allocatedBlocks = new AllocatedBlock[oldState.getAmountOfBlocks()];
        allocatedBlocks[freeIndex] = new AllocatedBlockImpl(freeIndex,
                oldState.getAllocatedBlocks()[freeIndex].getBlock(),
                offset,
                length);
        for (int i = 0; i < allocatedBlocks.length; i++)
            if (i != freeIndex) {
                allocatedBlocks[i] = oldState.getAllocatedBlocks()[i];
                freeBlocksStatus.set(i, oldState.getFreeBlocksStatus().get(i));
            }
        AllocatorState newState = new AllocatorState(oldState.getBlockLength(),
                oldState.getAmountOfBlocks(),
                freeBlocksStatus,
                allocatedBlocks);
        if (oldState.equals(newState))
            return Mono.error(new IllegalStateException("in the old state, the free-index was: " + freeIndex +
                    " and also in the new state even when we tried to put allocation there. the new state: " +
                    newState));
        return Mono.just(newState);
    }

    private boolean isNotFull(BitSet freeBlocksStatus) {
        // isEmpty return true if all bits are false.
        return !freeBlocksStatus.isEmpty();
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

    public static BlocksAllocator instance = new BlocksAllocatorImpl(1, 1);

    public static BlocksAllocator getInstance() {
        return instance;
    }

}
