package main.file.system;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.BitSet;

public class BlocksAllocatorImpl implements BlocksAllocator {
    private byte[] allocation;
    private int blockLength;
    private int amountOfBlocks;

    public BlocksAllocatorImpl(int amountOfBlocks, int blockLength) {
        assert blockLength > 0;
        assert amountOfBlocks > 0;
        this.blockLength = blockLength;
        this.amountOfBlocks = amountOfBlocks;
        this.allocation = new byte[blockLength * amountOfBlocks];
    }

    @Override
    public Mono<AllocatedBlock> allocate() {
        return Mono.empty();
    }

    @Override
    public void free(AllocatedBlock allocatedBlock) {

    }

    @Override
    public Flux<Integer> allocations$() {
        return Flux.empty();
    }

    @Override
    public Flux<Integer> frees$() {
        return Flux.empty();
    }

    @Override
    public BitSet currentFreeBlocksStatus() {
        return null;
    }

    @Override
    public int getBlockLength() {
        return blockLength;
    }

    @Override
    public int getAmountOfBlocks() {
        return amountOfBlocks;
    }
}
