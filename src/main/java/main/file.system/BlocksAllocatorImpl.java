package main.file.system;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.BitSet;

public class BlocksAllocatorImpl implements BlocksAllocator {
    private byte[] allocation;

    public BlocksAllocatorImpl(int blockLength, int amountOfBlocks) {
        assert blockLength > 0;
        assert amountOfBlocks > 0;
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
    public BitSet currentStatus() {
        return null;
    }
}
