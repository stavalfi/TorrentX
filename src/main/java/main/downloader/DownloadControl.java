package main.downloader;

import main.TorrentInfo;

public interface DownloadControl {

    TorrentInfo getTorrentInfo();

    void start();

    void stop();

    void resume();

    void pause();

    void remove();
}
