package main.torrent.status.reducers;

import main.torrent.status.*;
import main.torrent.status.state.tree.DownloadState;
import main.torrent.status.state.tree.PeersState;
import main.torrent.status.state.tree.TorrentFileSystemState;
import main.torrent.status.state.tree.TorrentStatusState;

import java.util.function.Supplier;

public class Reducer {

    public static Supplier<TorrentStatusState> defaultTorrentStateSupplier = () ->
            new TorrentStatusState(Action.INITIALIZE,
                    DownloadStateReducer.defaultDownloadStateSupplier.get(),
                    PeersStateReducer.defaultPeersStateSupplier.get(),
                    TorrentFileSystemStateReducer.defaultTorrentFileSystemStateSupplier.get());

    private DownloadStateReducer downloadStateReducer = new DownloadStateReducer();
    private PeersStateReducer peersStateReducer = new PeersStateReducer();
    private TorrentFileSystemStateReducer torrentFileSystemStateReducer = new TorrentFileSystemStateReducer();

    public TorrentStatusState reducer(TorrentStatusState lastState, Action action) {

        DownloadState downloadState = this.downloadStateReducer.reducer(lastState, action);
        PeersState peersState = this.peersStateReducer.reducer(lastState, action);
        TorrentFileSystemState torrentFileSystemState = this.torrentFileSystemStateReducer.reducer(lastState, action);
        if (lastState.getDownloadState().equals(downloadState) &&
                lastState.getPeersState().equals(peersState) &&
                lastState.getTorrentFileSystemState().equals(torrentFileSystemState))
            return lastState;
        return new TorrentStatusState(action,
                downloadState,
                peersState,
                torrentFileSystemState);
    }
}
