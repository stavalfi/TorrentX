package main.file.system;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

public class ActiveTorrentFile implements TorrentFile {
    private String filePath;
    private long from, to; // not closed range: [from,to).
    private RandomAccessFile randomAccessFile;

    public ActiveTorrentFile(String filePath, long from, long to) throws FileNotFoundException {
        this.filePath = filePath;
        this.from = from;
        this.to = to;
        this.randomAccessFile = new RandomAccessFile(filePath, "rw");
    }

    public long getFrom() {
        return from;
    }

    public long getTo() {
        return to;
    }

    // as implied here: https://stackoverflow.com/questions/45396252/concurrency-of-randomaccessfile-in-java/45490504
    // something can go wrong if multiple threads try to read/write concurrently.
    public synchronized void writeBlock(long begin, byte[] block, int arrayIndexFrom, int howMuchToWriteFromArray) throws IOException {
        this.randomAccessFile.seek(begin);
        this.randomAccessFile.write(block, arrayIndexFrom, howMuchToWriteFromArray);
    }


    // as implied here: https://stackoverflow.com/questions/45396252/concurrency-of-randomaccessfile-in-java/45490504
    // something can go wrong if multiple threads try to read/write concurrently.
    public synchronized byte[] readBlock(long begin, int blockLength) throws IOException {
        this.randomAccessFile.seek(begin);
        byte[] result = new byte[blockLength];
        this.randomAccessFile.read(result);
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
