package main.torrent.status.state.tree;

import java.util.Objects;

public class PeersState {
    private boolean isStartedListeningToIncomingPeersInProgress;
    private boolean isStartedListeningToIncomingPeersWindUp;
    private boolean isPauseListeningToIncomingPeersInProgress;
    private boolean isPauseListeningToIncomingPeersWindUp;
    private boolean isResumeListeningToIncomingPeersInProgress;
    private boolean isResumeListeningToIncomingPeersWindUp;

    private boolean isStartedSearchingPeersInProgress;
    private boolean isStartedSearchingPeersWindUp;
    private boolean isPauseSearchingPeersInProgress;
    private boolean isPauseSearchingPeersWindUp;
    private boolean isResumeSearchingPeersInProgress;
    private boolean isResumeSearchingPeersWindUp;

    public PeersState(boolean isStartedListeningToIncomingPeersInProgress,
                      boolean isStartedListeningToIncomingPeersWindUp,
                      boolean isPauseListeningToIncomingPeersInProgress,
                      boolean isPauseListeningToIncomingPeersWindUp,
                      boolean isResumeListeningToIncomingPeersInProgress,
                      boolean isResumeListeningToIncomingPeersWindUp,
                      boolean isStartedSearchingPeersInProgress,
                      boolean isStartedSearchingPeersWindUp,
                      boolean isPauseSearchingPeersInProgress,
                      boolean isPauseSearchingPeersWindUp,
                      boolean isResumeSearchingPeersInProgress,
                      boolean isResumeSearchingPeersWindUp) {
        this.isStartedListeningToIncomingPeersInProgress = isStartedListeningToIncomingPeersInProgress;
        this.isStartedListeningToIncomingPeersWindUp = isStartedListeningToIncomingPeersWindUp;
        this.isPauseListeningToIncomingPeersInProgress = isPauseListeningToIncomingPeersInProgress;
        this.isPauseListeningToIncomingPeersWindUp = isPauseListeningToIncomingPeersWindUp;
        this.isResumeListeningToIncomingPeersInProgress = isResumeListeningToIncomingPeersInProgress;
        this.isResumeListeningToIncomingPeersWindUp = isResumeListeningToIncomingPeersWindUp;
        this.isStartedSearchingPeersInProgress = isStartedSearchingPeersInProgress;
        this.isStartedSearchingPeersWindUp = isStartedSearchingPeersWindUp;
        this.isPauseSearchingPeersInProgress = isPauseSearchingPeersInProgress;
        this.isPauseSearchingPeersWindUp = isPauseSearchingPeersWindUp;
        this.isResumeSearchingPeersInProgress = isResumeSearchingPeersInProgress;
        this.isResumeSearchingPeersWindUp = isResumeSearchingPeersWindUp;
    }

    public boolean isStartedListeningToIncomingPeersInProgress() {
        return isStartedListeningToIncomingPeersInProgress;
    }

    public boolean isStartedListeningToIncomingPeersWindUp() {
        return isStartedListeningToIncomingPeersWindUp;
    }

    public boolean isPauseListeningToIncomingPeersInProgress() {
        return isPauseListeningToIncomingPeersInProgress;
    }

    public boolean isPauseListeningToIncomingPeersWindUp() {
        return isPauseListeningToIncomingPeersWindUp;
    }

    public boolean isResumeListeningToIncomingPeersInProgress() {
        return isResumeListeningToIncomingPeersInProgress;
    }

    public boolean isResumeListeningToIncomingPeersWindUp() {
        return isResumeListeningToIncomingPeersWindUp;
    }

    public boolean isStartedSearchingPeersInProgress() {
        return isStartedSearchingPeersInProgress;
    }

    public boolean isStartedSearchingPeersWindUp() {
        return isStartedSearchingPeersWindUp;
    }

    public boolean isPauseSearchingPeersInProgress() {
        return isPauseSearchingPeersInProgress;
    }

    public boolean isPauseSearchingPeersWindUp() {
        return isPauseSearchingPeersWindUp;
    }

    public boolean isResumeSearchingPeersInProgress() {
        return isResumeSearchingPeersInProgress;
    }

    public boolean isResumeSearchingPeersWindUp() {
        return isResumeSearchingPeersWindUp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PeersState)) return false;
        PeersState that = (PeersState) o;
        return isStartedListeningToIncomingPeersInProgress() == that.isStartedListeningToIncomingPeersInProgress() &&
                isStartedListeningToIncomingPeersWindUp() == that.isStartedListeningToIncomingPeersWindUp() &&
                isPauseListeningToIncomingPeersInProgress() == that.isPauseListeningToIncomingPeersInProgress() &&
                isPauseListeningToIncomingPeersWindUp() == that.isPauseListeningToIncomingPeersWindUp() &&
                isResumeListeningToIncomingPeersInProgress() == that.isResumeListeningToIncomingPeersInProgress() &&
                isResumeListeningToIncomingPeersWindUp() == that.isResumeListeningToIncomingPeersWindUp() &&
                isStartedSearchingPeersInProgress() == that.isStartedSearchingPeersInProgress() &&
                isStartedSearchingPeersWindUp() == that.isStartedSearchingPeersWindUp() &&
                isPauseSearchingPeersInProgress() == that.isPauseSearchingPeersInProgress() &&
                isPauseSearchingPeersWindUp() == that.isPauseSearchingPeersWindUp() &&
                isResumeSearchingPeersInProgress() == that.isResumeSearchingPeersInProgress() &&
                isResumeSearchingPeersWindUp() == that.isResumeSearchingPeersWindUp();
    }

    @Override
    public int hashCode() {

        return Objects.hash(isStartedListeningToIncomingPeersInProgress(), isStartedListeningToIncomingPeersWindUp(), isPauseListeningToIncomingPeersInProgress(), isPauseListeningToIncomingPeersWindUp(), isResumeListeningToIncomingPeersInProgress(), isResumeListeningToIncomingPeersWindUp(), isStartedSearchingPeersInProgress(), isStartedSearchingPeersWindUp(), isPauseSearchingPeersInProgress(), isPauseSearchingPeersWindUp(), isResumeSearchingPeersInProgress(), isResumeSearchingPeersWindUp());
    }

    @Override
    public String toString() {
        return "PeersState{" +
                "isStartedListeningToIncomingPeersInProgress=" + isStartedListeningToIncomingPeersInProgress +
                ", isStartedListeningToIncomingPeersWindUp=" + isStartedListeningToIncomingPeersWindUp +
                ", isPauseListeningToIncomingPeersInProgress=" + isPauseListeningToIncomingPeersInProgress +
                ", isPauseListeningToIncomingPeersWindUp=" + isPauseListeningToIncomingPeersWindUp +
                ", isResumeListeningToIncomingPeersInProgress=" + isResumeListeningToIncomingPeersInProgress +
                ", isResumeListeningToIncomingPeersWindUp=" + isResumeListeningToIncomingPeersWindUp +
                ", isStartedSearchingPeersInProgress=" + isStartedSearchingPeersInProgress +
                ", isStartedSearchingPeersWindUp=" + isStartedSearchingPeersWindUp +
                ", isPauseSearchingPeersInProgress=" + isPauseSearchingPeersInProgress +
                ", isPauseSearchingPeersWindUp=" + isPauseSearchingPeersWindUp +
                ", isResumeSearchingPeersInProgress=" + isResumeSearchingPeersInProgress +
                ", isResumeSearchingPeersWindUp=" + isResumeSearchingPeersWindUp +
                '}';
    }

    public static class PeersStateBuilder {
        private boolean isStartedListeningToIncomingPeersInProgress;
        private boolean isStartedListeningToIncomingPeersWindUp;
        private boolean isPauseListeningToIncomingPeersInProgress;
        private boolean isPauseListeningToIncomingPeersWindUp;
        private boolean isResumeListeningToIncomingPeersInProgress;
        private boolean isResumeListeningToIncomingPeersWindUp;

        private boolean isStartedSearchingPeersInProgress;
        private boolean isStartedSearchingPeersWindUp;
        private boolean isPauseSearchingPeersInProgress;
        private boolean isPauseSearchingPeersWindUp;
        private boolean isResumeSearchingPeersInProgress;
        private boolean isResumeSearchingPeersWindUp;

        public static PeersStateBuilder builder(PeersState peersState) {
            return new PeersStateBuilder(peersState);
        }

        public static PeersStateBuilder builder() {
            return new PeersStateBuilder();
        }

        public PeersState build() {
            return new PeersState(this.isStartedListeningToIncomingPeersInProgress,
                    this.isStartedListeningToIncomingPeersWindUp,
                    this.isPauseListeningToIncomingPeersInProgress,
                    this.isPauseListeningToIncomingPeersWindUp,
                    this.isResumeListeningToIncomingPeersInProgress,
                    this.isResumeListeningToIncomingPeersWindUp,
                    this.isStartedSearchingPeersInProgress,
                    this.isStartedSearchingPeersWindUp,
                    this.isPauseSearchingPeersInProgress,
                    this.isPauseSearchingPeersWindUp,
                    this.isResumeSearchingPeersInProgress,
                    this.isResumeSearchingPeersWindUp);
        }

        private PeersStateBuilder() {
        }

        public PeersStateBuilder(PeersState peersState) {
            this.isStartedListeningToIncomingPeersInProgress = peersState.isStartedListeningToIncomingPeersInProgress;
            this.isStartedListeningToIncomingPeersWindUp = peersState.isStartedListeningToIncomingPeersWindUp;
            this.isPauseListeningToIncomingPeersInProgress = peersState.isPauseListeningToIncomingPeersInProgress;
            this.isPauseListeningToIncomingPeersWindUp = peersState.isPauseListeningToIncomingPeersWindUp;
            this.isResumeListeningToIncomingPeersInProgress = peersState.isResumeListeningToIncomingPeersInProgress;
            this.isResumeListeningToIncomingPeersWindUp = peersState.isResumeListeningToIncomingPeersWindUp;
            this.isStartedSearchingPeersInProgress = peersState.isStartedSearchingPeersInProgress;
            this.isStartedSearchingPeersWindUp = peersState.isStartedSearchingPeersWindUp;
            this.isPauseSearchingPeersInProgress = peersState.isPauseSearchingPeersInProgress;
            this.isPauseSearchingPeersWindUp = peersState.isPauseSearchingPeersWindUp;
            this.isResumeSearchingPeersInProgress = peersState.isResumeSearchingPeersInProgress;
            this.isResumeSearchingPeersWindUp = peersState.isResumeSearchingPeersWindUp;
        }

        public PeersStateBuilder setStartedListeningToIncomingPeersInProgress(boolean startedListeningToIncomingPeersInProgress) {
            isStartedListeningToIncomingPeersInProgress = startedListeningToIncomingPeersInProgress;
            return this;
        }

        public PeersStateBuilder setStartedListeningToIncomingPeersWindUp(boolean startedListeningToIncomingPeersWindUp) {
            isStartedListeningToIncomingPeersWindUp = startedListeningToIncomingPeersWindUp;
            return this;
        }

        public PeersStateBuilder setPauseListeningToIncomingPeersInProgress(boolean pauseListeningToIncomingPeersInProgress) {
            isPauseListeningToIncomingPeersInProgress = pauseListeningToIncomingPeersInProgress;
            return this;
        }

        public PeersStateBuilder setPauseListeningToIncomingPeersWindUp(boolean pauseListeningToIncomingPeersWindUp) {
            isPauseListeningToIncomingPeersWindUp = pauseListeningToIncomingPeersWindUp;
            return this;
        }

        public PeersStateBuilder setResumeListeningToIncomingPeersInProgress(boolean resumeListeningToIncomingPeersInProgress) {
            isResumeListeningToIncomingPeersInProgress = resumeListeningToIncomingPeersInProgress;
            return this;
        }

        public PeersStateBuilder setResumeListeningToIncomingPeersWindUp(boolean resumeListeningToIncomingPeersWindUp) {
            isResumeListeningToIncomingPeersWindUp = resumeListeningToIncomingPeersWindUp;
            return this;
        }

        public PeersStateBuilder setStartedSearchingPeersInProgress(boolean startedSearchingPeersInProgress) {
            isStartedSearchingPeersInProgress = startedSearchingPeersInProgress;
            return this;
        }

        public PeersStateBuilder setStartedSearchingPeersWindUp(boolean startedSearchingPeersWindUp) {
            isStartedSearchingPeersWindUp = startedSearchingPeersWindUp;
            return this;
        }

        public PeersStateBuilder setPauseSearchingPeersInProgress(boolean pauseSearchingPeersInProgress) {
            isPauseSearchingPeersInProgress = pauseSearchingPeersInProgress;
            return this;
        }

        public PeersStateBuilder setPauseSearchingPeersWindUp(boolean pauseSearchingPeersWindUp) {
            isPauseSearchingPeersWindUp = pauseSearchingPeersWindUp;
            return this;
        }

        public PeersStateBuilder setResumeSearchingPeersInProgress(boolean resumeSearchingPeersInProgress) {
            isResumeSearchingPeersInProgress = resumeSearchingPeersInProgress;
            return this;
        }

        public PeersStateBuilder setResumeSearchingPeersWindUp(boolean resumeSearchingPeersWindUp) {
            isResumeSearchingPeersWindUp = resumeSearchingPeersWindUp;
            return this;
        }
    }
}
