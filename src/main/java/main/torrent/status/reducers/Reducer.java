package main.torrent.status.reducers;

import main.torrent.status.*;
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
        return new TorrentStatusState(action,
                this.downloadStateReducer.reducer(lastState, action),
                this.peersStateReducer.reducer(lastState, action),
                this.torrentFileSystemStateReducer.reducer(lastState, action));
    }
}
