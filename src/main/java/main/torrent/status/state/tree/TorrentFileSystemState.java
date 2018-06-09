package main.torrent.status.state.tree;

import main.torrent.status.TorrentStatusAction;

import java.util.Objects;

public class TorrentFileSystemState {
    private boolean isTorrentRemovedInProgress;
    private boolean isTorrentRemovedSelfResolved;
    private boolean isTorrentRemovedWindUp;

    private boolean isFilesRemovedInProgress;
    private boolean isFilesRemovedSelfResolved;
    private boolean isFilesRemovedWindUp;

    private TorrentFileSystemState(boolean isTorrentRemovedInProgress,
                                   boolean isTorrentRemovedSelfResolved,
                                   boolean isTorrentRemovedWindUp,
                                   boolean isFilesRemovedInProgress,
                                   boolean isFilesRemovedSelfResolved,
                                   boolean isFilesRemovedWindUp) {
        this.isTorrentRemovedInProgress = isTorrentRemovedInProgress;
        this.isTorrentRemovedSelfResolved = isTorrentRemovedSelfResolved;
        this.isTorrentRemovedWindUp = isTorrentRemovedWindUp;
        this.isFilesRemovedInProgress = isFilesRemovedInProgress;
        this.isFilesRemovedSelfResolved = isFilesRemovedSelfResolved;
        this.isFilesRemovedWindUp = isFilesRemovedWindUp;
    }

    public boolean isNotInAnyRemovingState() {
        return !isFilesRemovedInProgress() &&
                !isFilesRemovedWindUp() &&
                !isTorrentRemovedInProgress() &&
                !isTorrentRemovedWindUp();
    }

    public boolean fromAction(TorrentStatusAction torrentStatusAction) {
        switch (torrentStatusAction) {
            case REMOVE_FILES_IN_PROGRESS:
                return this.isFilesRemovedInProgress;
            case REMOVE_FILES_SELF_RESOLVED:
                return this.isFilesRemovedSelfResolved;
            case REMOVE_FILES_WIND_UP:
                return this.isFilesRemovedWindUp;
            case REMOVE_TORRENT_IN_PROGRESS:
                return this.isTorrentRemovedInProgress;
            case REMOVE_TORRENT_SELF_RESOLVED:
                return this.isTorrentRemovedSelfResolved;
            case REMOVE_TORRENT_WIND_UP:
                return this.isTorrentRemovedWindUp;
            default:
                return false;
        }
    }

    public boolean isTorrentRemovedSelfResolved() {
        return isTorrentRemovedSelfResolved;
    }

    public boolean isFilesRemovedSelfResolved() {
        return isFilesRemovedSelfResolved;
    }

    public boolean isTorrentRemovedInProgress() {
        return isTorrentRemovedInProgress;
    }

    public boolean isTorrentRemovedWindUp() {
        return isTorrentRemovedWindUp;
    }

    public boolean isFilesRemovedInProgress() {
        return isFilesRemovedInProgress;
    }

    public boolean isFilesRemovedWindUp() {
        return isFilesRemovedWindUp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TorrentFileSystemState that = (TorrentFileSystemState) o;
        return isTorrentRemovedInProgress == that.isTorrentRemovedInProgress &&
                isTorrentRemovedSelfResolved == that.isTorrentRemovedSelfResolved &&
                isTorrentRemovedWindUp == that.isTorrentRemovedWindUp &&
                isFilesRemovedInProgress == that.isFilesRemovedInProgress &&
                isFilesRemovedSelfResolved == that.isFilesRemovedSelfResolved &&
                isFilesRemovedWindUp == that.isFilesRemovedWindUp;
    }

    @Override
    public int hashCode() {

        return Objects.hash(isTorrentRemovedInProgress, isTorrentRemovedSelfResolved, isTorrentRemovedWindUp, isFilesRemovedInProgress, isFilesRemovedSelfResolved, isFilesRemovedWindUp);
    }

    @Override
    public String toString() {
        return "TorrentFileSystemState{" +
                "isTorrentRemovedInProgress=" + isTorrentRemovedInProgress +
                ", isTorrentRemovedSelfResolved=" + isTorrentRemovedSelfResolved +
                ", isTorrentRemovedWindUp=" + isTorrentRemovedWindUp +
                ", isFilesRemovedInProgress=" + isFilesRemovedInProgress +
                ", isFilesRemovedSelfResolved=" + isFilesRemovedSelfResolved +
                ", isFilesRemovedWindUp=" + isFilesRemovedWindUp +
                '}';
    }

    public static class TorrentFileSystemStateBuilder {
        private boolean isTorrentRemovedInProgress;
        private boolean isTorrentRemovedSelfResolved;
        private boolean isTorrentRemovedWindUp;

        private boolean isFilesRemovedInProgress;
        private boolean isFilesRemovedSelfResolved;
        private boolean isFilesRemovedWindUp;

        public static TorrentFileSystemStateBuilder builder(TorrentFileSystemState torrentFileSystemState) {
            return new TorrentFileSystemStateBuilder(torrentFileSystemState);
        }

        public static TorrentFileSystemStateBuilder builder() {
            return new TorrentFileSystemStateBuilder();
        }

        public TorrentFileSystemState build() {
            return new TorrentFileSystemState(this.isTorrentRemovedInProgress,
                    this.isTorrentRemovedSelfResolved,
                    this.isTorrentRemovedWindUp,
                    this.isFilesRemovedInProgress,
                    this.isFilesRemovedSelfResolved,
                    this.isFilesRemovedWindUp);
        }

        private TorrentFileSystemStateBuilder() {
        }

        private TorrentFileSystemStateBuilder(TorrentFileSystemState torrentFileSystemState) {
            this.isTorrentRemovedInProgress = torrentFileSystemState.isTorrentRemovedInProgress;
            this.isTorrentRemovedSelfResolved = torrentFileSystemState.isTorrentRemovedSelfResolved;
            this.isTorrentRemovedWindUp = torrentFileSystemState.isTorrentRemovedWindUp;
            this.isFilesRemovedInProgress = torrentFileSystemState.isFilesRemovedInProgress;
            this.isFilesRemovedSelfResolved = torrentFileSystemState.isFilesRemovedSelfResolved;
            this.isFilesRemovedWindUp = torrentFileSystemState.isFilesRemovedWindUp;
        }

        public TorrentFileSystemStateBuilder setTorrentRemovedSelfResolved(boolean torrentRemovedSelfResolved) {
            isTorrentRemovedSelfResolved = torrentRemovedSelfResolved;
            return this;
        }

        public TorrentFileSystemStateBuilder setFilesRemovedSelfResolved(boolean filesRemovedSelfResolved) {
            isFilesRemovedSelfResolved = filesRemovedSelfResolved;
            return this;
        }

        public TorrentFileSystemStateBuilder setTorrentRemovedInProgress(boolean torrentRemovedInProgress) {
            isTorrentRemovedInProgress = torrentRemovedInProgress;
            return this;
        }

        public TorrentFileSystemStateBuilder setTorrentRemovedWindUp(boolean torrentRemovedWindUp) {
            isTorrentRemovedWindUp = torrentRemovedWindUp;
            return this;
        }

        public TorrentFileSystemStateBuilder setFilesRemovedInProgress(boolean filesRemovedInProgress) {
            isFilesRemovedInProgress = filesRemovedInProgress;
            return this;
        }

        public TorrentFileSystemStateBuilder setFilesRemovedWindUp(boolean filesRemovedWindUp) {
            isFilesRemovedWindUp = filesRemovedWindUp;
            return this;
        }
    }
}
