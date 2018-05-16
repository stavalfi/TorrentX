package main.torrent.status.reducers;

import main.TorrentInfo;
import main.torrent.status.Action;
import main.torrent.status.state.tree.DownloadState;
import main.torrent.status.state.tree.PeersState;
import main.torrent.status.state.tree.TorrentFileSystemState;
import main.torrent.status.state.tree.TorrentStatusState;

import java.util.function.Function;

public class Reducer {

    public static Function<TorrentInfo, TorrentStatusState> defaultTorrentStateSupplier = torrentInfo ->
            new TorrentStatusState(torrentInfo, Action.INITIALIZE,
                    DownloadStateReducer.defaultDownloadStateSupplier.get(),
                    PeersStateReducer.defaultPeersStateSupplier.get(),
                    TorrentFileSystemStateReducer.defaultTorrentFileSystemStateSupplier.get());

    private DownloadStateReducer downloadStateReducer = new DownloadStateReducer();
    private PeersStateReducer peersStateReducer = new PeersStateReducer();
    private TorrentFileSystemStateReducer torrentFileSystemStateReducer = new TorrentFileSystemStateReducer();

    public TorrentStatusState reducer(TorrentInfo torrentInfo, TorrentStatusState lastState, Action action) {

        DownloadState downloadState = this.downloadStateReducer.reducer(lastState, action);
        PeersState peersState = this.peersStateReducer.reducer(lastState, action);
        TorrentFileSystemState torrentFileSystemState = this.torrentFileSystemStateReducer.reducer(lastState, action);
        if (lastState.getDownloadState().equals(downloadState) &&
                lastState.getPeersState().equals(peersState) &&
                lastState.getTorrentFileSystemState().equals(torrentFileSystemState))
            return lastState;
        return new TorrentStatusState(torrentInfo, action,
                downloadState,
                peersState,
                torrentFileSystemState);
    }
}
