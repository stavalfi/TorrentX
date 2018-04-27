package main.peer.peerMessages;

import main.peer.Peer;

import java.nio.ByteBuffer;

// The request message is fixed length, and is used to request a block.
public class RequestMessage extends PeerMessage {
	private static int length = 13;
	private static final byte messageId = 6;
	private int index;
	private int begin;
	private int blockLength;

	/**
	 * The payload contains the following information: (in this order)
	 *
	 * @param index       integer (4 bytes) specifying the zero-based piece index.
	 * @param begin       integer (4 bytes) specifying the zero-based byte offset within the piece.
	 * @param blockLength integer (4 bytes) specifying the requested length.
	 */
	public RequestMessage(Peer from, Peer to, int index, int begin, int blockLength) {
		super(to, from, RequestMessage.length, messageId, ByteBuffer.allocate(12)
				.putInt(index)
				.putInt(begin)
				.putInt(blockLength).array());
		this.index = index;
		this.begin = begin;
		this.blockLength = blockLength;
	}

	public RequestMessage(Peer from, Peer to, byte[] peerMessage) {
		super(to, peerMessage, from);
		ByteBuffer byteBuffer = ByteBuffer.wrap(super.getPayload());
		this.index = byteBuffer.getInt();
		this.begin = byteBuffer.getInt();
		this.blockLength = byteBuffer.getInt();
	}

	public int getIndex() {
		return index;
	}

	public int getBegin() {
		return begin;
	}

	public int getBlockLength() {
		return blockLength;
	}

	@Override
	public String toString() {
		return "RequestMessage{" +
				"index=" + index +
				", begin=" + begin +
				", blockLength=" + blockLength +
				"} " + super.toString();
	}
}