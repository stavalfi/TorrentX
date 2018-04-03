package main.torrent.status;

public interface TorrentStatusController extends TorrentStatus {

    void start();

    void resume();

    void pause();

    void remove();

    void completedDownloading();
}
