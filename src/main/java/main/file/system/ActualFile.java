package main.file.system;

import reactor.core.publisher.Mono;

import java.io.IOException;

public interface ActualFile {
    String getFilePath();

    long getLength();

    long getFrom();

    long getTo();

    Mono<ActualFileImpl> closeFileChannel();

    void writeBlock(long begin, byte[] block, int arrayIndexFrom, int howMuchToWriteFromArray) throws IOException;

    void readBlock(long begin, int blockLength, byte[] readTo, int offset) throws IOException;
}
