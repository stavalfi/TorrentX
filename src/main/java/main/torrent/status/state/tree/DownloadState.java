package main.torrent.status.state.tree;

import main.torrent.status.Action;

import java.util.Objects;

public class DownloadState {
    private boolean isStartDownloadInProgress;
    private boolean isStartDownloadWindUp;

    private boolean isPauseDownloadInProgress;
    private boolean isPauseDownloadWindUp;

    private boolean isResumeDownloadInProgress;
    private boolean isResumeDownloadWindUp;

    private boolean isStartUploadInProgress;
    private boolean isStartUploadWindUp;

    private boolean isPauseUploadInProgress;
    private boolean isPauseUploadWindUp;

    private boolean isResumeUploadInProgress;
    private boolean isResumeUploadWindUp;

    private boolean isCompletedDownloadingInProgress;
    private boolean isCompletedDownloadingWindUp;

    private DownloadState(boolean isStartDownloadInProgress,
                          boolean isStartDownloadWindUp,
                          boolean isPauseDownloadInProgress,
                          boolean isPauseDownloadWindUp,
                          boolean isResumeDownloadInProgress,
                          boolean isResumeDownloadWindUp,
                          boolean isStartUploadInProgress,
                          boolean isStartUploadWindUp,
                          boolean isPauseUploadInProgress,
                          boolean isPauseUploadWindUp,
                          boolean isResumeUploadInProgress,
                          boolean isResumeUploadWindUp,
                          boolean isCompletedDownloadingInProgress,
                          boolean isCompletedDownloadingWindUp) {
        this.isStartDownloadInProgress = isStartDownloadInProgress;
        this.isStartDownloadWindUp = isStartDownloadWindUp;
        this.isPauseDownloadInProgress = isPauseDownloadInProgress;
        this.isPauseDownloadWindUp = isPauseDownloadWindUp;
        this.isResumeDownloadInProgress = isResumeDownloadInProgress;
        this.isResumeDownloadWindUp = isResumeDownloadWindUp;
        this.isStartUploadInProgress = isStartUploadInProgress;
        this.isStartUploadWindUp = isStartUploadWindUp;
        this.isPauseUploadInProgress = isPauseUploadInProgress;
        this.isPauseUploadWindUp = isPauseUploadWindUp;
        this.isResumeUploadInProgress = isResumeUploadInProgress;
        this.isResumeUploadWindUp = isResumeUploadWindUp;
        this.isCompletedDownloadingInProgress = isCompletedDownloadingInProgress;
        this.isCompletedDownloadingWindUp = isCompletedDownloadingWindUp;
    }

    public boolean fromAction(Action action) {
        switch (action) {
            case START_DOWNLOAD_IN_PROGRESS:
                return this.isStartDownloadInProgress;
            case START_DOWNLOAD_WIND_UP:
                return this.isStartDownloadWindUp;
            case PAUSE_DOWNLOAD_IN_PROGRESS:
                return this.isPauseDownloadInProgress;
            case PAUSE_DOWNLOAD_WIND_UP:
                return this.isPauseDownloadWindUp;
            case RESUME_DOWNLOAD_IN_PROGRESS:
                return this.isResumeDownloadInProgress;
            case RESUME_DOWNLOAD_WIND_UP:
                return this.isResumeDownloadWindUp;
            case START_UPLOAD_IN_PROGRESS:
                return this.isStartUploadInProgress;
            case START_UPLOAD_WIND_UP:
                return this.isStartUploadWindUp;
            case PAUSE_UPLOAD_IN_PROGRESS:
                return this.isPauseUploadInProgress;
            case PAUSE_UPLOAD_WIND_UP:
                return this.isPauseUploadWindUp;
            case RESUME_UPLOAD_IN_PROGRESS:
                return this.isResumeUploadInProgress;
            case RESUME_UPLOAD_WIND_UP:
                return this.isResumeUploadWindUp;
            case COMPLETED_DOWNLOADING_IN_PROGRESS:
                return this.isCompletedDownloadingInProgress;
            case COMPLETED_DOWNLOADING_WIND_UP:
                return this.isCompletedDownloadingWindUp;
            default:
                return false;
        }
    }

    public boolean isStartDownloadInProgress() {
        return isStartDownloadInProgress;
    }

    public boolean isStartDownloadWindUp() {
        return isStartDownloadWindUp;
    }

    public boolean isPauseDownloadInProgress() {
        return isPauseDownloadInProgress;
    }

    public boolean isPauseDownloadWindUp() {
        return isPauseDownloadWindUp;
    }

    public boolean isResumeDownloadInProgress() {
        return isResumeDownloadInProgress;
    }

    public boolean isResumeDownloadWindUp() {
        return isResumeDownloadWindUp;
    }

    public boolean isStartUploadInProgress() {
        return isStartUploadInProgress;
    }

    public boolean isStartUploadWindUp() {
        return isStartUploadWindUp;
    }

    public boolean isPauseUploadInProgress() {
        return isPauseUploadInProgress;
    }

    public boolean isPauseUploadWindUp() {
        return isPauseUploadWindUp;
    }

    public boolean isResumeUploadInProgress() {
        return isResumeUploadInProgress;
    }

    public boolean isResumeUploadWindUp() {
        return isResumeUploadWindUp;
    }

    public boolean isCompletedDownloadingInProgress() {
        return isCompletedDownloadingInProgress;
    }

    public boolean isCompletedDownloadingWindUp() {
        return isCompletedDownloadingWindUp;
    }

    public static class DownloadStateBuilder {
        private boolean isStartDownloadInProgress;
        private boolean isStartDownloadWindUp;

        private boolean isPauseDownloadInProgress;
        private boolean isPauseDownloadWindUp;

        private boolean isResumeDownloadInProgress;
        private boolean isResumeDownloadWindUp;

        private boolean isStartUploadInProgress;
        private boolean isStartUploadWindUp;

        private boolean isPauseUploadInProgress;
        private boolean isPauseUploadWindUp;

        private boolean isResumeUploadInProgress;
        private boolean isResumeUploadWindUp;

        private boolean isCompletedDownloadingInProgress;
        private boolean isCompletedDownloadingWindUp;

        public DownloadStateBuilder() {

        }

        private DownloadStateBuilder(DownloadState downloadState) {
            this.isStartDownloadInProgress = downloadState.isStartDownloadInProgress;
            this.isStartDownloadWindUp = downloadState.isStartDownloadWindUp;
            this.isPauseDownloadInProgress = downloadState.isPauseDownloadInProgress;
            this.isPauseDownloadWindUp = downloadState.isPauseDownloadWindUp;
            this.isResumeDownloadInProgress = downloadState.isResumeDownloadInProgress;
            this.isResumeDownloadWindUp = downloadState.isResumeDownloadWindUp;
            this.isStartUploadInProgress = downloadState.isStartUploadInProgress;
            this.isStartUploadWindUp = downloadState.isStartUploadWindUp;
            this.isPauseUploadInProgress = downloadState.isPauseUploadInProgress;
            this.isPauseUploadWindUp = downloadState.isPauseUploadWindUp;
            this.isResumeUploadInProgress = downloadState.isResumeUploadInProgress;
            this.isResumeUploadWindUp = downloadState.isResumeUploadWindUp;
            this.isCompletedDownloadingInProgress = downloadState.isCompletedDownloadingInProgress;
            this.isCompletedDownloadingWindUp = downloadState.isCompletedDownloadingWindUp;
        }

        public static DownloadStateBuilder builder(DownloadState downloadState) {
            return new DownloadStateBuilder(downloadState);
        }

        public static DownloadStateBuilder builder() {
            return new DownloadStateBuilder();
        }

        public DownloadState build() {
            return new DownloadState(this.isStartDownloadInProgress,
                    this.isStartDownloadWindUp,
                    this.isPauseDownloadInProgress,
                    this.isPauseDownloadWindUp,
                    this.isResumeDownloadInProgress,
                    this.isResumeDownloadWindUp,
                    this.isStartUploadInProgress,
                    this.isStartUploadWindUp,
                    this.isPauseUploadInProgress,
                    this.isPauseUploadWindUp,
                    this.isResumeUploadInProgress,
                    this.isResumeUploadWindUp,
                    this.isCompletedDownloadingInProgress,
                    this.isCompletedDownloadingWindUp);
        }

        public DownloadStateBuilder setStartDownloadInProgress(boolean startDownloadInProgress) {
            isStartDownloadInProgress = startDownloadInProgress;
            return this;
        }

        public DownloadStateBuilder setStartDownloadWindUp(boolean startDownloadWindUp) {
            isStartDownloadWindUp = startDownloadWindUp;
            return this;
        }

        public DownloadStateBuilder setPauseDownloadInProgress(boolean pauseDownloadInProgress) {
            isPauseDownloadInProgress = pauseDownloadInProgress;
            return this;
        }

        public DownloadStateBuilder setPauseDownloadWindUp(boolean pauseDownloadWindUp) {
            isPauseDownloadWindUp = pauseDownloadWindUp;
            return this;
        }

        public DownloadStateBuilder setResumeDownloadInProgress(boolean resumeDownloadInProgress) {
            isResumeDownloadInProgress = resumeDownloadInProgress;
            return this;
        }

        public DownloadStateBuilder setResumeDownloadWindUp(boolean resumeDownloadWindUp) {
            isResumeDownloadWindUp = resumeDownloadWindUp;
            return this;
        }

        public DownloadStateBuilder setStartUploadInProgress(boolean startUploadInProgress) {
            isStartUploadInProgress = startUploadInProgress;
            return this;
        }

        public DownloadStateBuilder setStartUploadWindUp(boolean startUploadWindUp) {
            isStartUploadWindUp = startUploadWindUp;
            return this;
        }

        public DownloadStateBuilder setPauseUploadInProgress(boolean pauseUploadInProgress) {
            isPauseUploadInProgress = pauseUploadInProgress;
            return this;
        }

        public DownloadStateBuilder setPauseUploadWindUp(boolean pauseUploadWindUp) {
            isPauseUploadWindUp = pauseUploadWindUp;
            return this;
        }

        public DownloadStateBuilder setResumeUploadInProgress(boolean resumeUploadInProgress) {
            isResumeUploadInProgress = resumeUploadInProgress;
            return this;
        }

        public DownloadStateBuilder setResumeUploadWindUp(boolean resumeUploadWindUp) {
            isResumeUploadWindUp = resumeUploadWindUp;
            return this;
        }

        public DownloadStateBuilder setCompletedDownloadingInProgress(boolean completedDownloadingInProgress) {
            isCompletedDownloadingInProgress = completedDownloadingInProgress;
            return this;
        }

        public DownloadStateBuilder setCompletedDownloadingWindUp(boolean completedDownloadingWindUp) {
            isCompletedDownloadingWindUp = completedDownloadingWindUp;
            return this;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DownloadState)) return false;
        DownloadState that = (DownloadState) o;
        return isStartDownloadInProgress() == that.isStartDownloadInProgress() &&
                isStartDownloadWindUp() == that.isStartDownloadWindUp() &&
                isPauseDownloadInProgress() == that.isPauseDownloadInProgress() &&
                isPauseDownloadWindUp() == that.isPauseDownloadWindUp() &&
                isResumeDownloadInProgress() == that.isResumeDownloadInProgress() &&
                isResumeDownloadWindUp() == that.isResumeDownloadWindUp() &&
                isStartUploadInProgress() == that.isStartUploadInProgress() &&
                isStartUploadWindUp() == that.isStartUploadWindUp() &&
                isPauseUploadInProgress() == that.isPauseUploadInProgress() &&
                isPauseUploadWindUp() == that.isPauseUploadWindUp() &&
                isResumeUploadInProgress() == that.isResumeUploadInProgress() &&
                isResumeUploadWindUp() == that.isResumeUploadWindUp() &&
                isCompletedDownloadingInProgress() == that.isCompletedDownloadingInProgress() &&
                isCompletedDownloadingWindUp() == that.isCompletedDownloadingWindUp();
    }

    @Override
    public int hashCode() {

        return Objects.hash(isStartDownloadInProgress(), isStartDownloadWindUp(), isPauseDownloadInProgress(), isPauseDownloadWindUp(), isResumeDownloadInProgress(), isResumeDownloadWindUp(), isStartUploadInProgress(), isStartUploadWindUp(), isPauseUploadInProgress(), isPauseUploadWindUp(), isResumeUploadInProgress(), isResumeUploadWindUp(), isCompletedDownloadingInProgress(), isCompletedDownloadingWindUp());
    }

    @Override
    public String toString() {
        return "DownloadState{" +
                "isStartDownloadInProgress=" + isStartDownloadInProgress +
                ", isStartDownloadWindUp=" + isStartDownloadWindUp +
                ", isPauseDownloadInProgress=" + isPauseDownloadInProgress +
                ", isPauseDownloadWindUp=" + isPauseDownloadWindUp +
                ", isResumeDownloadInProgress=" + isResumeDownloadInProgress +
                ", isResumeDownloadWindUp=" + isResumeDownloadWindUp +
                ", isStartUploadInProgress=" + isStartUploadInProgress +
                ", isStartUploadWindUp=" + isStartUploadWindUp +
                ", isPauseUploadInProgress=" + isPauseUploadInProgress +
                ", isPauseUploadWindUp=" + isPauseUploadWindUp +
                ", isResumeUploadInProgress=" + isResumeUploadInProgress +
                ", isResumeUploadWindUp=" + isResumeUploadWindUp +
                ", isCompletedDownloadingInProgress=" + isCompletedDownloadingInProgress +
                ", isCompletedDownloadingWindUp=" + isCompletedDownloadingWindUp +
                '}';
    }
}
