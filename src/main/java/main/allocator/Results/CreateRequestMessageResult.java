package main.allocator.Results;

import main.allocator.AllocatorAction;
import main.allocator.AllocatorState;
import main.allocator.requests.CreateRequestMessageRequest;
import main.peer.peerMessages.RequestMessage;
import main.redux.store.Result;

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
