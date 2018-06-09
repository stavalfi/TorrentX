package main.torrent.status.reducers;

import main.torrent.status.TorrentStatusAction;
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

    public TorrentFileSystemState reducer(TorrentStatusState lastState, TorrentStatusAction torrentStatusAction) {
        TorrentFileSystemState torrentFileSystemState = lastState.getTorrentFileSystemState();

        switch (torrentStatusAction) {
            case REMOVE_FILES_IN_PROGRESS:
                if (torrentFileSystemState.isFilesRemovedInProgress() ||
                        torrentFileSystemState.isFilesRemovedWindUp())
                    return torrentFileSystemState;
                return TorrentFileSystemState.TorrentFileSystemStateBuilder.builder(torrentFileSystemState)
                        .setFilesRemovedInProgress(true)
                        .build();
            case REMOVE_FILES_SELF_RESOLVED:
                if (!torrentFileSystemState.isFilesRemovedInProgress() ||
                        torrentFileSystemState.isFilesRemovedSelfResolved() ||
                        torrentFileSystemState.isFilesRemovedWindUp())
                    return torrentFileSystemState;
                return TorrentFileSystemState.TorrentFileSystemStateBuilder.builder(torrentFileSystemState)
                        .setFilesRemovedSelfResolved(true)
                        .build();
            case REMOVE_FILES_WIND_UP:
                if (!torrentFileSystemState.isFilesRemovedInProgress() ||
                        !torrentFileSystemState.isFilesRemovedSelfResolved() ||
                        torrentFileSystemState.isFilesRemovedWindUp() ||
                        !lastState.getDownloadState().isPauseDownloadWindUp() ||
                        !lastState.getDownloadState().isPauseUploadWindUp() ||
                        !lastState.getSearchPeersState().isPauseSearchingPeersWindUp())
                    return torrentFileSystemState;
                return TorrentFileSystemState.TorrentFileSystemStateBuilder.builder(torrentFileSystemState)
                        .setFilesRemovedInProgress(false)
                        .setFilesRemovedSelfResolved(false)
                        .setFilesRemovedWindUp(true)
                        .build();
            case REMOVE_TORRENT_IN_PROGRESS:
                if (torrentFileSystemState.isTorrentRemovedInProgress() ||
                        torrentFileSystemState.isTorrentRemovedWindUp())
                    return torrentFileSystemState;
                return TorrentFileSystemState.TorrentFileSystemStateBuilder.builder(torrentFileSystemState)
                        .setTorrentRemovedInProgress(true)
                        .build();
            case REMOVE_TORRENT_SELF_RESOLVED:
                if (!torrentFileSystemState.isTorrentRemovedInProgress() ||
                        torrentFileSystemState.isTorrentRemovedSelfResolved() ||
                        torrentFileSystemState.isTorrentRemovedWindUp())
                    return torrentFileSystemState;
                return TorrentFileSystemState.TorrentFileSystemStateBuilder.builder(torrentFileSystemState)
                        .setTorrentRemovedSelfResolved(true)
                        .build();
            case REMOVE_TORRENT_WIND_UP:
                if (!torrentFileSystemState.isTorrentRemovedInProgress() ||
                        !torrentFileSystemState.isTorrentRemovedSelfResolved() ||
                        torrentFileSystemState.isTorrentRemovedWindUp() ||
                        !lastState.getDownloadState().isPauseDownloadWindUp() ||
                        !lastState.getDownloadState().isPauseUploadWindUp() ||
                        !lastState.getSearchPeersState().isPauseSearchingPeersWindUp())
                    return torrentFileSystemState;
                return TorrentFileSystemState.TorrentFileSystemStateBuilder.builder(torrentFileSystemState)
                        .setTorrentRemovedInProgress(false)
                        .setTorrentRemovedSelfResolved(false)
                        .setTorrentRemovedWindUp(true)
                        .build();
        }

        return lastState.getTorrentFileSystemState();
    }
}
