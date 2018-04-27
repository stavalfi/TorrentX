package main.peer.peerMessages;

import main.peer.Peer;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Objects;

public class PieceMessage extends PeerMessage {
	private static final byte messageId = 7;
	private int index;
	private int begin;
	private byte[] block;

	/**
	 * The payload contains the following information: (by this order)
	 *
	 * @param index integer specifying the zero-based piece index
	 * @param begin integer specifying the zero-based byte offset within the piece
	 * @param block block of data, which is a subset of the piece specified by index.
	 */
	public PieceMessage(Peer from, Peer to, int index, int begin, byte[] block) {
		super(to, from, 9 + block.length, messageId, ByteBuffer.allocate(4 + 4 + block.length)
				.putInt(index)
				.putInt(begin)
				.put(block).array());
		this.index = index;
		this.begin = begin;
		this.block = block;
	}

	public PieceMessage(Peer from, Peer to, byte[] peerMessage) {
		super(to, peerMessage, from);
		ByteBuffer byteBuffer = ByteBuffer.wrap(super.getPayload());
		this.index = byteBuffer.getInt();
		this.begin = byteBuffer.getInt();
		// 8 == `index` in bytes + `begin` in bytes.
		this.block = new byte[super.getPayload().length - 8];
		byteBuffer.get(this.block);
	}

	public int getIndex() {
		return index;
	}

	public int getBegin() {
		return begin;
	}

	public byte[] getBlock() {
		return block;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		PieceMessage that = (PieceMessage) o;
		return index == that.index &&
				begin == that.begin &&
				Arrays.equals(block, that.block);
	}

	@Override
	public int hashCode() {

		int result = Objects.hash(index, begin);
		result = 31 * result + Arrays.hashCode(block);
		return result;
	}

	@Override
	public String toString() {
		return "PieceMessage{" +
				"index=" + index +
				", begin=" + begin +
				", blockLength=" + block.length +
				"} " + super.toString();
	}
}