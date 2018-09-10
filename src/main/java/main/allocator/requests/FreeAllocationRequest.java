package main.allocator.requests;

import main.allocator.AllocatedBlock;
import main.allocator.AllocatorAction;
import main.redux.store.Request;

public class FreeAllocationRequest extends Request<AllocatorAction> {
	private AllocatedBlock allocatedBlock;

	public FreeAllocationRequest(AllocatedBlock allocatedBlock) {
		super(AllocatorAction.FREE_ALLOCATION);
		this.allocatedBlock = allocatedBlock;
	}

	public AllocatedBlock getAllocatedBlock() {
		return allocatedBlock;
	}

	@Override
	public String toString() {
		return "FreeAllocationRequest{" +
				"allocatedBlock=" + allocatedBlock +
				'}';
	}
}
