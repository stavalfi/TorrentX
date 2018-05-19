package main.listen.state.tree;

public class ListenState {
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

    public ListenState(boolean isStartedListeningInProgress,
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

    public static class ListenStateBuilder {
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

        public static ListenState.ListenStateBuilder builder(ListenState listenState) {
            return new ListenState.ListenStateBuilder(listenState);
        }

        public static ListenState.ListenStateBuilder builder() {
            return new ListenState.ListenStateBuilder();
        }

        public ListenState build() {
            return new ListenState(
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

        private ListenStateBuilder() {
        }

        public ListenStateBuilder(ListenState listenState) {
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
    }
}
