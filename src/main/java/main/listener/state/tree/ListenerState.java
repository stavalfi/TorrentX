package main.listener.state.tree;

import main.listener.ListenerAction;
import redux.state.State;
import redux.store.Request;

public class ListenerState extends State<ListenerAction> {
	private boolean isStartedListeningInProgress;
	private boolean isStartedListeningSelfResolved;
	private boolean isStartedListeningWindUp;
	private boolean isPauseListeningInProgress;
	private boolean isPauseListeningSelfResolved;
	private boolean isPauseListeningWindUp;
	private boolean isResumeListeningInProgress;
	private boolean isResumeListeningSelfResolved;
	private boolean isResumeListeningWindUp;
	private boolean isRestartListeningInProgress;
	private boolean isRestartListeningSelfResolved;
	private boolean isRestartListeningWindUp;

	public ListenerState(String id, ListenerAction action,
						 boolean isStartedListeningInProgress,
						 boolean isStartedListeningSelfResolved,
						 boolean isStartedListeningWindUp,
						 boolean isPauseListeningInProgress,
						 boolean isPauseListeningSelfResolved,
						 boolean isPauseListeningWindUp,
						 boolean isResumeListeningInProgress,
						 boolean isResumeListeningSelfResolved,
						 boolean isResumeListeningWindUp,
						 boolean isRestartListeningInProgress,
						 boolean isRestartListeningSelfResolved,
						 boolean isRestartListeningWindUp) {
		super(id, action);
		this.isStartedListeningInProgress = isStartedListeningInProgress;
		this.isStartedListeningSelfResolved = isStartedListeningSelfResolved;
		this.isStartedListeningWindUp = isStartedListeningWindUp;
		this.isPauseListeningInProgress = isPauseListeningInProgress;
		this.isPauseListeningSelfResolved = isPauseListeningSelfResolved;
		this.isPauseListeningWindUp = isPauseListeningWindUp;
		this.isResumeListeningInProgress = isResumeListeningInProgress;
		this.isResumeListeningSelfResolved = isResumeListeningSelfResolved;
		this.isResumeListeningWindUp = isResumeListeningWindUp;
		this.isRestartListeningInProgress = isRestartListeningInProgress;
		this.isRestartListeningSelfResolved = isRestartListeningSelfResolved;
		this.isRestartListeningWindUp = isRestartListeningWindUp;
	}

	public boolean isStartedListeningInProgress() {
		return isStartedListeningInProgress;
	}

	public boolean isStartedListeningSelfResolved() {
		return isStartedListeningSelfResolved;
	}

	public boolean isStartedListeningWindUp() {
		return isStartedListeningWindUp;
	}

	public boolean isPauseListeningInProgress() {
		return isPauseListeningInProgress;
	}

	public boolean isPauseListeningSelfResolved() {
		return isPauseListeningSelfResolved;
	}

	public boolean isPauseListeningWindUp() {
		return isPauseListeningWindUp;
	}

	public boolean isResumeListeningInProgress() {
		return isResumeListeningInProgress;
	}

	public boolean isResumeListeningSelfResolved() {
		return isResumeListeningSelfResolved;
	}

	public boolean isResumeListeningWindUp() {
		return isResumeListeningWindUp;
	}

	public boolean isRestartListeningInProgress() {
		return isRestartListeningInProgress;
	}

	public boolean isRestartListeningSelfResolved() {
		return isRestartListeningSelfResolved;
	}

	public boolean isRestartListeningWindUp() {
		return isRestartListeningWindUp;
	}

	@Override
	public String toString() {
		return "ListenerState{" + super.toString() +
				"isStartedListeningInProgress=" + isStartedListeningInProgress +
				", isStartedListeningSelfResolved=" + isStartedListeningSelfResolved +
				", isStartedListeningWindUp=" + isStartedListeningWindUp +
				", isPauseListeningInProgress=" + isPauseListeningInProgress +
				", isPauseListeningSelfResolved=" + isPauseListeningSelfResolved +
				", isPauseListeningWindUp=" + isPauseListeningWindUp +
				", isResumeListeningInProgress=" + isResumeListeningInProgress +
				", isResumeListeningSelfResolved=" + isResumeListeningSelfResolved +
				", isResumeListeningWindUp=" + isResumeListeningWindUp +
				", isRestartListeningInProgress=" + isRestartListeningInProgress +
				", isRestartListeningSelfResolved=" + isRestartListeningSelfResolved +
				", isRestartListeningWindUp=" + isRestartListeningWindUp +
				"} ";
	}

	@Override
	public boolean fromAction(ListenerAction action) {
		switch (action) {
			case START_LISTENING_IN_PROGRESS:
				return isStartedListeningInProgress;
			case START_LISTENING_SELF_RESOLVED:
				return isStartedListeningSelfResolved;
			case START_LISTENING_WIND_UP:
				return isStartedListeningWindUp;
			case RESUME_LISTENING_IN_PROGRESS:
				return isResumeListeningInProgress;
			case RESUME_LISTENING_SELF_RESOLVED:
				return isResumeListeningSelfResolved;
			case RESUME_LISTENING_WIND_UP:
				return isResumeListeningWindUp;
			case PAUSE_LISTENING_IN_PROGRESS:
				return isPauseListeningInProgress;
			case PAUSE_LISTENING_SELF_RESOLVED:
				return isPauseListeningSelfResolved;
			case PAUSE_LISTENING_WIND_UP:
				return isPauseListeningWindUp;
			case RESTART_LISTENING_IN_PROGRESS:
				return isRestartListeningInProgress;
			case RESTART_LISTENING_SELF_RESOLVED:
				return isRestartListeningSelfResolved;
			case RESTART_LISTENING_WIND_UP:
				return isRestartListeningWindUp;
			default:
				return false;
		}
	}

	public static class ListenStateBuilder {
		private String id;
		private ListenerAction action;
		private boolean isStartedListeningInProgress;
		private boolean isStartedListeningSelfResolved;
		private boolean isStartedListeningWindUp;
		private boolean isPauseListeningInProgress;
		private boolean isPauseListeningSelfResolved;
		private boolean isPauseListeningWindUp;
		private boolean isResumeListeningInProgress;
		private boolean isResumeListeningSelfResolved;
		private boolean isResumeListeningWindUp;
		private boolean isRestartListeningInProgress;
		private boolean isRestartListeningSelfResolved;
		private boolean isRestartListeningWindUp;

		public static ListenerState.ListenStateBuilder builder(Request<ListenerAction> request, ListenerState listenerState) {
			return new ListenerState.ListenStateBuilder(request, listenerState);
		}

		public static ListenerState.ListenStateBuilder builder(ListenerAction action) {
			return new ListenerState.ListenStateBuilder(action);
		}

		public ListenerState build() {
			return new ListenerState(this.id,
					this.action,
					this.isStartedListeningInProgress,
					this.isStartedListeningSelfResolved,
					this.isStartedListeningWindUp,
					this.isPauseListeningInProgress,
					this.isPauseListeningSelfResolved,
					this.isPauseListeningWindUp,
					this.isResumeListeningInProgress,
					this.isResumeListeningSelfResolved,
					this.isResumeListeningWindUp,
					this.isRestartListeningInProgress,
					this.isRestartListeningSelfResolved,
					this.isRestartListeningWindUp);
		}

		private ListenStateBuilder(ListenerAction action) {
			this.action = action;
		}

		private ListenStateBuilder(Request<ListenerAction> request) {
			this.id = request.getId();
			this.action = request.getAction();
		}

		private ListenStateBuilder(Request<ListenerAction> request, ListenerState listenerState) {
			this.id = request.getId();
			this.action = request.getAction();
			this.isStartedListeningInProgress = listenerState.isStartedListeningInProgress;
			this.isStartedListeningSelfResolved = listenerState.isStartedListeningSelfResolved;
			this.isStartedListeningWindUp = listenerState.isStartedListeningWindUp;
			this.isPauseListeningInProgress = listenerState.isPauseListeningInProgress;
			this.isPauseListeningSelfResolved = listenerState.isPauseListeningSelfResolved;
			this.isPauseListeningWindUp = listenerState.isPauseListeningWindUp;
			this.isResumeListeningInProgress = listenerState.isResumeListeningInProgress;
			this.isResumeListeningSelfResolved = listenerState.isResumeListeningSelfResolved;
			this.isResumeListeningWindUp = listenerState.isResumeListeningWindUp;
			this.isRestartListeningInProgress = listenerState.isRestartListeningInProgress;
			this.isRestartListeningSelfResolved = listenerState.isRestartListeningSelfResolved;
			this.isRestartListeningWindUp = listenerState.isRestartListeningWindUp;
		}

		public ListenStateBuilder setStartedListeningInProgress(boolean startedListeningInProgress) {
			isStartedListeningInProgress = startedListeningInProgress;
			return this;
		}

		public ListenStateBuilder setStartedListeningSelfResolved(boolean startedListeningSelfResolved) {
			isStartedListeningSelfResolved = startedListeningSelfResolved;
			return this;
		}

		public ListenStateBuilder setStartedListeningWindUp(boolean startedListeningWindUp) {
			isStartedListeningWindUp = startedListeningWindUp;
			return this;
		}

		public ListenStateBuilder setPauseListeningInProgress(boolean pauseListeningInProgress) {
			isPauseListeningInProgress = pauseListeningInProgress;
			return this;
		}

		public ListenStateBuilder setPauseListeningSelfResolved(boolean pauseListeningSelfResolved) {
			isPauseListeningSelfResolved = pauseListeningSelfResolved;
			return this;
		}

		public ListenStateBuilder setPauseListeningWindUp(boolean pauseListeningWindUp) {
			isPauseListeningWindUp = pauseListeningWindUp;
			return this;
		}

		public ListenStateBuilder setResumeListeningInProgress(boolean resumeListeningInProgress) {
			isResumeListeningInProgress = resumeListeningInProgress;
			return this;
		}

		public ListenStateBuilder setResumeListeningSelfResolved(boolean resumeListeningSelfResolved) {
			isResumeListeningSelfResolved = resumeListeningSelfResolved;
			return this;
		}

		public ListenStateBuilder setResumeListeningWindUp(boolean resumeListeningWindUp) {
			isResumeListeningWindUp = resumeListeningWindUp;
			return this;
		}

		public ListenStateBuilder setRestartListeningInProgress(boolean restartListeningInProgress) {
			isRestartListeningInProgress = restartListeningInProgress;
			return this;
		}

		public ListenStateBuilder setRestartListeningSelfResolved(boolean restartListeningSelfResolved) {
			isRestartListeningSelfResolved = restartListeningSelfResolved;
			return this;
		}

		public ListenStateBuilder setRestartListeningWindUp(boolean restartListeningWindUp) {
			isRestartListeningWindUp = restartListeningWindUp;
			return this;
		}
	}
}
