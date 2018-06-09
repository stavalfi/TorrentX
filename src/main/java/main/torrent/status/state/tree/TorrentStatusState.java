package main.torrent.status.state.tree;

import main.torrent.status.TorrentStatusAction;
import redux.state.State;

public class TorrentStatusState extends State<TorrentStatusAction> {
    private DownloadState downloadState;
    private SearchPeersState searchPeersState;
    private TorrentFileSystemState torrentFileSystemState;

    public TorrentStatusState(String id,
                              TorrentStatusAction action,
                              DownloadState downloadState,
                              SearchPeersState searchPeersState,
                              TorrentFileSystemState torrentFileSystemState) {
        super(id, action);
        this.downloadState = downloadState;
        this.searchPeersState = searchPeersState;
        this.torrentFileSystemState = torrentFileSystemState;
    }

    public boolean fromAction(TorrentStatusAction torrentStatusAction) {
        return this.downloadState.fromAction(torrentStatusAction) ||
                this.searchPeersState.fromAction(torrentStatusAction) ||
                this.torrentFileSystemState.fromAction(torrentStatusAction);
    }

    public DownloadState getDownloadState() {
        return downloadState;
    }

    public SearchPeersState getSearchPeersState() {
        return searchPeersState;
    }

    public TorrentFileSystemState getTorrentFileSystemState() {
        return torrentFileSystemState;
    }

    @Override
    public String toString() {
        return "TorrentStatusState{" + super.toString() +
                "downloadState=" + downloadState +
                ", searchPeersState=" + searchPeersState +
                ", torrentFileSystemState=" + torrentFileSystemState +
                "} ";
    }
}
