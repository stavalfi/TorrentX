package main.torrent.status.reducers;

import main.torrent.status.Action;
import main.torrent.status.state.tree.PeersState;
import main.torrent.status.state.tree.TorrentStatusState;

import java.util.function.Supplier;

public class PeersStateReducer {
    public static Supplier<PeersState> defaultPeersStateSupplier = () ->
            PeersState.PeersStateBuilder.builder()
                    .setStartedListeningToIncomingPeersInProgress(false)
                    .setStartedListeningToIncomingPeersWindUp(false)
                    .setPauseListeningToIncomingPeersInProgress(false)
                    .setPauseListeningToIncomingPeersWindUp(false)
                    .setResumeListeningToIncomingPeersInProgress(false)
                    .setResumeListeningToIncomingPeersWindUp(false)
                    .setStartedSearchingPeersInProgress(false)
                    .setStartedSearchingPeersWindUp(false)
                    .setPauseSearchingPeersInProgress(false)
                    .setPauseSearchingPeersWindUp(false)
                    .setResumeSearchingPeersInProgress(false)
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
                if (peersState.isStartedListeningToIncomingPeersWindUp() ||
                        peersState.isPauseSearchingPeersInProgress() ||
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


            case START_LISTENING_TO_INCOMING_PEERS_IN_PROGRESS:
                if (isCompletedOrInProgress ||
                        isSomethingRemovedOrInRemoveOrInProgress ||
                        peersState.isStartedListeningToIncomingPeersInProgress() ||
                        peersState.isStartedListeningToIncomingPeersWindUp())
                    return peersState;
                return PeersState.PeersStateBuilder.builder(peersState)
                        .setStartedListeningToIncomingPeersInProgress(true)
                        .build();
            case START_LISTENING_TO_INCOMING_PEERS_WIND_UP:
                if (isCompletedOrInProgress ||
                        isSomethingRemovedOrInRemoveOrInProgress ||
                        !peersState.isStartedListeningToIncomingPeersInProgress() ||
                        peersState.isStartedListeningToIncomingPeersWindUp())
                    return peersState;
                return PeersState.PeersStateBuilder.builder(peersState)
                        .setStartedListeningToIncomingPeersInProgress(false)
                        .setStartedListeningToIncomingPeersWindUp(true)
                        .build();
            case PAUSE_LISTENING_TO_INCOMING_PEERS_IN_PROGRESS:
                if (peersState.isStartedListeningToIncomingPeersWindUp() ||
                        peersState.isPauseListeningToIncomingPeersInProgress() ||
                        peersState.isPauseListeningToIncomingPeersWindUp())
                    return peersState;
                return PeersState.PeersStateBuilder.builder(peersState)
                        .setPauseListeningToIncomingPeersInProgress(true)
                        .setResumeListeningToIncomingPeersInProgress(false)
                        .build();
            case PAUSE_LISTENING_TO_INCOMING_PEERS_WIND_UP:
                if (!peersState.isPauseListeningToIncomingPeersInProgress() ||
                        peersState.isPauseListeningToIncomingPeersWindUp())
                    return peersState;
                return PeersState.PeersStateBuilder.builder(peersState)
                        .setPauseListeningToIncomingPeersInProgress(false)
                        .setPauseListeningToIncomingPeersWindUp(true)
                        .setResumeListeningToIncomingPeersWindUp(false)
                        .build();
            case RESUME_LISTENING_TO_INCOMING_PEERS_IN_PROGRESS:
                if (isCompletedOrInProgress ||
                        isSomethingRemovedOrInRemoveOrInProgress ||
                        peersState.isPauseListeningToIncomingPeersInProgress() ||
                        !peersState.isStartedListeningToIncomingPeersWindUp() ||
                        peersState.isResumeListeningToIncomingPeersInProgress() ||
                        peersState.isResumeListeningToIncomingPeersWindUp())
                    return peersState;
                return PeersState.PeersStateBuilder.builder(peersState)
                        .setResumeListeningToIncomingPeersInProgress(true)
                        .build();
            case RESUME_LISTENING_TO_INCOMING_PEERS_WIND_UP:
                if (isCompletedOrInProgress ||
                        isSomethingRemovedOrInRemoveOrInProgress ||
                        !peersState.isResumeListeningToIncomingPeersInProgress() ||
                        peersState.isResumeListeningToIncomingPeersWindUp())
                    return peersState;
                return PeersState.PeersStateBuilder.builder(peersState)
                        .setResumeListeningToIncomingPeersInProgress(false)
                        .setResumeListeningToIncomingPeersWindUp(true)
                        .setPauseListeningToIncomingPeersWindUp(false)
                        .build();
        }

        return lastState.getPeersState();
    }
}
