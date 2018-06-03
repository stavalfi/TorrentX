package main.torrent.status.side.effects;

import main.TorrentInfo;
import main.torrent.status.TorrentStatusAction;
import main.torrent.status.state.tree.TorrentStatusState;
import redux.store.Store;

public class TorrentStatesSideEffects {
    private TorrentInfo torrentInfo;

    private DownloadStateSideEffects downloadStateSideEffects;
    private PeersStateSideEffects peersStateSideEffects;
    private TorrentFileSystemStatesSideEffects torrentFileSystemStatesSideEffects;

    public TorrentStatesSideEffects(TorrentInfo torrentInfo,
									Store<TorrentStatusState, TorrentStatusAction> store) {
        this.torrentInfo = torrentInfo;
        this.downloadStateSideEffects = new DownloadStateSideEffects(torrentInfo, store);
        this.peersStateSideEffects = new PeersStateSideEffects(torrentInfo, store);
        this.torrentFileSystemStatesSideEffects = new TorrentFileSystemStatesSideEffects(store);
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
