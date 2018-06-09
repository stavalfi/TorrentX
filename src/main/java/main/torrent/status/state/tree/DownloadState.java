package main.torrent.status.state.tree;

import main.torrent.status.TorrentStatusAction;

import java.util.Objects;

public class DownloadState {
    private boolean isStartDownloadInProgress;
    private boolean isStartDownloadSelfResolved;
    private boolean isStartDownloadWindUp;

    private boolean isPauseDownloadInProgress;
    private boolean isPauseDownloadSelfResolved;
    private boolean isPauseDownloadWindUp;

    private boolean isResumeDownloadInProgress;
    private boolean isResumeDownloadSelfResolved;
    private boolean isResumeDownloadWindUp;

    private boolean isStartUploadInProgress;
    private boolean isStartUploadSelfResolved;
    private boolean isStartUploadWindUp;

    private boolean isPauseUploadInProgress;
    private boolean isPauseUploadSelfResolved;
    private boolean isPauseUploadWindUp;

    private boolean isResumeUploadInProgress;
    private boolean isResumeUploadSelfResolved;
    private boolean isResumeUploadWindUp;

    private boolean isCompletedDownloadingInProgress;
    private boolean isCompletedDownloadingSelfResolved;
    private boolean isCompletedDownloadingWindUp;

    public DownloadState(boolean isStartDownloadInProgress,
                         boolean isStartDownloadSelfResolved,
                         boolean isStartDownloadWindUp,
                         boolean isPauseDownloadInProgress,
                         boolean isPauseDownloadSelfResolved,
                         boolean isPauseDownloadWindUp,
                         boolean isResumeDownloadInProgress,
                         boolean isResumeDownloadSelfResolved,
                         boolean isResumeDownloadWindUp,
                         boolean isStartUploadInProgress,
                         boolean isStartUploadSelfResolved,
                         boolean isStartUploadWindUp,
                         boolean isPauseUploadInProgress,
                         boolean isPauseUploadSelfResolved,
                         boolean isPauseUploadWindUp,
                         boolean isResumeUploadInProgress,
                         boolean isResumeUploadSelfResolved,
                         boolean isResumeUploadWindUp,
                         boolean isCompletedDownloadingInProgress,
                         boolean isCompletedDownloadingSelfResolved,
                         boolean isCompletedDownloadingWindUp) {
        this.isStartDownloadInProgress = isStartDownloadInProgress;
        this.isStartDownloadSelfResolved = isStartDownloadSelfResolved;
        this.isStartDownloadWindUp = isStartDownloadWindUp;
        this.isPauseDownloadInProgress = isPauseDownloadInProgress;
        this.isPauseDownloadSelfResolved = isPauseDownloadSelfResolved;
        this.isPauseDownloadWindUp = isPauseDownloadWindUp;
        this.isResumeDownloadInProgress = isResumeDownloadInProgress;
        this.isResumeDownloadSelfResolved = isResumeDownloadSelfResolved;
        this.isResumeDownloadWindUp = isResumeDownloadWindUp;
        this.isStartUploadInProgress = isStartUploadInProgress;
        this.isStartUploadSelfResolved = isStartUploadSelfResolved;
        this.isStartUploadWindUp = isStartUploadWindUp;
        this.isPauseUploadInProgress = isPauseUploadInProgress;
        this.isPauseUploadSelfResolved = isPauseUploadSelfResolved;
        this.isPauseUploadWindUp = isPauseUploadWindUp;
        this.isResumeUploadInProgress = isResumeUploadInProgress;
        this.isResumeUploadSelfResolved = isResumeUploadSelfResolved;
        this.isResumeUploadWindUp = isResumeUploadWindUp;
        this.isCompletedDownloadingInProgress = isCompletedDownloadingInProgress;
        this.isCompletedDownloadingSelfResolved = isCompletedDownloadingSelfResolved;
        this.isCompletedDownloadingWindUp = isCompletedDownloadingWindUp;
    }

    public boolean isNotInAnyCompleteState() {
        return !isCompletedDownloadingInProgress() &&
                !isCompletedDownloadingWindUp();
    }

    public boolean fromAction(TorrentStatusAction torrentStatusAction) {
        switch (torrentStatusAction) {
            case START_DOWNLOAD_IN_PROGRESS:
                return this.isStartDownloadInProgress;
            case START_DOWNLOAD_SELF_RESOLVED:
                return this.isStartDownloadSelfResolved;
            case START_DOWNLOAD_WIND_UP:
                return this.isStartDownloadWindUp;
            case PAUSE_DOWNLOAD_IN_PROGRESS:
                return this.isPauseDownloadInProgress;
            case PAUSE_DOWNLOAD_SELF_RESOLVED:
                return this.isPauseDownloadSelfResolved;
            case PAUSE_DOWNLOAD_WIND_UP:
                return this.isPauseDownloadWindUp;
            case RESUME_DOWNLOAD_IN_PROGRESS:
                return this.isResumeDownloadInProgress;
            case RESUME_DOWNLOAD_SELF_RESOLVED:
                return this.isResumeDownloadSelfResolved;
            case RESUME_DOWNLOAD_WIND_UP:
                return this.isResumeDownloadWindUp;
            case START_UPLOAD_IN_PROGRESS:
                return this.isStartUploadInProgress;
            case START_UPLOAD_SELF_RESOLVED:
                return this.isStartUploadSelfResolved;
            case START_UPLOAD_WIND_UP:
                return this.isStartUploadWindUp;
            case PAUSE_UPLOAD_IN_PROGRESS:
                return this.isPauseUploadInProgress;
            case PAUSE_UPLOAD_SELF_RESOLVED:
                return this.isPauseUploadSelfResolved;
            case PAUSE_UPLOAD_WIND_UP:
                return this.isPauseUploadWindUp;
            case RESUME_UPLOAD_IN_PROGRESS:
                return this.isResumeUploadInProgress;
            case RESUME_UPLOAD_SELF_RESOLVED:
                return this.isResumeUploadSelfResolved;
            case RESUME_UPLOAD_WIND_UP:
                return this.isResumeUploadWindUp;
            case COMPLETED_DOWNLOADING_IN_PROGRESS:
                return this.isCompletedDownloadingInProgress;
            case COMPLETED_DOWNLOADING_SELF_RESOLVED:
                return this.isCompletedDownloadingSelfResolved;
            case COMPLETED_DOWNLOADING_WIND_UP:
                return this.isCompletedDownloadingWindUp;
            default:
                return false;
        }
    }

    public boolean isStartDownloadInProgress() {
        return isStartDownloadInProgress;
    }

    public boolean isStartDownloadSelfResolved() {
        return isStartDownloadSelfResolved;
    }

    public boolean isStartDownloadWindUp() {
        return isStartDownloadWindUp;
    }

    public boolean isPauseDownloadInProgress() {
        return isPauseDownloadInProgress;
    }

    public boolean isPauseDownloadSelfResolved() {
        return isPauseDownloadSelfResolved;
    }

    public boolean isPauseDownloadWindUp() {
        return isPauseDownloadWindUp;
    }

    public boolean isResumeDownloadInProgress() {
        return isResumeDownloadInProgress;
    }

    public boolean isResumeDownloadSelfResolved() {
        return isResumeDownloadSelfResolved;
    }

    public boolean isResumeDownloadWindUp() {
        return isResumeDownloadWindUp;
    }

    public boolean isStartUploadInProgress() {
        return isStartUploadInProgress;
    }

    public boolean isStartUploadSelfResolved() {
        return isStartUploadSelfResolved;
    }

    public boolean isStartUploadWindUp() {
        return isStartUploadWindUp;
    }

    public boolean isPauseUploadInProgress() {
        return isPauseUploadInProgress;
    }

    public boolean isPauseUploadSelfResolved() {
        return isPauseUploadSelfResolved;
    }

    public boolean isPauseUploadWindUp() {
        return isPauseUploadWindUp;
    }

    public boolean isResumeUploadInProgress() {
        return isResumeUploadInProgress;
    }

    public boolean isResumeUploadSelfResolved() {
        return isResumeUploadSelfResolved;
    }

    public boolean isResumeUploadWindUp() {
        return isResumeUploadWindUp;
    }

    public boolean isCompletedDownloadingInProgress() {
        return isCompletedDownloadingInProgress;
    }

    public boolean isCompletedDownloadingSelfResolved() {
        return isCompletedDownloadingSelfResolved;
    }

    public boolean isCompletedDownloadingWindUp() {
        return isCompletedDownloadingWindUp;
    }

    public static class DownloadStateBuilder {
        private boolean isStartDownloadInProgress;
        private boolean isStartDownloadSelfResolved;
        private boolean isStartDownloadWindUp;

        private boolean isPauseDownloadInProgress;
        private boolean isPauseDownloadSelfResolved;
        private boolean isPauseDownloadWindUp;

        private boolean isResumeDownloadInProgress;
        private boolean isResumeDownloadSelfResolved;
        private boolean isResumeDownloadWindUp;

        private boolean isStartUploadInProgress;
        private boolean isStartUploadSelfResolved;
        private boolean isStartUploadWindUp;

        private boolean isPauseUploadInProgress;
        private boolean isPauseUploadSelfResolved;
        private boolean isPauseUploadWindUp;

        private boolean isResumeUploadInProgress;
        private boolean isResumeUploadSelfResolved;
        private boolean isResumeUploadWindUp;

        private boolean isCompletedDownloadingInProgress;
        private boolean isCompletedDownloadingSelfResolved;
        private boolean isCompletedDownloadingWindUp;

        public DownloadStateBuilder() {

        }

        private DownloadStateBuilder(DownloadState downloadState) {
            this.isStartDownloadInProgress = downloadState.isStartDownloadInProgress;
            this.isStartDownloadSelfResolved = downloadState.isStartDownloadSelfResolved;
            this.isStartDownloadWindUp = downloadState.isStartDownloadWindUp;
            this.isPauseDownloadInProgress = downloadState.isPauseDownloadInProgress;
            this.isPauseDownloadSelfResolved = downloadState.isPauseDownloadSelfResolved;
            this.isPauseDownloadWindUp = downloadState.isPauseDownloadWindUp;
            this.isResumeDownloadInProgress = downloadState.isResumeDownloadInProgress;
            this.isResumeDownloadSelfResolved = downloadState.isResumeDownloadSelfResolved;
            this.isResumeDownloadWindUp = downloadState.isResumeDownloadWindUp;
            this.isStartUploadInProgress = downloadState.isStartUploadInProgress;
            this.isStartUploadSelfResolved = downloadState.isStartUploadSelfResolved;
            this.isStartUploadWindUp = downloadState.isStartUploadWindUp;
            this.isPauseUploadInProgress = downloadState.isPauseUploadInProgress;
            this.isPauseUploadSelfResolved = downloadState.isPauseUploadSelfResolved;
            this.isPauseUploadWindUp = downloadState.isPauseUploadWindUp;
            this.isResumeUploadInProgress = downloadState.isResumeUploadInProgress;
            this.isResumeUploadSelfResolved = downloadState.isResumeUploadSelfResolved;
            this.isResumeUploadWindUp = downloadState.isResumeUploadWindUp;
            this.isCompletedDownloadingInProgress = downloadState.isCompletedDownloadingInProgress;
            this.isCompletedDownloadingSelfResolved = downloadState.isCompletedDownloadingSelfResolved;
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
                    this.isStartDownloadSelfResolved,
                    this.isStartDownloadWindUp,
                    this.isPauseDownloadInProgress,
                    this.isPauseDownloadSelfResolved,
                    this.isPauseDownloadWindUp,
                    this.isResumeDownloadInProgress,
                    this.isResumeDownloadSelfResolved,
                    this.isResumeDownloadWindUp,
                    this.isStartUploadInProgress,
                    this.isStartUploadSelfResolved,
                    this.isStartUploadWindUp,
                    this.isPauseUploadInProgress,
                    this.isPauseUploadSelfResolved,
                    this.isPauseUploadWindUp,
                    this.isResumeUploadInProgress,
                    this.isResumeUploadSelfResolved,
                    this.isResumeUploadWindUp,
                    this.isCompletedDownloadingInProgress,
                    this.isCompletedDownloadingSelfResolved,
                    this.isCompletedDownloadingWindUp);
        }

        public DownloadStateBuilder setStartDownloadSelfResolved(boolean startDownloadSelfResolved) {
            isStartDownloadSelfResolved = startDownloadSelfResolved;
            return this;
        }

        public DownloadStateBuilder setPauseDownloadSelfResolved(boolean pauseDownloadSelfResolved) {
            isPauseDownloadSelfResolved = pauseDownloadSelfResolved;
            return this;
        }

        public DownloadStateBuilder setResumeDownloadSelfResolved(boolean resumeDownloadSelfResolved) {
            isResumeDownloadSelfResolved = resumeDownloadSelfResolved;
            return this;
        }

        public DownloadStateBuilder setStartUploadSelfResolved(boolean startUploadSelfResolved) {
            isStartUploadSelfResolved = startUploadSelfResolved;
            return this;
        }

        public DownloadStateBuilder setPauseUploadSelfResolved(boolean pauseUploadSelfResolved) {
            isPauseUploadSelfResolved = pauseUploadSelfResolved;
            return this;
        }

        public DownloadStateBuilder setResumeUploadSelfResolved(boolean resumeUploadSelfResolved) {
            isResumeUploadSelfResolved = resumeUploadSelfResolved;
            return this;
        }

        public DownloadStateBuilder setCompletedDownloadingSelfResolved(boolean completedDownloadingSelfResolved) {
            isCompletedDownloadingSelfResolved = completedDownloadingSelfResolved;
            return this;
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
        if (o == null || getClass() != o.getClass()) return false;
        DownloadState that = (DownloadState) o;
        return isStartDownloadInProgress == that.isStartDownloadInProgress &&
                isStartDownloadSelfResolved == that.isStartDownloadSelfResolved &&
                isStartDownloadWindUp == that.isStartDownloadWindUp &&
                isPauseDownloadInProgress == that.isPauseDownloadInProgress &&
                isPauseDownloadSelfResolved == that.isPauseDownloadSelfResolved &&
                isPauseDownloadWindUp == that.isPauseDownloadWindUp &&
                isResumeDownloadInProgress == that.isResumeDownloadInProgress &&
                isResumeDownloadSelfResolved == that.isResumeDownloadSelfResolved &&
                isResumeDownloadWindUp == that.isResumeDownloadWindUp &&
                isStartUploadInProgress == that.isStartUploadInProgress &&
                isStartUploadSelfResolved == that.isStartUploadSelfResolved &&
                isStartUploadWindUp == that.isStartUploadWindUp &&
                isPauseUploadInProgress == that.isPauseUploadInProgress &&
                isPauseUploadSelfResolved == that.isPauseUploadSelfResolved &&
                isPauseUploadWindUp == that.isPauseUploadWindUp &&
                isResumeUploadInProgress == that.isResumeUploadInProgress &&
                isResumeUploadSelfResolved == that.isResumeUploadSelfResolved &&
                isResumeUploadWindUp == that.isResumeUploadWindUp &&
                isCompletedDownloadingInProgress == that.isCompletedDownloadingInProgress &&
                isCompletedDownloadingSelfResolved == that.isCompletedDownloadingSelfResolved &&
                isCompletedDownloadingWindUp == that.isCompletedDownloadingWindUp;
    }

    @Override
    public int hashCode() {

        return Objects.hash(isStartDownloadInProgress, isStartDownloadSelfResolved, isStartDownloadWindUp, isPauseDownloadInProgress, isPauseDownloadSelfResolved, isPauseDownloadWindUp, isResumeDownloadInProgress, isResumeDownloadSelfResolved, isResumeDownloadWindUp, isStartUploadInProgress, isStartUploadSelfResolved, isStartUploadWindUp, isPauseUploadInProgress, isPauseUploadSelfResolved, isPauseUploadWindUp, isResumeUploadInProgress, isResumeUploadSelfResolved, isResumeUploadWindUp, isCompletedDownloadingInProgress, isCompletedDownloadingSelfResolved, isCompletedDownloadingWindUp);
    }

    @Override
    public String toString() {
        return "DownloadState{" +
                "isStartDownloadInProgress=" + isStartDownloadInProgress +
                ", isStartDownloadSelfResolved=" + isStartDownloadSelfResolved +
                ", isStartDownloadWindUp=" + isStartDownloadWindUp +
                ", isPauseDownloadInProgress=" + isPauseDownloadInProgress +
                ", isPauseDownloadSelfResolved=" + isPauseDownloadSelfResolved +
                ", isPauseDownloadWindUp=" + isPauseDownloadWindUp +
                ", isResumeDownloadInProgress=" + isResumeDownloadInProgress +
                ", isResumeDownloadSelfResolved=" + isResumeDownloadSelfResolved +
                ", isResumeDownloadWindUp=" + isResumeDownloadWindUp +
                ", isStartUploadInProgress=" + isStartUploadInProgress +
                ", isStartUploadSelfResolved=" + isStartUploadSelfResolved +
                ", isStartUploadWindUp=" + isStartUploadWindUp +
                ", isPauseUploadInProgress=" + isPauseUploadInProgress +
                ", isPauseUploadSelfResolved=" + isPauseUploadSelfResolved +
                ", isPauseUploadWindUp=" + isPauseUploadWindUp +
                ", isResumeUploadInProgress=" + isResumeUploadInProgress +
                ", isResumeUploadSelfResolved=" + isResumeUploadSelfResolved +
                ", isResumeUploadWindUp=" + isResumeUploadWindUp +
                ", isCompletedDownloadingInProgress=" + isCompletedDownloadingInProgress +
                ", isCompletedDownloadingSelfResolved=" + isCompletedDownloadingSelfResolved +
                ", isCompletedDownloadingWindUp=" + isCompletedDownloadingWindUp +
                '}';
    }
}
