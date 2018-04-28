package main.file.system;

public class AllocatedBlock {
    int blockIndex;
    byte[] allocation;
    int from;
    int to;

    public AllocatedBlock(byte[] allocation, int from, int to) {
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
}
