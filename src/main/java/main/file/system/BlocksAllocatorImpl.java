package main.file.system;

import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Arrays;
import java.util.BitSet;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.concurrent.atomic.AtomicLong;
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
        assert 0 < blockLength;
        assert 0 < amountOfBlocks;
        this.blockLength = blockLength;
        this.amountOfBlocks = amountOfBlocks;
        this.allocations = IntStream.range(0, amountOfBlocks)
                .mapToObj(allocationIndex -> new AllocatedBlockImpl(allocationIndex, blockLength))
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
                        AllocatedBlock newAllocatedBlock = new AllocatedBlockImpl(oldAllocatedBlock.getBlockIndex(),
                                this.allocations[freeBlock.getAsInt()].getBlock(),
                                false, offset, length);
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
    public AllocatedBlock free(AllocatedBlock allocatedBlock) {
        synchronized (this.notifyOnFreeIndex) {
            if (!allocatedBlock.isFreed() &&
                    !this.freeBlocksStatus.get(allocatedBlock.getBlockIndex()) &&
                    this.allocations[allocatedBlock.getBlockIndex()].getAllocationId().equals(allocatedBlock.getAllocationId())) {
                this.freeBlocksStatus.set(allocatedBlock.getBlockIndex());
                this.notifyOnFreeIndex.notify();
                this.freesSink.next(allocatedBlock.getBlockIndex());
                return new AllocatedBlockImpl(allocatedBlock.getBlockIndex(),
                        allocatedBlock.getBlock(),
                        true,
                        allocatedBlock.getOffset(),
                        allocatedBlock.getLength());
            }
            return allocatedBlock;
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
        assert length <= this.blockLength;
        assert this.allocations[oldAllocatedBlock.getBlockIndex()].getOffset() + length <= this.blockLength;
        // check if someone try to update a freed block.
        assert this.allocations[oldAllocatedBlock.getBlockIndex()].getAllocationId().equals(oldAllocatedBlock.getAllocationId());
        assert !oldAllocatedBlock.isFreed();

        AllocatedBlock newAllocatedBlock = new AllocatedBlockImpl(oldAllocatedBlock.getBlockIndex(),
                oldAllocatedBlock.getBlock(), oldAllocatedBlock.isFreed(), oldAllocatedBlock.getOffset(), length);
        this.allocations[oldAllocatedBlock.getBlockIndex()] = newAllocatedBlock;
        return newAllocatedBlock;
    }

    private static BlocksAllocator instance = new BlocksAllocatorImpl(2, 17_000);

    public static BlocksAllocator getInstance() {
        return instance;
    }

    static class AllocatedBlockImpl implements AllocatedBlock {
        private static AtomicLong idCounter = new AtomicLong();
        private int blockIndex;
        private String allocationId;
        private byte[] block;
        private boolean isFreed;
        private int offset, length;

        AllocatedBlockImpl(int blockIndex, int blockSize) {
            this.blockIndex = blockIndex;
            this.block = new byte[blockSize];
            this.allocationId = String.valueOf(AllocatedBlockImpl.idCounter.getAndIncrement());
            this.isFreed = false;
        }

        AllocatedBlockImpl(int blockIndex, byte[] block, boolean isFreed, int offset, int length) {
            assert 0 <= offset && offset < block.length;
            assert length <= block.length - offset;
            this.allocationId = String.valueOf(idCounter.getAndIncrement());
            this.blockIndex = blockIndex;
            this.block = block;
            this.isFreed = isFreed;
            this.offset = offset;
            this.length = length;
        }

        @Override
        public byte[] getBlock() {
            return block;
        }

        @Override
        public int getBlockIndex() {
            return blockIndex;
        }

        @Override
        public int getOffset() {
            return offset;
        }

        @Override
        public int getLength() {
            return length;
        }

        @Override
        public String getAllocationId() {
            return allocationId;
        }

        @Override
        public boolean isFreed() {
            return isFreed;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof AllocatedBlock)) return false;
            AllocatedBlock that = (AllocatedBlock) o;
            if (getLength() == that.getLength()) {
                for (int i = this.offset; i < this.length; i++)
                    if (this.block[i] != that.getBlock()[i])
                        return false;
            }
            return true;
        }

        @Override
        public int hashCode() {
            int result = Objects.hash(getLength());
            return result;
        }

        @Override
        public String toString() {
            return "AllocatedBlock{" +
                    "blockIndex=" + blockIndex +
                    ", blockLength=" + block.length +
                    ", offset=" + offset +
                    ", length=" + length +
                    '}';
        }
    }
}
