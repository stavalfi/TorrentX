package main.torrent.status.state.tree;

import main.torrent.status.TorrentStatusAction;
import redux.state.State;

import java.util.Objects;

public class TorrentStatusState extends State<TorrentStatusAction> {
    private DownloadState downloadState;
    private PeersState peersState;
    private TorrentFileSystemState torrentFileSystemState;

    public TorrentStatusState(TorrentStatusAction torrentStatusAction,
                              DownloadState downloadState,
                              PeersState peersState,
                              TorrentFileSystemState torrentFileSystemState) {
        super(torrentStatusAction);
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
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TorrentStatusState)) return false;
        if (!super.equals(o)) return false;
        TorrentStatusState that = (TorrentStatusState) o;
        return Objects.equals(getDownloadState(), that.getDownloadState()) &&
                Objects.equals(getPeersState(), that.getPeersState()) &&
                Objects.equals(getTorrentFileSystemState(), that.getTorrentFileSystemState());
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getDownloadState(), getPeersState(), getTorrentFileSystemState());
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
