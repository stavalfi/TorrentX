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
                    return peersState;
                return PeersState.PeersStateBuilder.builder(peersState)
                        .setStartedSearchingPeersInProgress(true)
                        .build();
            case START_SEARCHING_PEERS_WIND_UP:
                if (isCompletedOrInProgress ||
                        isSomethingRemovedOrInRemoveOrInProgress ||
                        !peersState.isStartedSearchingPeersInProgress() ||
                        peersState.isStartedSearchingPeersWindUp())
                    return peersState;
                return PeersState.PeersStateBuilder.builder(peersState)
                        .setStartedSearchingPeersInProgress(false)
                        .setStartedSearchingPeersWindUp(true)
                        .build();
            case PAUSE_SEARCHING_PEERS_IN_PROGRESS:
                if (peersState.isPauseSearchingPeersInProgress() ||
                        peersState.isPauseSearchingPeersWindUp())
                    return peersState;
                return PeersState.PeersStateBuilder.builder(peersState)
                        .setPauseSearchingPeersInProgress(true)
                        .setResumeSearchingPeersInProgress(false)
                        .build();
            case PAUSE_SEARCHING_PEERS_WIND_UP:
                if (!peersState.isPauseSearchingPeersInProgress() ||
                        peersState.isPauseSearchingPeersWindUp())
                    return peersState;
                return PeersState.PeersStateBuilder.builder(peersState)
                        .setPauseSearchingPeersInProgress(false)
                        .setPauseSearchingPeersWindUp(true)
                        .setResumeSearchingPeersWindUp(false)
                        .build();
            case RESUME_SEARCHING_PEERS_IN_PROGRESS:
                if (isCompletedOrInProgress ||
                        isSomethingRemovedOrInRemoveOrInProgress ||
                        peersState.isPauseSearchingPeersInProgress() ||
                        !peersState.isStartedSearchingPeersWindUp() ||
                        peersState.isResumeSearchingPeersInProgress() ||
                        peersState.isResumeSearchingPeersWindUp())
                    return peersState;
                return PeersState.PeersStateBuilder.builder(peersState)
                        .setResumeSearchingPeersInProgress(true)
                        .build();
            case RESUME_SEARCHING_PEERS_WIND_UP:
                if (isCompletedOrInProgress ||
                        isSomethingRemovedOrInRemoveOrInProgress ||
                        !peersState.isResumeSearchingPeersInProgress() ||
                        peersState.isResumeSearchingPeersWindUp())
                    return peersState;
                return PeersState.PeersStateBuilder.builder(peersState)
                        .setResumeSearchingPeersInProgress(false)
                        .setResumeSearchingPeersWindUp(true)
                        .setPauseSearchingPeersWindUp(false)
                        .build();
        }

        return lastState.getPeersState();
    }
}
