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

    public AllocatedBlock(int blockIndex, byte[] block, int offset, int length) {
        assert 0 <= offset && offset < block.length;
        assert length < block.length - offset;
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

    public int getActualLength() {
        return length;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AllocatedBlock)) return false;
        AllocatedBlock that = (AllocatedBlock) o;
        return getBlockIndex() == that.getBlockIndex() &&
                getOffset() == that.getOffset() &&
                getActualLength() == that.getActualLength();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getBlockIndex(), getOffset(), getActualLength());
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
