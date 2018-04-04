package main.torrent.status;

public enum TorrentStatusType {
    START_DOWNLOAD,
    START_UPLOAD,
    PAUSE_DOWNLOAD,
    RESUME_DOWNLOAD,
    PAUSE_UPLOAD,
    RESUME_UPLOAD,
    COMPLETED_DOWNLOADING,
    REMOVE_TORRENT,
    REMOVE_FILES
}
