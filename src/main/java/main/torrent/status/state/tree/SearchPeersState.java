package main.torrent.status.state.tree;

import main.torrent.status.TorrentStatusAction;

import java.util.Objects;

public class SearchPeersState {

    private boolean isStartedSearchingPeersInProgress;
    private boolean isStartedSearchingPeersSelfResolved;
    private boolean isStartedSearchingPeersWindUp;
    private boolean isPauseSearchingPeersInProgress;
    private boolean isPauseSearchingPeersSelfResolved;
    private boolean isPauseSearchingPeersWindUp;
    private boolean isResumeSearchingPeersInProgress;
    private boolean isResumeSearchingPeersSelfResolved;
    private boolean isResumeSearchingPeersWindUp;

    public SearchPeersState(boolean isStartedSearchingPeersInProgress,
                            boolean isStartedSearchingPeersSelfResolved,
                            boolean isStartedSearchingPeersWindUp,
                            boolean isPauseSearchingPeersInProgress,
                            boolean isPauseSearchingPeersSelfResolved,
                            boolean isPauseSearchingPeersWindUp,
                            boolean isResumeSearchingPeersInProgress,
                            boolean isResumeSearchingPeersSelfResolved,
                            boolean isResumeSearchingPeersWindUp) {
        this.isStartedSearchingPeersInProgress = isStartedSearchingPeersInProgress;
        this.isStartedSearchingPeersSelfResolved = isStartedSearchingPeersSelfResolved;
        this.isStartedSearchingPeersWindUp = isStartedSearchingPeersWindUp;
        this.isPauseSearchingPeersInProgress = isPauseSearchingPeersInProgress;
        this.isPauseSearchingPeersSelfResolved = isPauseSearchingPeersSelfResolved;
        this.isPauseSearchingPeersWindUp = isPauseSearchingPeersWindUp;
        this.isResumeSearchingPeersInProgress = isResumeSearchingPeersInProgress;
        this.isResumeSearchingPeersSelfResolved = isResumeSearchingPeersSelfResolved;
        this.isResumeSearchingPeersWindUp = isResumeSearchingPeersWindUp;
    }

    public boolean fromAction(TorrentStatusAction torrentStatusAction) {
        switch (torrentStatusAction) {
            case START_SEARCHING_PEERS_IN_PROGRESS:
                return this.isStartedSearchingPeersInProgress;
            case START_SEARCHING_PEERS_SELF_RESOLVED:
                return this.isStartedSearchingPeersSelfResolved;
            case START_SEARCHING_PEERS_WIND_UP:
                return this.isStartedSearchingPeersWindUp;
            case PAUSE_SEARCHING_PEERS_IN_PROGRESS:
                return this.isPauseSearchingPeersInProgress;
            case PAUSE_SEARCHING_PEERS_SELF_RESOLVED:
                return this.isPauseSearchingPeersSelfResolved;
            case PAUSE_SEARCHING_PEERS_WIND_UP:
                return this.isPauseSearchingPeersWindUp;
            case RESUME_SEARCHING_PEERS_IN_PROGRESS:
                return this.isResumeSearchingPeersInProgress;
            case RESUME_SEARCHING_PEERS_SELF_RESOLVED:
                return this.isResumeSearchingPeersSelfResolved;
            case RESUME_SEARCHING_PEERS_WIND_UP:
                return this.isResumeSearchingPeersWindUp;
            default:
                return false;
        }
    }

    public boolean isStartedSearchingPeersInProgress() {
        return isStartedSearchingPeersInProgress;
    }

    public boolean isStartedSearchingPeersSelfResolved() {
        return isStartedSearchingPeersSelfResolved;
    }

    public boolean isStartedSearchingPeersWindUp() {
        return isStartedSearchingPeersWindUp;
    }

    public boolean isPauseSearchingPeersInProgress() {
        return isPauseSearchingPeersInProgress;
    }

    public boolean isPauseSearchingPeersSelfResolved() {
        return isPauseSearchingPeersSelfResolved;
    }

    public boolean isPauseSearchingPeersWindUp() {
        return isPauseSearchingPeersWindUp;
    }

    public boolean isResumeSearchingPeersInProgress() {
        return isResumeSearchingPeersInProgress;
    }

    public boolean isResumeSearchingPeersSelfResolved() {
        return isResumeSearchingPeersSelfResolved;
    }

    public boolean isResumeSearchingPeersWindUp() {
        return isResumeSearchingPeersWindUp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SearchPeersState that = (SearchPeersState) o;
        return isStartedSearchingPeersInProgress == that.isStartedSearchingPeersInProgress &&
                isStartedSearchingPeersSelfResolved == that.isStartedSearchingPeersSelfResolved &&
                isStartedSearchingPeersWindUp == that.isStartedSearchingPeersWindUp &&
                isPauseSearchingPeersInProgress == that.isPauseSearchingPeersInProgress &&
                isPauseSearchingPeersSelfResolved == that.isPauseSearchingPeersSelfResolved &&
                isPauseSearchingPeersWindUp == that.isPauseSearchingPeersWindUp &&
                isResumeSearchingPeersInProgress == that.isResumeSearchingPeersInProgress &&
                isResumeSearchingPeersSelfResolved == that.isResumeSearchingPeersSelfResolved &&
                isResumeSearchingPeersWindUp == that.isResumeSearchingPeersWindUp;
    }

    @Override
    public int hashCode() {

        return Objects.hash(isStartedSearchingPeersInProgress, isStartedSearchingPeersSelfResolved, isStartedSearchingPeersWindUp, isPauseSearchingPeersInProgress, isPauseSearchingPeersSelfResolved, isPauseSearchingPeersWindUp, isResumeSearchingPeersInProgress, isResumeSearchingPeersSelfResolved, isResumeSearchingPeersWindUp);
    }

    @Override
    public String toString() {
        return "SearchPeersState{" +
                "isStartedSearchingPeersInProgress=" + isStartedSearchingPeersInProgress +
                ", isStartedSearchingPeersSelfResolved=" + isStartedSearchingPeersSelfResolved +
                ", isStartedSearchingPeersWindUp=" + isStartedSearchingPeersWindUp +
                ", isPauseSearchingPeersInProgress=" + isPauseSearchingPeersInProgress +
                ", isPauseSearchingPeersSelfResolved=" + isPauseSearchingPeersSelfResolved +
                ", isPauseSearchingPeersWindUp=" + isPauseSearchingPeersWindUp +
                ", isResumeSearchingPeersInProgress=" + isResumeSearchingPeersInProgress +
                ", isResumeSearchingPeersSelfResolved=" + isResumeSearchingPeersSelfResolved +
                ", isResumeSearchingPeersWindUp=" + isResumeSearchingPeersWindUp +
                '}';
    }

    public static class PeersStateBuilder {
        private boolean isStartedSearchingPeersInProgress;
        private boolean isStartedSearchingPeersSelfResolved;
        private boolean isStartedSearchingPeersWindUp;
        private boolean isPauseSearchingPeersInProgress;
        private boolean isPauseSearchingPeersSelfResolved;
        private boolean isPauseSearchingPeersWindUp;
        private boolean isResumeSearchingPeersInProgress;
        private boolean isResumeSearchingPeersSelfResolved;
        private boolean isResumeSearchingPeersWindUp;

        public static PeersStateBuilder builder(SearchPeersState searchPeersState) {
            return new PeersStateBuilder(searchPeersState);
        }

        public static PeersStateBuilder builder() {
            return new PeersStateBuilder();
        }

        public SearchPeersState build() {
            return new SearchPeersState(
                    this.isStartedSearchingPeersInProgress,
                    this.isStartedSearchingPeersSelfResolved,
                    this.isStartedSearchingPeersWindUp,
                    this.isPauseSearchingPeersInProgress,
                    this.isPauseSearchingPeersSelfResolved,
                    this.isPauseSearchingPeersWindUp,
                    this.isResumeSearchingPeersInProgress,
                    this.isResumeSearchingPeersSelfResolved,
                    this.isResumeSearchingPeersWindUp);
        }

        private PeersStateBuilder() {
        }

        public PeersStateBuilder(SearchPeersState searchPeersState) {
            this.isStartedSearchingPeersInProgress = searchPeersState.isStartedSearchingPeersInProgress;
            this.isStartedSearchingPeersSelfResolved = searchPeersState.isStartedSearchingPeersSelfResolved;
            this.isStartedSearchingPeersWindUp = searchPeersState.isStartedSearchingPeersWindUp;
            this.isPauseSearchingPeersInProgress = searchPeersState.isPauseSearchingPeersInProgress;
            this.isPauseSearchingPeersSelfResolved = searchPeersState.isPauseSearchingPeersSelfResolved;
            this.isPauseSearchingPeersWindUp = searchPeersState.isPauseSearchingPeersWindUp;
            this.isResumeSearchingPeersInProgress = searchPeersState.isResumeSearchingPeersInProgress;
            this.isResumeSearchingPeersSelfResolved = searchPeersState.isResumeSearchingPeersSelfResolved;
            this.isResumeSearchingPeersWindUp = searchPeersState.isResumeSearchingPeersWindUp;
        }

        public PeersStateBuilder setStartedSearchingPeersSelfResolved(boolean startedSearchingPeersSelfResolved) {
            isStartedSearchingPeersSelfResolved = startedSearchingPeersSelfResolved;
            return this;
        }

        public PeersStateBuilder setPauseSearchingPeersSelfResolved(boolean pauseSearchingPeersSelfResolved) {
            isPauseSearchingPeersSelfResolved = pauseSearchingPeersSelfResolved;
            return this;
        }

        public PeersStateBuilder setResumeSearchingPeersSelfResolved(boolean resumeSearchingPeersSelfResolved) {
            isResumeSearchingPeersSelfResolved = resumeSearchingPeersSelfResolved;
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
