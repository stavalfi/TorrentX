package main.file.system;

public class AllocatedBlock {
    private int blockIndex;
    private byte[] block;

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


}
