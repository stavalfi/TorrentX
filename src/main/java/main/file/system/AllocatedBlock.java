package main.file.system;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

// TODO: make it inner class of BlockAllocator so only he will have access to the property: blockIndex.
public class AllocatedBlock {
    private static AtomicLong idCounter = new AtomicLong();

    private int blockIndex;
    private String allocationId;
    private byte[] block;
    private int offset, length;

    public AllocatedBlock(int blockIndex, int blockSize) {
        this.blockIndex = blockIndex;
        this.block = new byte[blockSize];
        this.allocationId = String.valueOf(idCounter.getAndIncrement());
        ;
    }

    public AllocatedBlock(int blockIndex, byte[] block, int offset, int length) {
        assert 0 <= offset && offset < block.length;
        assert length <= block.length - offset;
        this.allocationId = String.valueOf(idCounter.getAndIncrement());
        ;
        this.blockIndex = blockIndex;
        this.block = block;
        this.offset = offset;
        this.length = length;
    }

    public byte[] getBlock() {
        return block;
    }

    public int getBlockIndex() {
        return blockIndex;
    }

    public int getOffset() {
        return offset;
    }

    public int getLength() {
        return length;
    }

    public String getAllocationId() {
        return allocationId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AllocatedBlock)) return false;
        AllocatedBlock that = (AllocatedBlock) o;
        return Objects.equals(getAllocationId(), that.getAllocationId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getAllocationId());
    }

    @Override
    public String toString() {
        return "AllocatedBlock{" +
                "blockIndex=" + blockIndex +
                ", blockLength=" + block.length +
                ", offset=" + offset +
                ", length=" + length +
                '}';
    }
}
