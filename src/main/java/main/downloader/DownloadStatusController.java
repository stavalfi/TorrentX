package main.downloader;

public interface DownloadStatusController extends DownloadStatus{

    void start();

    void resume();

    void pause();

    void remove();

    void completedDownloading();
}
