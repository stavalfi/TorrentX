package main.listener.reducers;

import main.listener.ListenerAction;
import main.listener.state.tree.ListenerState;
import redux.reducer.Reducer;
import redux.store.Request;
import redux.store.Result;

import java.util.function.Predicate;

public class ListenerReducer implements Reducer<ListenerState, ListenerAction> {
	public static ListenerState defaultListenState =
			ListenerState.ListenStateBuilder.builder(ListenerAction.INITIALIZE)
					.setStartedListeningInProgress(false)
					.setStartedListeningSelfResolved(false)
					.setStartedListeningWindUp(false)
					.setResumeListeningInProgress(false)
					.setResumeListeningSelfResolved(false)
					.setResumeListeningWindUp(false)
					.setPauseListeningInProgress(false)
					.setPauseListeningSelfResolved(false)
					.setPauseListeningWindUp(true)
					.setRestartListeningInProgress(false)
					.setRestartListeningSelfResolved(false)
					.setRestartListeningWindUp(false)
					.build();

	public Result<ListenerState, ListenerAction> reducer(ListenerState lastState, Request<ListenerAction> request) {
		boolean isRestartedOrRestarting = lastState.isRestartListeningInProgress() ||
				lastState.isRestartListeningWindUp();

		switch (request.getAction()) {
			case INITIALIZE:
				if (!lastState.isRestartListeningWindUp())
					return new Result<>(request, lastState, false);
				return new Result<>(request, ListenerState.ListenStateBuilder.builder(request, lastState)
						.setRestartListeningWindUp(false)
						.build(), true);

			case START_LISTENING_IN_PROGRESS:
				if (isRestartedOrRestarting ||
						lastState.isStartedListeningInProgress() ||
						lastState.isStartedListeningWindUp())
					return new Result<>(request, lastState, false);
				return new Result<>(request, ListenerState.ListenStateBuilder.builder(request, lastState)
						.setStartedListeningInProgress(true)
						.build(), true);

			case START_LISTENING_SELF_RESOLVED:
				if (isRestartedOrRestarting ||
						!lastState.isStartedListeningInProgress() ||
						lastState.isStartedListeningSelfResolved() ||
						lastState.isStartedListeningWindUp())
					return new Result<>(request, lastState, false);
				return new Result<>(request, ListenerState.ListenStateBuilder.builder(request, lastState)
						.setStartedListeningSelfResolved(true)
						.build(), true);

			case START_LISTENING_WIND_UP:
				if (isRestartedOrRestarting ||
						!lastState.isStartedListeningInProgress() ||
						!lastState.isStartedListeningSelfResolved() ||
						lastState.isStartedListeningWindUp())
					return new Result<>(request, lastState, false);
				return new Result<>(request, ListenerState.ListenStateBuilder.builder(request, lastState)
						.setStartedListeningInProgress(false)
						.setStartedListeningSelfResolved(false)
						.setStartedListeningWindUp(true)
						.build(), true);

			case RESUME_LISTENING_IN_PROGRESS:
				if (isRestartedOrRestarting ||
						lastState.isPauseListeningInProgress() ||
						!lastState.isStartedListeningWindUp() ||
						lastState.isResumeListeningInProgress() ||
						lastState.isResumeListeningWindUp() ||
						!lastState.isPauseListeningWindUp())
					return new Result<>(request, lastState, false);
				return new Result<>(request, ListenerState.ListenStateBuilder.builder(request, lastState)
						.setResumeListeningInProgress(true)
						.build(), true);

			case RESUME_LISTENING_SELF_RESOLVED:
				if (isRestartedOrRestarting ||
						lastState.isPauseListeningInProgress() ||
						!lastState.isResumeListeningInProgress() ||
						lastState.isResumeListeningSelfResolved() ||
						lastState.isResumeListeningWindUp() ||
						!lastState.isPauseListeningWindUp())
					return new Result<>(request, lastState, false);
				return new Result<>(request, ListenerState.ListenStateBuilder.builder(request, lastState)
						.setResumeListeningSelfResolved(true)
						.build(), true);

			case RESUME_LISTENING_WIND_UP:
				if (isRestartedOrRestarting ||
						lastState.isPauseListeningInProgress() ||
						!lastState.isResumeListeningInProgress() ||
						!lastState.isResumeListeningSelfResolved() ||
						lastState.isResumeListeningWindUp() ||
						!lastState.isPauseListeningWindUp())
					return new Result<>(request, lastState, false);
				return new Result<>(request, ListenerState.ListenStateBuilder.builder(request, lastState)
						.setResumeListeningInProgress(false)
						.setResumeListeningSelfResolved(false)
						.setResumeListeningWindUp(true)
						.setPauseListeningWindUp(false)
						.build(), true);

			case PAUSE_LISTENING_IN_PROGRESS:
				if (!lastState.isStartedListeningWindUp() ||
						lastState.isPauseListeningInProgress() ||
						lastState.isPauseListeningWindUp() ||
						!lastState.isResumeListeningWindUp())
					return new Result<>(request, lastState, false);
				return new Result<>(request, ListenerState.ListenStateBuilder.builder(request, lastState)
						.setPauseListeningInProgress(true)
						.setResumeListeningInProgress(false)
						.setResumeListeningSelfResolved(false)
						.build(), true);

			case PAUSE_LISTENING_SELF_RESOLVED:
				if (!lastState.isPauseListeningInProgress() ||
						lastState.isPauseListeningSelfResolved() ||
						lastState.isPauseListeningWindUp() ||
						!lastState.isResumeListeningWindUp())
					return new Result<>(request, lastState, false);
				return new Result<>(request, ListenerState.ListenStateBuilder.builder(request, lastState)
						.setPauseListeningSelfResolved(true)
						.setResumeListeningInProgress(false)
						.setResumeListeningSelfResolved(false)
						.build(), true);

			case PAUSE_LISTENING_WIND_UP:
				if (!lastState.isPauseListeningInProgress() ||
						!lastState.isPauseListeningSelfResolved() ||
						lastState.isPauseListeningWindUp() ||
						!lastState.isResumeListeningWindUp())
					return new Result<>(request, lastState, false);
				return new Result<>(request, ListenerState.ListenStateBuilder.builder(request, lastState)
						.setPauseListeningInProgress(false)
						.setPauseListeningSelfResolved(false)
						.setPauseListeningWindUp(true)
						.setResumeListeningSelfResolved(false)
						.setResumeListeningWindUp(false)
						.build(), true);

			case RESTART_LISTENING_IN_PROGRESS:
				Predicate<ListenerState> isEqualByProperties = listenerState ->
						defaultListenState.getAction().equals(listenerState.getAction()) &&
								defaultListenState.isStartedListeningInProgress() == listenerState.isStartedListeningInProgress() &&
								defaultListenState.isStartedListeningSelfResolved() == listenerState.isStartedListeningSelfResolved() &&
								defaultListenState.isStartedListeningWindUp() == listenerState.isStartedListeningWindUp() &&
								defaultListenState.isResumeListeningInProgress() == listenerState.isResumeListeningInProgress() &&
								defaultListenState.isResumeListeningSelfResolved() == listenerState.isResumeListeningSelfResolved() &&
								defaultListenState.isResumeListeningWindUp() == listenerState.isResumeListeningWindUp() &&
								defaultListenState.isPauseListeningInProgress() == listenerState.isPauseListeningInProgress() &&
								defaultListenState.isPauseListeningSelfResolved() == listenerState.isPauseListeningSelfResolved() &&
								defaultListenState.isPauseListeningWindUp() == listenerState.isPauseListeningWindUp() &&
								defaultListenState.isRestartListeningInProgress() == listenerState.isRestartListeningInProgress() &&
								defaultListenState.isRestartListeningSelfResolved() == listenerState.isRestartListeningSelfResolved() &&
								defaultListenState.isRestartListeningWindUp() == listenerState.isRestartListeningWindUp();

				if (isEqualByProperties.test(lastState) ||
						lastState.isRestartListeningInProgress() ||
						lastState.isRestartListeningWindUp())
					return new Result<>(request, lastState, false);
				return new Result<>(request, ListenerState.ListenStateBuilder.builder(request, lastState)
						.setRestartListeningInProgress(true)
						.build(), true);

			case RESTART_LISTENING_SELF_RESOLVED:
				if (!lastState.isRestartListeningInProgress() ||
						lastState.isRestartListeningSelfResolved() ||
						lastState.isRestartListeningWindUp())
					return new Result<>(request, lastState, false);
				return new Result<>(request, ListenerState.ListenStateBuilder.builder(request, lastState)
						.setRestartListeningSelfResolved(true)
						.build(), true);

			case RESTART_LISTENING_WIND_UP:
				if (!lastState.isPauseListeningWindUp() ||
						!lastState.isRestartListeningInProgress() ||
						!lastState.isRestartListeningSelfResolved() ||
						lastState.isRestartListeningWindUp())
					return new Result<>(request, lastState, false);
				return new Result<>(request, ListenerState.ListenStateBuilder.builder(request, lastState)
						.setStartedListeningInProgress(false)
						.setStartedListeningSelfResolved(false)
						.setStartedListeningWindUp(false)
						.setResumeListeningInProgress(false)
						.setResumeListeningSelfResolved(false)
						.setRestartListeningInProgress(false)
						.setRestartListeningSelfResolved(false)
						.setRestartListeningWindUp(true)
						.build(), true);

			default:
				return new Result<>(request, lastState, false);
		}
	}
}
