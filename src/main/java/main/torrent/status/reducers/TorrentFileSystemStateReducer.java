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

    public TorrentFileSystemState reducer(TorrentStatusState lastState, Action action) {
        TorrentFileSystemState torrentFileSystemState = lastState.getTorrentFileSystemState();

        boolean isDownloadingOrUploadingOrInProgress = lastState.getDownloadState().isStartDownloadInProgress() ||
                lastState.getDownloadState().isStartDownloadWindUp() ||
                lastState.getDownloadState().isStartUploadInProgress() ||
                lastState.getDownloadState().isStartUploadWindUp();

        boolean areWeListeningOrSearchingOrInProgress = lastState.getPeersState().isResumeListeningToIncomingPeersInProgress() ||
                lastState.getPeersState().isResumeListeningToIncomingPeersWindUp() ||
                lastState.getPeersState().isResumeSearchingPeersInProgress() ||
                lastState.getPeersState().isResumeSearchingPeersWindUp();

        switch (action) {
            case REMOVE_FILES_IN_PROGRESS:
                if (torrentFileSystemState.isFilesRemovedInProgress() ||
                        torrentFileSystemState.isFilesRemovedWindUp())
                    return torrentFileSystemState;
                return TorrentFileSystemState.TorrentFileSystemStateBuilder.builder()
                        .setFilesRemovedInProgress(true)
                        .build();
            case REMOVE_FILES_WIND_UP:
                if (isDownloadingOrUploadingOrInProgress ||
                        areWeListeningOrSearchingOrInProgress ||
                        !torrentFileSystemState.isFilesRemovedInProgress() ||
                        torrentFileSystemState.isFilesRemovedWindUp())
                    return torrentFileSystemState;
                return TorrentFileSystemState.TorrentFileSystemStateBuilder.builder()
                        .setFilesRemovedInProgress(false)
                        .setFilesRemovedWindUp(true)
                        .build();
            case REMOVE_TORRENT_IN_PROGRESS:
                if (torrentFileSystemState.isTorrentRemovedInProgress() ||
                        torrentFileSystemState.isTorrentRemovedWindUp())
                    return torrentFileSystemState;
                return TorrentFileSystemState.TorrentFileSystemStateBuilder.builder()
                        .setTorrentRemovedInProgress(true)
                        .build();
            case REMOVE_TORRENT_WIND_UP:
                if (isDownloadingOrUploadingOrInProgress ||
                        areWeListeningOrSearchingOrInProgress ||
                        !torrentFileSystemState.isTorrentRemovedInProgress() ||
                        torrentFileSystemState.isTorrentRemovedWindUp())
                    return torrentFileSystemState;
                return TorrentFileSystemState.TorrentFileSystemStateBuilder.builder()
                        .setTorrentRemovedInProgress(false)
                        .setTorrentRemovedWindUp(true)
                        .build();
        }

        return lastState.getTorrentFileSystemState();
    }
}
