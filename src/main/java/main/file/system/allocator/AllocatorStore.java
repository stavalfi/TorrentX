package main.file.system.allocator;

import main.peer.Peer;
import main.peer.peerMessages.PieceMessage;
import main.peer.peerMessages.RequestMessage;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import redux.store.StoreNew;

public class AllocatorStore implements BlocksAllocator {

	private StoreNew<AllocatorState, AllocatorAction> allocatorStore = new StoreNew<>(new AllocatorReducer(),
			AllocatorReducer.defaultAllocatorState);

	@Override
	public Mono<AllocatorState> updateAllocations(int amountOfBlocks, int blockLength) {
		return Mono.empty();
//		UpdateAllocationArrayRequest request =
//				new UpdateAllocationArrayRequest(amountOfBlocks, blockLength);
//		return this.allocatorStore.dispatch(request)
//				.map();
	}

	@Override
	public Mono<PieceMessage> createPieceMessage(Peer from, Peer to, int index, int begin, int blockLength, int pieceLength) {
		assert begin >= 0;
		assert blockLength > 0;
		assert pieceLength > 0;
		return null;
//		return Mono.just(new Object())
//				.publishOn(Schedulers.single())
//				.flatMapMany(__ -> this.latestState$)
//				.filter(oldState -> isNotFull(oldState.getFreeBlocksStatus()))
//				// downstream is pushing new state so I will
//				// receive it here and we will have an infinite loop
//				// so I must take only the first state that passed the filter.
//				.take(1)
//				.single()
//				.flatMap(oldState -> {
//					int freeIndex = oldState.getFreeBlocksStatus().nextSetBit(0);
//					int fixedBegin = fixBlockBegin(pieceLength, begin);
//					int fixedBlockLength = fixBlockLength(pieceLength, fixedBegin, blockLength, oldState.getBlockLength());
//					int allocatedBlockOffset = 0;
//
//					return createNewStateForAllocation(oldState, freeIndex, allocatedBlockOffset, fixedBlockLength)
//							.flatMap(newState -> pushNewState(newState))
//							.map(newAllocatorState -> new PieceMessage(from, to, pieceIndex, fixedBegin, newAllocatorState.getAllocatedBlocks()[freeIndex]));
//				})
//				.publishOn(Schedulers.parallel());
	}

	@Override
	public Mono<RequestMessage> createRequestMessage(Peer from, Peer to, int index, int begin, int blockLength, int pieceLength) {
		return null;
	}

	@Override
	public Mono<AllocatorState> free(AllocatedBlock allocatedBlock) {
		return null;
	}

	@Override
	public Mono<AllocatorState> freeAll() {
		return null;
	}

	@Override
	public Mono<AllocatorState> getLatestState$() {
		return null;
	}

	@Override
	public Flux<AllocatorState> getState$() {
		return null;
	}

	@Override
	public Flux<AllocatorState> getStateHistory$() {
		return null;
	}
}

