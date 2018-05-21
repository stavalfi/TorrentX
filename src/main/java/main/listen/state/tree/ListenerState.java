package main.listen.state.tree;

import main.listen.ListenerAction;

import java.util.Objects;

public class ListenerState {
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

    public ListenerState(ListenerAction action,
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
        this.action = action;
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

    public ListenerAction getAction() {
        return action;
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
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ListenerState)) return false;
        ListenerState that = (ListenerState) o;
        return isStartedListeningInProgress() == that.isStartedListeningInProgress() &&
                isStartedListeningSelfResolved() == that.isStartedListeningSelfResolved() &&
                isStartedListeningWindUp() == that.isStartedListeningWindUp() &&
                isPauseListeningInProgress() == that.isPauseListeningInProgress() &&
                isPauseListeningSelfResolved() == that.isPauseListeningSelfResolved() &&
                isPauseListeningWindUp() == that.isPauseListeningWindUp() &&
                isResumeListeningInProgress() == that.isResumeListeningInProgress() &&
                isResumeListeningSelfResolved() == that.isResumeListeningSelfResolved() &&
                isResumeListeningWindUp() == that.isResumeListeningWindUp() &&
                isRestartListeningInProgress() == that.isRestartListeningInProgress() &&
                isRestartListeningSelfResolved() == that.isRestartListeningSelfResolved() &&
                isRestartListeningWindUp() == that.isRestartListeningWindUp() &&
                getAction() == that.getAction();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getAction(), isStartedListeningInProgress(), isStartedListeningSelfResolved(), isStartedListeningWindUp(), isPauseListeningInProgress(), isPauseListeningSelfResolved(), isPauseListeningWindUp(), isResumeListeningInProgress(), isResumeListeningSelfResolved(), isResumeListeningWindUp(), isRestartListeningInProgress(), isRestartListeningSelfResolved(), isRestartListeningWindUp());
    }

    @Override
    public String toString() {
        return "ListenerStore{" +
                "action=" + action +
                ", isStartedListeningInProgress=" + isStartedListeningInProgress +
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
                '}';
    }

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

        private ListenStateBuilder() {

        }

        private ListenStateBuilder(ListenerAction action) {
            this.action = action;
        }

        public static ListenerState.ListenStateBuilder builder(ListenerAction action, ListenerState listenerState) {
            return new ListenerState.ListenStateBuilder(action, listenerState);
        }

        public static ListenerState.ListenStateBuilder builder(ListenerAction action) {
            return new ListenerState.ListenStateBuilder(action);
        }

        public ListenerState build() {
            return new ListenerState(
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

        private ListenStateBuilder(ListenerAction action, ListenerState listenerState) {
            this.action = action;
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
