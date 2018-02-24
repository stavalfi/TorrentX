package main.file;

import main.TorrentInfo;

import java.io.File;

public class ActiveTorrent {
    private TorrentInfo torrentInfo;
    private File file;

    public ActiveTorrent(TorrentInfo torrentInfo, File file) {
        this.torrentInfo = torrentInfo;
        this.file = file;
    }

    public TorrentInfo getTorrentInfo() {
        return torrentInfo;
    }

    public File getFile() {
        return file;
    }

    @Override
    public String toString() {
        return "ActiveTorrent{" +
                "torrentInfo=" + torrentInfo +
                '}';
    }
}
