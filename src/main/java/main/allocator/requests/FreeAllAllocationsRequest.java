package main.allocator.requests;

import main.allocator.AllocatorAction;
import main.redux.store.Request;

public class FreeAllAllocationsRequest extends Request<AllocatorAction> {
	public FreeAllAllocationsRequest() {
		super(AllocatorAction.FREE_ALL_ALLOCATIONS);
	}

	@Override
	public String toString() {
		return "FreeAllAllocationsRequest{}";
	}
}
