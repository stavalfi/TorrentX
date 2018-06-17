package main.file.system.allocator;

import redux.state.State;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.Arrays;
import java.util.BitSet;
import java.util.Objects;

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
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof AllocatorState)) return false;
		AllocatorState that = (AllocatorState) o;
		return getBlockLength() == that.getBlockLength() &&
				getAmountOfBlocks() == that.getAmountOfBlocks() &&
				Objects.equals(getFreeBlocksStatus(), that.getFreeBlocksStatus()) &&
				Arrays.equals(getAllocatedBlocks(), that.getAllocatedBlocks());
	}

	@Override
	public int hashCode() {

		int result = Objects.hash(getBlockLength(), getAmountOfBlocks(), getFreeBlocksStatus());
		result = 31 * result + Arrays.hashCode(getAllocatedBlocks());
		return result;
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
