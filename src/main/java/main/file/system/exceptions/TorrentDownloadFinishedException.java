package main.file.system.exceptions;

import main.TorrentInfo;

public class TorrentDownloadFinishedException extends Exception {
    public TorrentDownloadFinishedException(TorrentInfo torrentInfo) {
        super("Can't build a request-message for a missing piece because all pieces were downloaded for torrent: " + torrentInfo);
    }
}
