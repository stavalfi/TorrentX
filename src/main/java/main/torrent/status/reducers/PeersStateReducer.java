package main.torrent.status.reducers;

import main.torrent.status.Action;
import main.torrent.status.state.tree.PeersState;
import main.torrent.status.state.tree.TorrentStatusState;

import java.util.function.Supplier;

public class PeersStateReducer {
    public static Supplier<PeersState> defaultPeersStateSupplier = () ->
            PeersState.PeersStateBuilder.builder()
                    .setStartedSearchingPeersInProgress(false)
                    .setStartedSearchingPeersSelfResolved(false)
                    .setStartedSearchingPeersWindUp(false)
                    .setPauseSearchingPeersInProgress(false)
                    .setPauseSearchingPeersSelfResolved(false)
                    .setPauseSearchingPeersWindUp(true)
                    .setResumeSearchingPeersInProgress(false)
                    .setResumeSearchingPeersSelfResolved(false)
                    .setResumeSearchingPeersWindUp(false)
                    .build();

    public PeersState reducer(TorrentStatusState lastState, Action action) {
        PeersState peersState = lastState.getPeersState();

        boolean isCompletedOrInProgress = lastState.getDownloadState().isCompletedDownloadingInProgress() ||
                lastState.getDownloadState().isCompletedDownloadingWindUp();

        boolean isSomethingRemovedOrInRemoveOrInProgress = lastState.getTorrentFileSystemState().isFilesRemovedInProgress() ||
                lastState.getTorrentFileSystemState().isFilesRemovedWindUp() ||
                lastState.getTorrentFileSystemState().isTorrentRemovedInProgress() ||
                lastState.getTorrentFileSystemState().isTorrentRemovedWindUp();

        switch (action) {
            case START_SEARCHING_PEERS_IN_PROGRESS:
                if (isCompletedOrInProgress ||
                        isSomethingRemovedOrInRemoveOrInProgress ||
                        peersState.isStartedSearchingPeersInProgress() ||
                        peersState.isStartedSearchingPeersWindUp())
                    return lastState.getPeersState();
                return PeersState.PeersStateBuilder.builder(peersState)
                        .setStartedSearchingPeersInProgress(true)
                        .build();
            case START_SEARCHING_PEERS_SELF_RESOLVED:
                if (isSomethingRemovedOrInRemoveOrInProgress ||
                        isCompletedOrInProgress ||
                        !peersState.isStartedSearchingPeersInProgress() ||
                        peersState.isStartedSearchingPeersSelfResolved() ||
                        peersState.isStartedSearchingPeersWindUp())
                    return lastState.getPeersState();
                return PeersState.PeersStateBuilder.builder(peersState)
                        .setStartedSearchingPeersSelfResolved(true)
                        .build();
            case START_SEARCHING_PEERS_WIND_UP:
                if (isCompletedOrInProgress ||
                        isSomethingRemovedOrInRemoveOrInProgress ||
                        !peersState.isStartedSearchingPeersInProgress() ||
                        !peersState.isStartedSearchingPeersSelfResolved() ||
                        peersState.isStartedSearchingPeersWindUp())
                    return lastState.getPeersState();
                return PeersState.PeersStateBuilder.builder(peersState)
                        .setStartedSearchingPeersInProgress(false)
                        .setStartedSearchingPeersSelfResolved(false)
                        .setStartedSearchingPeersWindUp(true)
                        .build();
            case PAUSE_SEARCHING_PEERS_IN_PROGRESS:
                if (!peersState.isStartedSearchingPeersWindUp() ||
                        !peersState.isResumeSearchingPeersWindUp() ||
                        peersState.isPauseSearchingPeersInProgress() ||
                        peersState.isPauseSearchingPeersWindUp())
                    return lastState.getPeersState();
                return PeersState.PeersStateBuilder.builder(peersState)
                        .setPauseSearchingPeersInProgress(true)
                        .build();
            case PAUSE_SEARCHING_PEERS_SELF_RESOLVED:
                if (!peersState.isPauseSearchingPeersInProgress() ||
                        peersState.isPauseSearchingPeersSelfResolved() ||
                        peersState.isPauseSearchingPeersWindUp())
                    return lastState.getPeersState();
                return PeersState.PeersStateBuilder.builder(peersState)
                        .setPauseSearchingPeersSelfResolved(true)
                        .build();
            case PAUSE_SEARCHING_PEERS_WIND_UP:
                if (!peersState.isPauseSearchingPeersInProgress() ||
                        peersState.isPauseSearchingPeersWindUp())
                    return lastState.getPeersState();
                return PeersState.PeersStateBuilder.builder(peersState)
                        .setPauseSearchingPeersInProgress(false)
                        .setPauseSearchingPeersSelfResolved(false)
                        .setPauseSearchingPeersWindUp(true)
                        .setResumeSearchingPeersWindUp(false)
                        .build();
            case RESUME_SEARCHING_PEERS_IN_PROGRESS:
                if (isCompletedOrInProgress ||
                        isSomethingRemovedOrInRemoveOrInProgress ||
                        !peersState.isPauseSearchingPeersWindUp() ||
                        !peersState.isStartedSearchingPeersWindUp() ||
                        peersState.isResumeSearchingPeersInProgress() ||
                        peersState.isResumeSearchingPeersWindUp())
                    return lastState.getPeersState();
                return PeersState.PeersStateBuilder.builder(peersState)
                        .setResumeSearchingPeersInProgress(true)
                        .build();
            case RESUME_SEARCHING_PEERS_SELF_RESOLVED:
                if (isCompletedOrInProgress ||
                        isSomethingRemovedOrInRemoveOrInProgress ||
                        !peersState.isPauseSearchingPeersWindUp() ||
                        !peersState.isResumeSearchingPeersInProgress() ||
                        peersState.isResumeSearchingPeersSelfResolved() ||
                        peersState.isResumeSearchingPeersWindUp())
                    return lastState.getPeersState();
                return PeersState.PeersStateBuilder.builder(peersState)
                        .setResumeSearchingPeersSelfResolved(true)
                        .build();
            case RESUME_SEARCHING_PEERS_WIND_UP:
                if (isCompletedOrInProgress ||
                        isSomethingRemovedOrInRemoveOrInProgress ||
                        !peersState.isPauseSearchingPeersWindUp() ||
                        !peersState.isResumeSearchingPeersInProgress() ||
                        !peersState.isResumeSearchingPeersSelfResolved() ||
                        peersState.isResumeSearchingPeersWindUp())
                    return lastState.getPeersState();
                return PeersState.PeersStateBuilder.builder(peersState)
                        .setResumeSearchingPeersInProgress(false)
                        .setResumeSearchingPeersSelfResolved(false)
                        .setResumeSearchingPeersWindUp(true)
                        .setPauseSearchingPeersWindUp(false)
                        .build();

            case REMOVE_FILES_WIND_UP:
                if (!lastState.getTorrentFileSystemState().isFilesRemovedInProgress() ||
                        !lastState.getTorrentFileSystemState().isFilesRemovedSelfResolved() ||
                        lastState.getTorrentFileSystemState().isFilesRemovedWindUp() ||
                        !lastState.getDownloadState().isPauseDownloadWindUp() ||
                        !lastState.getDownloadState().isPauseUploadWindUp() ||
                        !lastState.getPeersState().isPauseSearchingPeersWindUp())
                    return lastState.getPeersState();
                return PeersState.PeersStateBuilder.builder(peersState)
                        .setStartedSearchingPeersInProgress(false)
                        .setStartedSearchingPeersSelfResolved(false)
                        .setResumeSearchingPeersInProgress(false)
                        .setResumeSearchingPeersSelfResolved(false)
                        .build();

            case REMOVE_TORRENT_WIND_UP:
                if (!lastState.getTorrentFileSystemState().isTorrentRemovedInProgress() ||
                        !lastState.getTorrentFileSystemState().isTorrentRemovedSelfResolved() ||
                        lastState.getTorrentFileSystemState().isTorrentRemovedWindUp() ||
                        !lastState.getDownloadState().isPauseDownloadWindUp() ||
                        !lastState.getDownloadState().isPauseUploadWindUp() ||
                        !lastState.getPeersState().isPauseSearchingPeersWindUp())
                    return lastState.getPeersState();
                return PeersState.PeersStateBuilder.builder(peersState)
                        .setStartedSearchingPeersInProgress(false)
                        .setStartedSearchingPeersSelfResolved(false)
                        .setResumeSearchingPeersInProgress(false)
                        .setResumeSearchingPeersSelfResolved(false)
                        .build();
        }

        return lastState.getPeersState();
    }
}
