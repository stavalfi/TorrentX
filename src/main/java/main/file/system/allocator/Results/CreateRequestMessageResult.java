package main.file.system.allocator.Results;

import main.file.system.allocator.AllocatorAction;
import main.file.system.allocator.AllocatorState;
import main.file.system.allocator.requests.CreateRequestMessageRequest;
import main.peer.peerMessages.RequestMessage;
import redux.store.Result;

public class CreateRequestMessageResult extends Result<AllocatorState, AllocatorAction> {
	private RequestMessage requestMessage;

	public CreateRequestMessageResult(CreateRequestMessageRequest request, AllocatorState state,
									  boolean isNewState, RequestMessage requestMessage) {
		super(request, state, isNewState);
		this.requestMessage = requestMessage;
	}

	public RequestMessage getRequestMessage() {
		return requestMessage;
	}

	@Override
	public String toString() {
		return "CreateRequestMessageResult{" +
				"requestMessage=" + requestMessage +
				'}';
	}
}
