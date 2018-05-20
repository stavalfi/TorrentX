package main.listen.state.tree;

import main.listen.ListenAction;

import java.util.Objects;

public class ListenState {
    private ListenAction action;
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

    public ListenState(ListenAction action,
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

    public ListenAction getAction() {
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
        if (!(o instanceof ListenState)) return false;
        ListenState that = (ListenState) o;
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
        return "ListenStore{" +
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

    public boolean fromAction(ListenAction action) {
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
        private ListenAction action;
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

        private ListenStateBuilder(ListenAction action) {
            this.action = action;
        }

        public static ListenState.ListenStateBuilder builder(ListenAction action, ListenState listenState) {
            return new ListenState.ListenStateBuilder(action, listenState);
        }

        public static ListenState.ListenStateBuilder builder(ListenAction action) {
            return new ListenState.ListenStateBuilder(action);
        }

        public ListenState build() {
            return new ListenState(
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

        private ListenStateBuilder(ListenAction action, ListenState listenState) {
            this.isStartedListeningInProgress = listenState.isStartedListeningInProgress;
            this.isStartedListeningSelfResolved = listenState.isStartedListeningSelfResolved;
            this.isStartedListeningWindUp = listenState.isStartedListeningWindUp;
            this.isPauseListeningInProgress = listenState.isPauseListeningInProgress;
            this.isPauseListeningSelfResolved = listenState.isPauseListeningSelfResolved;
            this.isPauseListeningWindUp = listenState.isPauseListeningWindUp;
            this.isResumeListeningInProgress = listenState.isResumeListeningInProgress;
            this.isResumeListeningSelfResolved = listenState.isResumeListeningSelfResolved;
            this.isResumeListeningWindUp = listenState.isResumeListeningWindUp;
            this.isRestartListeningInProgress = listenState.isRestartListeningInProgress;
            this.isRestartListeningSelfResolved = listenState.isRestartListeningSelfResolved;
            this.isRestartListeningWindUp = listenState.isRestartListeningWindUp;
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
