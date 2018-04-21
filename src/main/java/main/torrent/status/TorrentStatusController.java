package main.torrent.status;

public interface TorrentStatusController extends TorrentStatus {

    void startDownload();

    void startUpload();

    void resumeDownload();

    void resumeUpload();

    void pauseDownload();

    void pauseUpload();

    void removeTorrent();

    void removeFiles();

    void completedDownloading();

    void startListeningToIncomingPeers();

    // TODO: if there is an error which made us stop listening,
    // I can't resume it without adding logic.
    void resumeListeningToIncomingPeers();

    void pauseListeningToIncomingPeers();

    void startSearchingPeers();

    void resumeSearchingPeers();

    void pauseSearchingPeers();
}
