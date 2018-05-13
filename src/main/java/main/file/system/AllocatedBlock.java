package main.file.system;

public interface AllocatedBlock {
    byte[] getBlock();

    int getBlockIndex();

    int getOffset();

    int getLength();

    String getAllocationId();
}
