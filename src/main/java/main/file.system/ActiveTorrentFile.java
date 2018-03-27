package main.file.system;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

public class ActiveTorrentFile implements TorrentFile {
    private String filePath;
    private long from, to; // not closed range: [from,to).
    private RandomAccessFile randomAccessFile;

    public ActiveTorrentFile(String filePath, long from, long to) {
        this.filePath = filePath;
        this.from = from;
        this.to = to;
    }

    public long getFrom() {
        return from;
    }

    public long getTo() {
        return to;
    }

    public synchronized RandomAccessFile getRandomAccessFile() throws FileNotFoundException {
        if (this.randomAccessFile == null)
            this.randomAccessFile = new RandomAccessFile(filePath, "rw");
        return this.randomAccessFile;
    }

    // as implied here: https://stackoverflow.com/questions/45396252/concurrency-of-randomaccessfile-in-java/45490504
    // something can go wrong if multiple threads try to read/write concurrently.
    public synchronized void writeBlock(long begin, byte[] block, int arrayIndexFrom, int howMuchToWriteFromArray) throws IOException {
        this.getRandomAccessFile().seek(begin);
        this.getRandomAccessFile().write(block, arrayIndexFrom, howMuchToWriteFromArray);
    }


    // as implied here: https://stackoverflow.com/questions/45396252/concurrency-of-randomaccessfile-in-java/45490504
    // something can go wrong if multiple threads try to read/write concurrently.
    public synchronized byte[] readBlock(long begin, int blockLength) throws IOException {
        this.getRandomAccessFile().seek(begin);
        byte[] result = new byte[blockLength];
        this.getRandomAccessFile().read(result);
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
}
