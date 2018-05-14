package main.torrent.status.state.tree;

import java.util.Objects;

public class TorrentFileSystemState {
    private boolean isTorrentRemovedInProgress;
    private boolean isTorrentRemovedWindUp;

    private boolean isFilesRemovedInProgress;
    private boolean isFilesRemovedWindUp;

    private TorrentFileSystemState(boolean isTorrentRemovedInProgress,
                                   boolean isTorrentRemovedWindUp,
                                   boolean isFilesRemovedInProgress,
                                   boolean isFilesRemovedWindUp) {
        this.isTorrentRemovedInProgress = isTorrentRemovedInProgress;
        this.isTorrentRemovedWindUp = isTorrentRemovedWindUp;
        this.isFilesRemovedInProgress = isFilesRemovedInProgress;
        this.isFilesRemovedWindUp = isFilesRemovedWindUp;
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

    public static class TorrentFileSystemStateBuilder {
        private boolean isTorrentRemovedInProgress;
        private boolean isTorrentRemovedWindUp;

        private boolean isFilesRemovedInProgress;
        private boolean isFilesRemovedWindUp;

        public static TorrentFileSystemStateBuilder builder(TorrentFileSystemState torrentFileSystemState) {
            return new TorrentFileSystemStateBuilder(torrentFileSystemState);
        }

        public static TorrentFileSystemStateBuilder builder() {
            return new TorrentFileSystemStateBuilder();
        }

        public TorrentFileSystemState build() {
            return new TorrentFileSystemState(this.isTorrentRemovedInProgress,
                    this.isTorrentRemovedWindUp,
                    this.isFilesRemovedInProgress,
                    this.isFilesRemovedWindUp);
        }

        private TorrentFileSystemStateBuilder() {
        }

        private TorrentFileSystemStateBuilder(TorrentFileSystemState torrentFileSystemState) {
            this.isTorrentRemovedInProgress = torrentFileSystemState.isTorrentRemovedInProgress;
            this.isTorrentRemovedWindUp = torrentFileSystemState.isTorrentRemovedWindUp;
            this.isFilesRemovedInProgress = torrentFileSystemState.isFilesRemovedInProgress;
            this.isFilesRemovedWindUp = torrentFileSystemState.isFilesRemovedWindUp;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TorrentFileSystemState)) return false;
        TorrentFileSystemState that = (TorrentFileSystemState) o;
        return isTorrentRemovedInProgress == that.isTorrentRemovedInProgress &&
                isTorrentRemovedWindUp == that.isTorrentRemovedWindUp &&
                isFilesRemovedInProgress == that.isFilesRemovedInProgress &&
                isFilesRemovedWindUp == that.isFilesRemovedWindUp;
    }

    @Override
    public int hashCode() {
        return Objects.hash(isTorrentRemovedInProgress, isTorrentRemovedWindUp, isFilesRemovedInProgress, isFilesRemovedWindUp);
    }


    @Override
    public String toString() {
        return "TorrentFileSystemState{" +
                "isTorrentRemovedInProgress=" + isTorrentRemovedInProgress +
                ", isTorrentRemovedWindUp=" + isTorrentRemovedWindUp +
                ", isFilesRemovedInProgress=" + isFilesRemovedInProgress +
                ", isFilesRemovedWindUp=" + isFilesRemovedWindUp +
                '}';
    }
}
