package main.file.system;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface BlocksAllocator {

    Mono<AllocatorState> updateAllocations(int amountOfBlocks, int blockLength);

    Mono<AllocatedBlock> updateAllocatedBlock(AllocatedBlock old, int offset, int length);

    Mono<AllocatedBlock> allocate(int offset, int length);

    Mono<AllocatorState> free(AllocatedBlock allocatedBlock);

    Mono<AllocatorState> freeAll();

    Mono<AllocatorState> getLatestState$();

    Flux<AllocatorState> getState$();

    Flux<AllocatorState> getStateHistory$();
}
