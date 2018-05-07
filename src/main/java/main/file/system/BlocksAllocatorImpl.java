package main.file.system;

import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Arrays;
import java.util.BitSet;
import java.util.stream.IntStream;

public class BlocksAllocatorImpl implements BlocksAllocator {
    private FluxSink<AllocatorState> latestStateSink;
    private Flux<AllocatorState> latestState$;
    private Flux<AllocatorState> stateHistory$;

    public BlocksAllocatorImpl(int amountOfBlocks, int blockLength) {
        this.latestState$ = Flux.<AllocatorState>create(sink -> this.latestStateSink = sink)
                .publishOn(Schedulers.single())
                .replay(1)
                .autoConnect(0);


        this.latestStateSink.next(createInitialState(amountOfBlocks, blockLength));

        this.stateHistory$ = this.latestState$.replay(10)
                .autoConnect(0);
    }

    @Override
    public Mono<AllocatedBlock> allocate(int offset, int length) {
        assert offset > 0;
        assert length > 0;

        return getState$().filter(allocatorState -> offset + length < allocatorState.getBlockLength())
                .filter(allocatorState -> isNotFull(allocatorState.getFreeBlocksStatus()))
                .take(1)
                .flatMap(oldState -> {
                    int freeIndex = oldState.getFreeBlocksStatus().nextSetBit(0);
                    return createNewStateToAllocation(oldState, freeIndex, offset, length)
                            .doOnNext(allocatorState -> this.latestStateSink.next(allocatorState))
                            // wait until the new status is updated.
                            .flatMapMany(allocatorState -> getState$().filter(allocatorState::equals))
                            .map(allocatorState -> allocatorState.getAllocatedBlocks()[freeIndex]);
                })
                .take(1)
                .single();
    }

    @Override
    public Mono<AllocatorState> free(AllocatedBlock allocatedBlock) {
        return getLatestState$()
                // check if the index is specified as not free.
                .filter(allocatorState -> !allocatorState.getFreeBlocksStatus().get(allocatedBlock.getBlockIndex()))
                // check if the given allocatedBlock is present.
                .filter(allocatorState -> Arrays.stream(allocatorState.getAllocatedBlocks())
                        .anyMatch(allocatedBlock1 -> allocatedBlock.getAllocationId()
                                .equals(allocatedBlock1.getAllocationId())))
                .flatMap(oldState -> createNewStateToFreeAllocation(oldState, allocatedBlock))
                .doOnNext(allocatorState -> this.latestStateSink.next(allocatorState))
                // wait until the new status is updated.
                .flatMapMany(allocatorState -> getState$().filter(allocatorState::equals))
                .take(1)
                .single();
    }

    @Override
    public Mono<AllocatorState> updateAllocations(int amountOfBlocks, int blockLength) {
        assert amountOfBlocks > 0;
        assert blockLength > 0;

        return getLatestState$()
                .flatMap(oldState -> createNewStateToUpdateAllocationArray(oldState, amountOfBlocks, blockLength))
                .doOnNext(allocatorState -> this.latestStateSink.next(allocatorState))
                // wait until the new status is updated.
                .flatMapMany(allocatorState -> getState$().filter(allocatorState::equals))
                .take(1)
                .single();
    }

    @Override
    public Mono<AllocatedBlock> updateAllocatedBlock(AllocatedBlock old, int offset, int length) {
        assert offset > 0;
        assert length > 0;

        return getLatestState$()
                // check if the index is specified as not free.
                .filter(allocatorState -> !allocatorState.getFreeBlocksStatus().get(old.getBlockIndex()))
                // check if the given allocatedBlock is present.
                .filter(allocatorState -> Arrays.stream(allocatorState.getAllocatedBlocks())
                        .anyMatch(allocatedBlock1 -> old.getAllocationId()
                                .equals(allocatedBlock1.getAllocationId())))
                .flatMap(oldState -> createNewStateToUpdateAllocatedBlock(oldState, old, offset, length))
                .doOnNext(allocatorState -> this.latestStateSink.next(allocatorState))
                // wait until the new status is updated.
                .flatMapMany(allocatorState -> getState$().filter(allocatorState::equals))
                .map(allocatorState -> allocatorState.getAllocatedBlocks()[old.getBlockIndex()])
                .take(1)
                .single();
    }

    @Override
    public Mono<AllocatorState> freeAll() {
        return getLatestState$().map(AllocatorState::getAllocatedBlocks)
                .flatMapMany(Flux::fromArray)
                .flatMap(this::free)
                .last();
    }

    @Override
    public Mono<AllocatorState> getLatestState$() {
        return this.latestState$.take(1).single();
    }

    @Override
    public Flux<AllocatorState> getState$() {
        return this.latestState$;
    }

    @Override
    public Flux<AllocatorState> getStateHistory$() {
        return this.stateHistory$;
    }

    private AllocatorState createInitialState(int amountOfBlocks, int blockLength) {
        BitSet freeBlocksStatus = new BitSet(amountOfBlocks);
        freeBlocksStatus.set(0, amountOfBlocks - 1, true);
        AllocatedBlock[] allocatedBlocks = IntStream.range(0, amountOfBlocks)
                .mapToObj(index -> new AllocatedBlockImpl(index, blockLength))
                .toArray(AllocatedBlock[]::new);

        return new AllocatorState(blockLength, amountOfBlocks, freeBlocksStatus, allocatedBlocks);
    }

    private Mono<AllocatorState> createNewStateToUpdateAllocatedBlock(AllocatorState oldState, AllocatedBlock old,
                                                                      int offset, int length) {
        if (old.getOffset() == offset &&
                old.getLength() == length)
            return Mono.empty();

        AllocatedBlock[] allocatedBlocks = new AllocatedBlock[oldState.getAmountOfBlocks()];
        allocatedBlocks[old.getBlockIndex()] = new AllocatedBlockImpl(old.getBlockIndex(),
                oldState.getAllocatedBlocks()[old.getBlockIndex()].getBlock(),
                offset,
                length);
        for (int i = 0; i < allocatedBlocks.length; i++)
            if (i != old.getBlockIndex()) {
                allocatedBlocks[i] = oldState.getAllocatedBlocks()[i];
            }
        AllocatorState newState = new AllocatorState(oldState.getBlockLength(),
                oldState.getAmountOfBlocks(),
                oldState.getFreeBlocksStatus(),
                oldState.getAllocatedBlocks());
        return oldState.equals(newState) ?
                Mono.empty() :
                Mono.just(newState);
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
        freeBlocksStatus.set(min, max, true);
        IntStream.rangeClosed(min, max)
                .forEach(index -> allocatedBlocks[index] = new AllocatedBlockImpl(index, blockLength));
        AllocatorState newState = new AllocatorState(blockLength,
                amountOfBlocks,
                freeBlocksStatus,
                allocatedBlocks);
        return oldState.equals(newState) ?
                Mono.empty() :
                Mono.just(newState);
    }

    private Mono<AllocatorState> createNewStateToFreeAllocation(AllocatorState oldState, AllocatedBlock allocatedBlock) {
        BitSet freeBlocksStatus = new BitSet(oldState.getAmountOfBlocks());
        freeBlocksStatus.set(allocatedBlock.getBlockIndex(), false);
        AllocatedBlock[] allocatedBlocks = new AllocatedBlock[oldState.getAmountOfBlocks()];
        allocatedBlocks[allocatedBlock.getBlockIndex()] = new AllocatedBlockImpl(allocatedBlock.getBlockIndex(),
                oldState.getAllocatedBlocks()[allocatedBlock.getBlockIndex()].getBlock(),
                0,
                0);
        for (int i = 0; i < allocatedBlocks.length; i++)
            if (i != allocatedBlock.getBlockIndex()) {
                allocatedBlocks[i] = oldState.getAllocatedBlocks()[i];
                freeBlocksStatus.set(i, oldState.getFreeBlocksStatus().get(i));
            }
        AllocatorState newState = new AllocatorState(oldState.getBlockLength(),
                oldState.getAmountOfBlocks(),
                oldState.getFreeBlocksStatus(),
                oldState.getAllocatedBlocks());
        return oldState.equals(newState) ?
                Mono.empty() :
                Mono.just(newState);
    }

    private Mono<AllocatorState> createNewStateToAllocation(AllocatorState oldState, int freeIndex, int offset, int length) {
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
                oldState.getFreeBlocksStatus(),
                oldState.getAllocatedBlocks());
        return oldState.equals(newState) ?
                Mono.empty() :
                Mono.just(newState);
    }

    private boolean isNotFull(BitSet freeBlocksStatus) {
        // isEmpty return true if all bits are false.
        return !freeBlocksStatus.isEmpty();
    }

    public static BlocksAllocator instance = new BlocksAllocatorImpl(1, 1);

    public static BlocksAllocator getInstance() {
        return instance;
    }

}
