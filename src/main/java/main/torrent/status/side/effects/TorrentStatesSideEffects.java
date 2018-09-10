package main.torrent.status.side.effects;

import main.TorrentInfo;
import main.redux.store.Store;
import main.torrent.status.TorrentStatusAction;
import main.torrent.status.state.tree.TorrentStatusState;

public class TorrentStatesSideEffects {
    private TorrentInfo torrentInfo;

    private DownloadStateSideEffects downloadStateSideEffects;
    private SearchPeersStateSideEffects searchPeersStateSideEffects;
    private TorrentFileSystemStatesSideEffects torrentFileSystemStatesSideEffects;

    public TorrentStatesSideEffects(TorrentInfo torrentInfo,
									Store<TorrentStatusState, TorrentStatusAction> store) {
        this.torrentInfo = torrentInfo;
        this.downloadStateSideEffects = new DownloadStateSideEffects(torrentInfo, store);
        this.searchPeersStateSideEffects = new SearchPeersStateSideEffects(torrentInfo, store);
        this.torrentFileSystemStatesSideEffects = new TorrentFileSystemStatesSideEffects(store);
    }

    public TorrentInfo getTorrentInfo() {
        return torrentInfo;
    }

    public DownloadStateSideEffects getDownloadStateSideEffects() {
        return downloadStateSideEffects;
    }

    public SearchPeersStateSideEffects getSearchPeersStateSideEffects() {
        return searchPeersStateSideEffects;
    }

    public TorrentFileSystemStatesSideEffects getTorrentFileSystemStatesSideEffects() {
        return torrentFileSystemStatesSideEffects;
    }
}
