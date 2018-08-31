package main.file.system.allocator.requests;

import main.file.system.allocator.AllocatorAction;
import redux.store.Request;

public class FreeAllAllocationsRequest extends Request<AllocatorAction> {
	public FreeAllAllocationsRequest() {
		super(AllocatorAction.FREE_ALL_ALLOCATIONS);
	}

	@Override
	public String toString() {
		return "FreeAllAllocationsRequest{}";
	}
}
