package main.file.system;

import java.util.Objects;

public class AllocatedBlock {
    private int blockIndex;
    private byte[] block;
    private int offset, length;

    public AllocatedBlock(int blockIndex, int blockSize) {
        this.blockIndex = blockIndex;
        this.block = new byte[blockSize];
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

    public AllocatedBlock setOffset(int offset) {
        assert 0 <= offset && offset < this.block.length;
        this.offset = offset;
        return this;
    }

    public AllocatedBlock setLength(int length) {
        assert length < this.block.length - offset;
        this.length = length;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AllocatedBlock)) return false;
        AllocatedBlock that = (AllocatedBlock) o;
        return getBlockIndex() == that.getBlockIndex() &&
                getOffset() == that.getOffset() &&
                getLength() == that.getLength();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getBlockIndex(), getOffset(), getLength());
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
