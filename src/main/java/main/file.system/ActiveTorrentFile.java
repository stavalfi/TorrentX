package main.file.system;

import reactor.core.publisher.Mono;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

public class ActiveTorrentFile implements TorrentFile {
    private String filePath;
    private long from, to; // not closed range: [from,to).
    private RandomAccessFile randomAccessFileRead;
    private RandomAccessFile randomAccessFileWrite;

    public ActiveTorrentFile(String filePath, long from, long to) {
        this.filePath = filePath;
        this.from = from;
        this.to = to;
    }

    public synchronized Mono<Void> closeRandomAccessFiles() {
        try {
            if (this.randomAccessFileRead != null)
                this.randomAccessFileRead.close();
            if (this.randomAccessFileWrite != null)
                this.randomAccessFileWrite.close();
        } catch (IOException e) {
            return Mono.error(e);
        }
        return Mono.empty();
    }

    public synchronized RandomAccessFile getRandomAccessFileRead() throws FileNotFoundException {
        if (this.randomAccessFileRead == null)
            this.randomAccessFileRead = new RandomAccessFile(filePath, "rw");
        return this.randomAccessFileRead;
    }

    public synchronized RandomAccessFile getRandomAccessFileWrite() throws FileNotFoundException {
        if (this.randomAccessFileWrite == null)
            this.randomAccessFileWrite = new RandomAccessFile(filePath, "rw");
        return this.randomAccessFileWrite;
    }

    // as implied here: https://stackoverflow.com/questions/45396252/concurrency-of-randomaccessfile-in-java/45490504
    // something can go wrong if multiple threads try to read/write concurrently.
    public synchronized void writeBlock(long begin, byte[] block, int arrayIndexFrom, int howMuchToWriteFromArray) throws IOException {
        this.getRandomAccessFileWrite().seek(begin);
        this.getRandomAccessFileWrite().write(block, arrayIndexFrom, howMuchToWriteFromArray);
    }


    // as implied here: https://stackoverflow.com/questions/45396252/concurrency-of-randomaccessfile-in-java/45490504
    // something can go wrong if multiple threads try to read/write concurrently.
    public synchronized byte[] readBlock(long begin, int blockLength) throws IOException {
        this.getRandomAccessFileRead().seek(begin);
        byte[] result = new byte[blockLength];
        this.getRandomAccessFileRead().read(result);
        return result;
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
