package main.file.system.allocator.Results;

import main.file.system.allocator.AllocatorAction;
import main.file.system.allocator.AllocatorState;
import main.file.system.allocator.requests.CreatePieceMessageRequest;
import main.peer.peerMessages.PieceMessage;
import redux.store.Result;

public class CreatePieceMessageResult extends Result<AllocatorState, AllocatorAction> {
	private PieceMessage pieceMessage;

	public CreatePieceMessageResult(CreatePieceMessageRequest request, AllocatorState state,
									boolean isNewState, PieceMessage pieceMessage) {
		super(request, state, isNewState);
		this.pieceMessage = pieceMessage;
	}

	public PieceMessage getPieceMessage() {
		return pieceMessage;
	}

	@Override
	public String toString() {
		return "CreatePieceMessageResult{" +
				"pieceMessage=" + pieceMessage +
				'}';
	}
}
