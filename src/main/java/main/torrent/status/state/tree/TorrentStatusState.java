package main.torrent.status.state.tree;

import main.torrent.status.TorrentStatusAction;
import redux.state.State;

public class TorrentStatusState extends State<TorrentStatusAction> {
    private DownloadState downloadState;
    private PeersState peersState;
    private TorrentFileSystemState torrentFileSystemState;

    public TorrentStatusState(String id,
                              TorrentStatusAction action,
                              DownloadState downloadState,
                              PeersState peersState,
                              TorrentFileSystemState torrentFileSystemState) {
        super(id, action);
        this.downloadState = downloadState;
        this.peersState = peersState;
        this.torrentFileSystemState = torrentFileSystemState;
    }

    public boolean fromAction(TorrentStatusAction torrentStatusAction) {
        return this.downloadState.fromAction(torrentStatusAction) ||
                this.peersState.fromAction(torrentStatusAction) ||
                this.torrentFileSystemState.fromAction(torrentStatusAction);
    }

    public DownloadState getDownloadState() {
        return downloadState;
    }

    public PeersState getPeersState() {
        return peersState;
    }

    public TorrentFileSystemState getTorrentFileSystemState() {
        return torrentFileSystemState;
    }

    @Override
    public String toString() {
        return "TorrentStatusState{" + super.toString() +
                "downloadState=" + downloadState +
                ", peersState=" + peersState +
                ", torrentFileSystemState=" + torrentFileSystemState +
                "} ";
    }
}
