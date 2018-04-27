package main.file.system;

import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;

public class ActiveTorrentFile implements TorrentFile {
	private String filePath;
	// 0 <= from <=to <= Hole-Torrent.size
	private long from, to; // inclusive.
	private SeekableByteChannel seekableByteChannel;

	public ActiveTorrentFile(String filePath, long from, long to, SeekableByteChannel seekableByteChannel) {
		this.filePath = filePath;
		this.from = from;
		this.to = to;
		this.seekableByteChannel = seekableByteChannel;
	}

	public synchronized Mono<ActiveTorrentFile> closeFileChannel() {
		try {
			if (this.seekableByteChannel.isOpen())
				this.seekableByteChannel.close();
		} catch (IOException e) {
			return Mono.error(e);
		}
		return Mono.just(this);
	}

	// as implied here: https://stackoverflow.com/questions/45396252/concurrency-of-randomaccessfile-in-java/45490504
	// something can go wrong if multiple threads try to read/write concurrently.
	public synchronized void writeBlock(long begin, byte[] block, int arrayIndexFrom, int howMuchToWriteFromArray) throws IOException {
		assert this.from <= begin && begin < this.to;
		assert howMuchToWriteFromArray <= this.to - this.from;
		assert arrayIndexFrom < block.length;
		assert howMuchToWriteFromArray <= block.length;

		long writeFrom = begin - this.from;
		this.seekableByteChannel.position(writeFrom);
		ByteBuffer blockFromArrayIndex = ByteBuffer.wrap(block);
		blockFromArrayIndex.position(arrayIndexFrom);
		blockFromArrayIndex.limit(howMuchToWriteFromArray);
		this.seekableByteChannel.write(blockFromArrayIndex);
	}


	// as implied here: https://stackoverflow.com/questions/45396252/concurrency-of-randomaccessfile-in-java/45490504
	// something can go wrong if multiple threads try to read/write concurrently.
	public synchronized byte[] readBlock(long begin, int blockLength) throws IOException {
		assert this.from <= begin && begin < this.to;
		assert blockLength <= this.to - this.from;

		long readFrom = begin - this.from;
		this.seekableByteChannel.position(readFrom);
		ByteBuffer block = ByteBuffer.allocate(blockLength);
		this.seekableByteChannel.read(block);
		return block.array();
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
