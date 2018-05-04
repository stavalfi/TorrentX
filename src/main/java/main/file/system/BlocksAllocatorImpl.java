package main.file.system;

import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Arrays;
import java.util.BitSet;
import java.util.OptionalInt;
import java.util.stream.IntStream;

public class BlocksAllocatorImpl implements BlocksAllocator {
    private AllocatedBlock[] allocations;
    private BitSet freeBlocksStatus;
    private int blockLength;
    private int amountOfBlocks;
    private Flux<Integer> frees$;
    private FluxSink<Integer> freesSink;
    private Flux<Integer> allocated$;
    private FluxSink<Integer> allocatedSink;
    private final Object notifyOnFreeIndex = new Object();

    public BlocksAllocatorImpl(int amountOfBlocks, int blockLength) {
        assert blockLength > 0;
        assert amountOfBlocks > 0;
        this.blockLength = blockLength;
        this.amountOfBlocks = amountOfBlocks;
        this.allocations = IntStream.range(0, amountOfBlocks)
                .mapToObj(allocationIndex -> new AllocatedBlock(allocationIndex, blockLength))
                .toArray(AllocatedBlock[]::new);
        this.freeBlocksStatus = new BitSet(amountOfBlocks);
        this.freeBlocksStatus.set(0, amountOfBlocks);
        this.frees$ = Flux.<Integer>create(sink -> this.freesSink = sink)
                .publish()
                .autoConnect(0);
        this.allocated$ = Flux.<Integer>create(sink -> this.allocatedSink = sink)
                .publish()
                .autoConnect(0);
    }

    @Override
    public Mono<AllocatedBlock> allocate(int offset, int length) {
        return Mono.<AllocatedBlock>create(sink -> {
            while (true) {
                synchronized (this.notifyOnFreeIndex) {
                    OptionalInt freeBlock = IntStream.range(0, amountOfBlocks)
                            .filter(isFreeBlock -> this.freeBlocksStatus.get(isFreeBlock))
                            .findAny();
                    if (freeBlock.isPresent()) {
                        this.freeBlocksStatus.set(freeBlock.getAsInt(), false);
                        AllocatedBlock oldAllocatedBlock = this.allocations[freeBlock.getAsInt()];
                        AllocatedBlock newAllocatedBlock = new AllocatedBlock(oldAllocatedBlock.getBlockIndex(),
                                this.allocations[freeBlock.getAsInt()].getBlock(),
                                offset, length);
                        this.allocations[freeBlock.getAsInt()] = newAllocatedBlock;
                        sink.success(newAllocatedBlock);
                        return;
                    }
                    try {
                        this.notifyOnFreeIndex.wait();
                    } catch (InterruptedException e) {
                        sink.error(e);
                        return;
                    }
                }
            }
        }).subscribeOn(Schedulers.elastic())
                .doOnNext(allocatedBlock -> this.allocatedSink.next(allocatedBlock.getBlockIndex()));
    }

    @Override
    public void free(AllocatedBlock allocatedBlock) {
        assert allocatedBlock.getBlock().length == this.blockLength;
        assert 0 <= allocatedBlock.getBlockIndex() && allocatedBlock.getBlockIndex() <= this.amountOfBlocks;
        assert !this.freeBlocksStatus.get(allocatedBlock.getBlockIndex());

        synchronized (this.notifyOnFreeIndex) {
            this.freeBlocksStatus.set(allocatedBlock.getBlockIndex());
            this.notifyOnFreeIndex.notify();
            this.freesSink.next(allocatedBlock.getBlockIndex());
        }
    }

    @Override
    public void freeAll() {
        Arrays.stream(this.allocations)
                .forEach(this::free);
    }

    @Override
    public Flux<Integer> allocated$() {
        return this.allocated$;
    }

    @Override
    public Flux<Integer> frees$() {
        return this.frees$;
    }

    @Override
    public BitSet getFreeBlocksStatus() {
        return this.freeBlocksStatus;
    }

    @Override
    public int getBlockLength() {
        return this.blockLength;
    }

    @Override
    public int getAmountOfBlocks() {
        return this.amountOfBlocks;
    }

    @Override
    public AllocatedBlock updateLength(AllocatedBlock oldAllocatedBlock, int length) {
        assert !this.freeBlocksStatus.get(oldAllocatedBlock.getBlockIndex());
        assert length < this.blockLength;
        assert this.allocations[oldAllocatedBlock.getBlockIndex()].getOffset() + length < this.blockLength;

        AllocatedBlock newAllocatedBlock = new AllocatedBlock(oldAllocatedBlock.getBlockIndex(),
                oldAllocatedBlock.getBlock(), oldAllocatedBlock.getOffset(), length);
        this.allocations[oldAllocatedBlock.getBlockIndex()] = newAllocatedBlock;
        return newAllocatedBlock;
    }

    private static BlocksAllocator instance = new BlocksAllocatorImpl(20_000, 17_000);

    public static BlocksAllocator getInstance() {
        return instance;
    }
}
