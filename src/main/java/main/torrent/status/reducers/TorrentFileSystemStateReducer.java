package main.torrent.status.reducers;

import main.torrent.status.Action;
import main.torrent.status.state.tree.TorrentFileSystemState;
import main.torrent.status.state.tree.TorrentStatusState;

import java.util.function.Supplier;

public class TorrentFileSystemStateReducer {
    public static Supplier<TorrentFileSystemState> defaultTorrentFileSystemStateSupplier = () ->
            TorrentFileSystemState.TorrentFileSystemStateBuilder.builder()
                    .setFilesRemovedInProgress(false)
                    .setFilesRemovedWindUp(false)
                    .setTorrentRemovedInProgress(false)
                    .setTorrentRemovedWindUp(false)
                    .build();

    public TorrentFileSystemState reducer(TorrentStatusState oldState, Action action) {
        return oldState.getTorrentFileSystemState();
    }
}
