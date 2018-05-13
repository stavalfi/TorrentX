package main.file.system;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

public class AllocatedBlockImpl implements AllocatedBlock {
    private static AtomicLong idCounter = new AtomicLong();
    private int blockIndex;
    private String allocationId;
    private byte[] block;
    private int offset, length;

    AllocatedBlockImpl(int blockIndex, int blockLength) {
        this.blockIndex = blockIndex;
        this.block = new byte[blockLength];
        this.allocationId = String.valueOf(AllocatedBlockImpl.idCounter.getAndIncrement());
    }

    AllocatedBlockImpl(int blockIndex, byte[] block, int offset, int length) {
        assert 0 <= offset && offset < block.length;
        assert length <= block.length - offset;
        this.allocationId = String.valueOf(idCounter.getAndIncrement());
        this.blockIndex = blockIndex;
        this.block = block;
        this.offset = offset;
        this.length = length;
    }

    AllocatedBlockImpl(String allocationId, int blockIndex, int blockLength) {
        this.allocationId = allocationId;
        this.blockIndex = blockIndex;
        this.block = new byte[blockLength];
    }

    @Override
    public byte[] getBlock() {
        return block;
    }

    @Override
    public int getBlockIndex() {
        return blockIndex;
    }

    @Override
    public int getOffset() {
        return offset;
    }

    @Override
    public int getLength() {
        return length;
    }

    @Override
    public String getAllocationId() {
        return allocationId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AllocatedBlock)) return false;
        AllocatedBlock that = (AllocatedBlock) o;
        if (getLength() == that.getLength()) {
            for (int i = this.offset; i < this.length; i++)
                if (this.block[i] != that.getBlock()[i])
                    return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getLength());
    }

    @Override
    public String toString() {
        return "AllocatedBlock{" +
                "blockIndex=" + blockIndex +
                ", length=" + length +
                '}';
    }
}
