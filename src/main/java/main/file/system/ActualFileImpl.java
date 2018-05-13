package main.file.system;

import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;

public class ActualFileImpl implements ActualFile {
    private String filePath;
    // 0 <= from <=to <= Hole-Torrent.size
    private long from, to; // inclusive.
    private SeekableByteChannel seekableByteChannel;

    public ActualFileImpl(String filePath, long from, long to, SeekableByteChannel seekableByteChannel) {
        this.filePath = filePath;
        this.from = from;
        this.to = to;
        this.seekableByteChannel = seekableByteChannel;
    }

    @Override
    public synchronized Mono<ActualFileImpl> closeFileChannel() {
        try {
            if (this.seekableByteChannel.isOpen())
                this.seekableByteChannel.close();
        } catch (IOException e) {
            return Mono.error(e);
        }
        return Mono.just(this);
    }

    @Override
    public synchronized void writeBlock(long begin, byte[] block, int arrayIndexFrom, int howMuchToWriteFromArray) throws IOException {
        assert this.from <= begin && begin < this.to;
        assert howMuchToWriteFromArray <= this.to - this.from;
        assert arrayIndexFrom < block.length;
        assert howMuchToWriteFromArray <= block.length;

        long writeFrom = begin - this.from;
        this.seekableByteChannel.position(writeFrom);

        ByteBuffer wrap = ByteBuffer.wrap(block, arrayIndexFrom, howMuchToWriteFromArray);
        this.seekableByteChannel.write(wrap);
    }

    @Override
    public synchronized void readBlock(long begin, int blockLength, byte[] readTo, int offset) throws IOException {
        assert this.from <= begin && begin < this.to;
        assert blockLength <= this.to - this.from;

        long readFrom = begin - this.from;
        this.seekableByteChannel.position(readFrom);
        ByteBuffer block = ByteBuffer.wrap(readTo, offset, blockLength);
        this.seekableByteChannel.read(block);
    }

    @Override
    public String getFilePath() {
        return this.filePath;
    }

    @Override
    public long getLength() {
        return this.to - this.from;
    }

    @Override
    public long getFrom() {
        return from;
    }

    @Override
    public long getTo() {
        return to;
    }
}
