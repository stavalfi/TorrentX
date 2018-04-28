package main.file.system;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.BitSet;

public interface BlocksAllocator {

    Mono<AllocatedBlock> allocate();

    void free(AllocatedBlock allocatedBlock);

    Flux<Integer> allocations$();

    Flux<Integer> frees$();

    BitSet currentStatus();
}
