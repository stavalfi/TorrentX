package main.torrent.status.state.tree;

import main.torrent.status.Action;

import java.util.Objects;

public class TorrentStatusState {
    private Action action;

    private DownloadState downloadState;
    private PeersState peersState;
    private TorrentFileSystemState torrentFileSystemState;

    public TorrentStatusState(Action action,
                              DownloadState downloadState,
                              PeersState peersState,
                              TorrentFileSystemState torrentFileSystemState) {
        this.action = action;
        this.downloadState = downloadState;
        this.peersState = peersState;
        this.torrentFileSystemState = torrentFileSystemState;
    }

    public Action getAction() {
        return action;
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
        TorrentStatusState that = (TorrentStatusState) o;
        return Objects.equals(getAction(), that.getAction()) &&
                Objects.equals(getDownloadState(), that.getDownloadState()) &&
                Objects.equals(getPeersState(), that.getPeersState()) &&
                Objects.equals(getTorrentFileSystemState(), that.getTorrentFileSystemState());
    }

    @Override
    public int hashCode() {

        return Objects.hash(getAction(), getDownloadState(), getPeersState(), getTorrentFileSystemState());
    }

    @Override
    public String toString() {
        return "TorrentStatusState{" +
                "action=" + action +
                ", downloadState=" + downloadState +
                ", peersState=" + peersState +
                ", torrentFileSystemState=" + torrentFileSystemState +
                '}';
    }
}
