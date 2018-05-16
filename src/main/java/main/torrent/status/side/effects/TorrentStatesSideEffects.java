package main.torrent.status.side.effects;

import main.TorrentInfo;
import main.torrent.status.TorrentStatusStore;

public class TorrentStatesSideEffects {
    private TorrentInfo torrentInfo;

    private DownloadStateSideEffects downloadStateSideEffects;
    private PeersStateSideEffects peersStateSideEffects;
    private TorrentFileSystemStatesSideEffects torrentFileSystemStatesSideEffects;

    public TorrentStatesSideEffects(TorrentInfo torrentInfo,
                                    TorrentStatusStore store) {
        this.torrentInfo = torrentInfo;
        this.downloadStateSideEffects = new DownloadStateSideEffects(torrentInfo, store);
        this.peersStateSideEffects = new PeersStateSideEffects(torrentInfo, store);
        this.torrentFileSystemStatesSideEffects = new TorrentFileSystemStatesSideEffects(torrentInfo, store);
    }

    public TorrentInfo getTorrentInfo() {
        return torrentInfo;
    }

    public DownloadStateSideEffects getDownloadStateSideEffects() {
        return downloadStateSideEffects;
    }

    public PeersStateSideEffects getPeersStateSideEffects() {
        return peersStateSideEffects;
    }

    public TorrentFileSystemStatesSideEffects getTorrentFileSystemStatesSideEffects() {
        return torrentFileSystemStatesSideEffects;
    }
}
