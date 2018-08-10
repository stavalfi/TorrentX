package main.file.system.allocator;

import redux.state.State;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.Arrays;
import java.util.BitSet;

public class AllocatorState extends State<AllocatorAction> {
	private int blockLength;
	private int amountOfBlocks;
	private BitSet freeBlocksStatus;
	private AllocatedBlock[] allocatedBlocks;

	public AllocatorState(String id, AllocatorAction action,
						  int blockLength, int amountOfBlocks, BitSet freeBlocksStatus, AllocatedBlock[] allocatedBlocks) {
		super(id, action);
		this.blockLength = blockLength;
		this.amountOfBlocks = amountOfBlocks;
		this.freeBlocksStatus = freeBlocksStatus;
		this.allocatedBlocks = allocatedBlocks;
	}

	public int getBlockLength() {
		return blockLength;
	}

	public int getAmountOfBlocks() {
		return amountOfBlocks;
	}

	public BitSet getFreeBlocksStatus() {
		return freeBlocksStatus;
	}

	public AllocatedBlock[] getAllocatedBlocks() {
		return allocatedBlocks;
	}

	@Override
	public boolean fromAction(AllocatorAction allocatorAction) {
		throw new NotImplementedException();
	}

	@Override
	public String toString() {
		return "AllocatorState{" + super.toString() +
				"blockLength=" + blockLength +
				", amountOfBlocks=" + amountOfBlocks +
				", freeBlocksStatus=" + freeBlocksStatus +
				", allocatedBlocks=" + Arrays.toString(allocatedBlocks) +
				'}';
	}
}
