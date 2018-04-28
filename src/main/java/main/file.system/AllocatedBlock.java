package main.file.system;

public class AllocatedBlock {
    private int blockIndex;
    private byte[] allocation;
    private int from;
    private int to;

    public AllocatedBlock(int blockIndex, byte[] allocation, int from, int to) {
        this.blockIndex = blockIndex;
        this.allocation = allocation;
        this.from = from;
        this.to = to;
    }

    public byte[] getAllocation() {
        return allocation;
    }

    public int getFrom() {
        return from;
    }

    public int getTo() {
        return to;
    }

    public int getBlockIndex() {
        return blockIndex;
    }


}
