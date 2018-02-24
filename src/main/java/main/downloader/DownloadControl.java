package main.downloader;

import main.TorrentInfo;

public interface DownloadControl {

    void start();

    void resume();

    void pause();

    void stop();

    void remove();

    TorrentInfo getTorrentInfo();
}
