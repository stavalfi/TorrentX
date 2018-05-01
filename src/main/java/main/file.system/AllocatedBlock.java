package main.file.system;

public class AllocatedBlock {
    private int blockIndex;
    private byte[] block;
    private int from, to;

    public AllocatedBlock(int blockIndex, int blockSize, int from, int to) {
        this.blockIndex = blockIndex;
        this.block = new byte[blockSize];
        this.from = from;
        this.to = to;
    }

    public byte[] getBlock() {
        return block;
    }

    public int getBlockIndex() {
        return blockIndex;
    }

    public int getFrom() {
        return from;
    }

    public int getTo() {
        return to;
    }

    public void setFrom(int from) {
        this.from = from;
    }

    public void setTo(int to) {
        this.to = to;
    }
}
