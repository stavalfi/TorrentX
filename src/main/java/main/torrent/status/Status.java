package main.torrent.status;

import java.util.Objects;
import java.util.Optional;

public class Status {
    private StatusType changed;

    private boolean isStartedDownload;
    private boolean isStartedUpload;
    private boolean isTorrentRemoved;
    private boolean isFilesRemoved;
    private boolean isUploading;
    private boolean isDownloading;
    private boolean isCompletedDownloading;
    private boolean isStartedListeningToIncomingPeers;
    private boolean isListeningToIncomingPeers;
    private boolean isStartedSearchingPeers;
    private boolean isSearchingPeers;

    public Status(boolean isStartedDownload,
                  boolean isStartedUpload,
                  boolean isTorrentRemoved,
                  boolean isFilesRemoved,
                  boolean isUploading,
                  boolean isDownloading,
                  boolean isCompletedDownloading,
                  boolean isStartedListeningToIncomingPeers,
                  boolean isListeningToIncomingPeers,
                  boolean isStartedSearchingPeers,
                  boolean isSearchingPeers) {
        this.changed = null;
        this.isStartedDownload = isStartedDownload;
        this.isStartedUpload = isStartedUpload;
        this.isTorrentRemoved = isTorrentRemoved;
        this.isFilesRemoved = isFilesRemoved;
        this.isUploading = isUploading;
        this.isDownloading = isDownloading;
        this.isCompletedDownloading = isCompletedDownloading;
        this.isStartedListeningToIncomingPeers = isStartedListeningToIncomingPeers;
        this.isListeningToIncomingPeers = isListeningToIncomingPeers;
        this.isStartedSearchingPeers = isStartedSearchingPeers;
        this.isSearchingPeers = isSearchingPeers;
    }

    public Status(StatusType changed,
                  boolean isStartedDownload,
                  boolean isStartedUpload,
                  boolean isTorrentRemoved,
                  boolean isFilesRemoved,
                  boolean isUploading,
                  boolean isDownloading,
                  boolean isCompletedDownloading,
                  boolean isStartedListeningToIncomingPeers,
                  boolean isListeningToIncomingPeers,
                  boolean isStartedSearchingPeers,
                  boolean isSearchingPeers) {
        this.changed = changed;
        this.isStartedDownload = isStartedDownload;
        this.isStartedUpload = isStartedUpload;
        this.isTorrentRemoved = isTorrentRemoved;
        this.isFilesRemoved = isFilesRemoved;
        this.isUploading = isUploading;
        this.isDownloading = isDownloading;
        this.isCompletedDownloading = isCompletedDownloading;
        this.isStartedListeningToIncomingPeers = isStartedListeningToIncomingPeers;
        this.isListeningToIncomingPeers = isListeningToIncomingPeers;
        this.isStartedSearchingPeers = isStartedSearchingPeers;
        this.isSearchingPeers = isSearchingPeers;
    }

    public Optional<StatusType> getChanged() {
        return Optional.ofNullable(changed);
    }

    public boolean isStartedDownload() {
        return isStartedDownload;
    }

    public boolean isStartedUpload() {
        return isStartedUpload;
    }

    public boolean isTorrentRemoved() {
        return isTorrentRemoved;
    }

    public boolean isFilesRemoved() {
        return isFilesRemoved;
    }

    public boolean isUploading() {
        return isUploading;
    }

    public boolean isDownloading() {
        return isDownloading;
    }

    public boolean isCompletedDownloading() {
        return isCompletedDownloading;
    }

    public boolean isStartedListeningToIncomingPeers() {
        return isStartedListeningToIncomingPeers;
    }

    public boolean isListeningToIncomingPeers() {
        return isListeningToIncomingPeers;
    }

    public boolean isStartedSearchingPeers() {
        return isStartedSearchingPeers;
    }

    public boolean isSearchingPeers() {
        return isSearchingPeers;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Status)) return false;
        Status that = (Status) o;
        return isStartedDownload() == that.isStartedDownload() &&
                isStartedUpload() == that.isStartedUpload() &&
                isTorrentRemoved() == that.isTorrentRemoved() &&
                isFilesRemoved() == that.isFilesRemoved() &&
                isUploading() == that.isUploading() &&
                isDownloading() == that.isDownloading() &&
                isCompletedDownloading() == that.isCompletedDownloading() &&
                isStartedListeningToIncomingPeers() == that.isStartedListeningToIncomingPeers() &&
                isListeningToIncomingPeers() == that.isListeningToIncomingPeers() &&
                isStartedSearchingPeers() == that.isStartedSearchingPeers() &&
                isSearchingPeers() == that.isSearchingPeers();
    }

    @Override
    public int hashCode() {
        return Objects.hash(isStartedDownload(),
                isStartedUpload(),
                isTorrentRemoved(),
                isFilesRemoved(),
                isUploading(),
                isDownloading(),
                isCompletedDownloading(),
                isStartedListeningToIncomingPeers(),
                isListeningToIncomingPeers(),
                isStartedSearchingPeers(),
                isSearchingPeers());
    }

    @Override
    public String toString() {
        return "Status{" +
                "isStartedDownload=" + isStartedDownload +
                ", isStartedUpload=" + isStartedUpload +
                ", isTorrentRemoved=" + isTorrentRemoved +
                ", isFilesRemoved=" + isFilesRemoved +
                ", isUploading=" + isUploading +
                ", isDownloading=" + isDownloading +
                ", isCompletedDownloading=" + isCompletedDownloading +
                ", isStartedListeningToIncomingPeers=" + isStartedListeningToIncomingPeers +
                ", isListeningToIncomingPeers=" + isListeningToIncomingPeers +
                ", isStartedSearchingPeers=" + isStartedSearchingPeers +
                ", isSearchingPeers=" + isSearchingPeers +
                '}';
    }
}
