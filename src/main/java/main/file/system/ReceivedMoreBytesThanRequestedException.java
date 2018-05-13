package main.file.system;

import main.peer.peerMessages.PieceMessage;

public class ReceivedMoreBytesThanRequestedException extends Exception {
	private PieceMessage pieceMessage;

	public ReceivedMoreBytesThanRequestedException(PieceMessage pieceMessage) {
		this.pieceMessage = pieceMessage;
	}

	public PieceMessage getPieceMessage() {
		return pieceMessage;
	}
}
