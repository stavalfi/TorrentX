package main.file.system.allocator.requests;

import main.file.system.allocator.AllocatorAction;
import redux.store.Request;

public class UpdateAllocationArrayRequest extends Request<AllocatorAction> {
	private int amountOfBlocks;
	private int blockLength;

	public UpdateAllocationArrayRequest(int amountOfBlocks, int blockLength) {
		super(AllocatorAction.UPDATE_ALLOCATIONS_ARRAY);
		this.amountOfBlocks = amountOfBlocks;
		this.blockLength = blockLength;
	}


	public int getAmountOfBlocks() {
		return amountOfBlocks;
	}

	public int getBlockLength() {
		return blockLength;
	}

	@Override
	public String toString() {
		return "UpdateAllocationArrayRequest{" +
				"amountOfBlocks=" + amountOfBlocks +
				", blockLength=" + blockLength +
				'}';
	}
}
