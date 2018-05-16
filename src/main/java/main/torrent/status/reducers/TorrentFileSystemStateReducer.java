package main.torrent.status.reducers;

import main.torrent.status.Action;
import main.torrent.status.state.tree.TorrentFileSystemState;
import main.torrent.status.state.tree.TorrentStatusState;

import java.util.function.Supplier;

public class TorrentFileSystemStateReducer {
    public static Supplier<TorrentFileSystemState> defaultTorrentFileSystemStateSupplier = () ->
            TorrentFileSystemState.TorrentFileSystemStateBuilder.builder()
                    .setFilesRemovedInProgress(false)
                    .setFilesRemovedSelfResolved(false)
                    .setFilesRemovedWindUp(false)
                    .setTorrentRemovedInProgress(false)
                    .setTorrentRemovedSelfResolved(false)
                    .setTorrentRemovedWindUp(false)
                    .build();

    public TorrentFileSystemState reducer(TorrentStatusState lastState, Action action) {
        TorrentFileSystemState torrentFileSystemState = lastState.getTorrentFileSystemState();

        switch (action) {
            case REMOVE_FILES_IN_PROGRESS:
                if (torrentFileSystemState.isFilesRemovedInProgress() ||
                        torrentFileSystemState.isFilesRemovedWindUp())
                    return torrentFileSystemState;
                return TorrentFileSystemState.TorrentFileSystemStateBuilder.builder(torrentFileSystemState)
                        .setFilesRemovedInProgress(true)
                        .build();
            case REMOVE_FILES_WIND_UP:
                if (!torrentFileSystemState.isFilesRemovedInProgress() ||
                        torrentFileSystemState.isFilesRemovedWindUp() ||
                        !lastState.getDownloadState().isPauseDownloadWindUp() ||
                        !lastState.getDownloadState().isPauseUploadWindUp() ||
                        !lastState.getPeersState().isPauseSearchingPeersWindUp())
                    return torrentFileSystemState;
                return TorrentFileSystemState.TorrentFileSystemStateBuilder.builder(torrentFileSystemState)
                        .setFilesRemovedInProgress(false)
                        .setFilesRemovedWindUp(true)
                        .build();
            case REMOVE_TORRENT_IN_PROGRESS:
                if (torrentFileSystemState.isTorrentRemovedInProgress() ||
                        torrentFileSystemState.isTorrentRemovedWindUp())
                    return torrentFileSystemState;
                return TorrentFileSystemState.TorrentFileSystemStateBuilder.builder(torrentFileSystemState)
                        .setTorrentRemovedInProgress(true)
                        .build();
            case REMOVE_TORRENT_WIND_UP:
                if (!torrentFileSystemState.isTorrentRemovedInProgress() ||
                        torrentFileSystemState.isTorrentRemovedWindUp() ||
                        !lastState.getDownloadState().isPauseDownloadWindUp() ||
                        !lastState.getDownloadState().isPauseUploadWindUp() ||
                        !lastState.getPeersState().isPauseSearchingPeersWindUp())
                    return torrentFileSystemState;
                return TorrentFileSystemState.TorrentFileSystemStateBuilder.builder(torrentFileSystemState)
                        .setTorrentRemovedInProgress(false)
                        .setTorrentRemovedWindUp(true)
                        .build();
        }

        return lastState.getTorrentFileSystemState();
    }
}
