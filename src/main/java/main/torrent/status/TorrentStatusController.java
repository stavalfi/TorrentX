package main.torrent.status;

public interface TorrentStatusController extends TorrentStatus {

    void start();

    void resumeDownload();

    void resumeUpload();

    void pauseDownload();

    void pauseUpload();

    void removeTorrent();

    void removeFiles();

    void completedDownloading();
}
