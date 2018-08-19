package main.file.system.allocator.requests;

import main.file.system.allocator.AllocatedBlock;
import main.file.system.allocator.AllocatorAction;
import redux.store.Request;

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
