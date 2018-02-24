package main.downloader;

import main.TorrentInfo;

public interface DownloadControl {

    TorrentInfo getTorrentInfo();

    void start();

    void resume();

    void pause();

    void stop();

    void remove();
}
