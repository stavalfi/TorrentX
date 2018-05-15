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

        boolean isSomethingRemovedOrInRemoveOrInProgress = lastState.getTorrentFileSystemState().isFilesRemovedInProgress() ||
                lastState.getTorrentFileSystemState().isFilesRemovedWindUp() ||
                lastState.getTorrentFileSystemState().isTorrentRemovedInProgress() ||
                lastState.getTorrentFileSystemState().isTorrentRemovedWindUp();

        switch (action) {
            case START_LISTENING_TO_INCOMING_PEERS_IN_PROGRESS:
                if (peersState.isStartedListeningToIncomingPeersInProgress() ||
                        peersState.isStartedListeningToIncomingPeersWindUp())
                    return peersState;
                return PeersState.PeersStateBuilder.builder()
                        .setStartedListeningToIncomingPeersInProgress(true)
                        .build();
            case START_LISTENING_TO_INCOMING_PEERS_WIND_UP:
                if (isSomethingRemovedOrInRemoveOrInProgress ||
                        !peersState.isStartedListeningToIncomingPeersInProgress() ||
                        peersState.isStartedListeningToIncomingPeersWindUp())
                    return peersState;
                return PeersState.PeersStateBuilder.builder()
                        .setStartedListeningToIncomingPeersInProgress(false)
                        .setStartedListeningToIncomingPeersWindUp(true)
                        .build();
            case PAUSE_LISTENING_TO_INCOMING_PEERS_IN_PROGRESS:
                if (peersState.isPauseListeningToIncomingPeersInProgress() ||
                        peersState.isPauseListeningToIncomingPeersWindUp())
                    return peersState;
                return PeersState.PeersStateBuilder.builder()
                        .setPauseListeningToIncomingPeersInProgress(true)
                        .build();
            case PAUSE_LISTENING_TO_INCOMING_PEERS_WIND_UP:
                if (isSomethingRemovedOrInRemoveOrInProgress ||
                        !peersState.isPauseListeningToIncomingPeersInProgress() ||
                        peersState.isPauseListeningToIncomingPeersWindUp())
                    return peersState;
                return PeersState.PeersStateBuilder.builder()
                        .setPauseListeningToIncomingPeersInProgress(false)
                        .setPauseListeningToIncomingPeersWindUp(true)
                        .build();
            case RESUME_LISTENING_TO_INCOMING_PEERS_IN_PROGRESS:
                if (peersState.isResumeListeningToIncomingPeersInProgress() ||
                        peersState.isResumeListeningToIncomingPeersWindUp())
                    return peersState;
                return PeersState.PeersStateBuilder.builder()
                        .setResumeListeningToIncomingPeersInProgress(true)
                        .build();
            case RESUME_LISTENING_TO_INCOMING_PEERS_WIND_UP:
                if (isSomethingRemovedOrInRemoveOrInProgress ||
                        !peersState.isResumeListeningToIncomingPeersInProgress() ||
                        peersState.isResumeListeningToIncomingPeersWindUp())
                    return peersState;
                return PeersState.PeersStateBuilder.builder()
                        .setResumeListeningToIncomingPeersInProgress(false)
                        .setResumeListeningToIncomingPeersWindUp(true)
                        .build();
            //....

            case START_SEARCHING_PEERS_IN_PROGRESS:
                if (peersState.isStartedSearchingPeersInProgress() ||
                        peersState.isStartedSearchingPeersWindUp())
                    return peersState;
                return PeersState.PeersStateBuilder.builder()
                        .setStartedSearchingPeersInProgress(true)
                        .build();
            case START_SEARCHING_PEERS_WIND_UP:
                if (isSomethingRemovedOrInRemoveOrInProgress ||
                        !peersState.isStartedSearchingPeersInProgress() ||
                        peersState.isStartedSearchingPeersWindUp())
                    return peersState;
                return PeersState.PeersStateBuilder.builder()
                        .setStartedSearchingPeersInProgress(false)
                        .setStartedSearchingPeersWindUp(true)
                        .build();
            case PAUSE_SEARCHING_PEERS_IN_PROGRESS:
                if (peersState.isPauseSearchingPeersInProgress() ||
                        peersState.isPauseSearchingPeersWindUp())
                    return peersState;
                return PeersState.PeersStateBuilder.builder()
                        .setPauseSearchingPeersInProgress(true)
                        .build();
            case PAUSE_SEARCHING_PEERS_WIND_UP:
                if (isSomethingRemovedOrInRemoveOrInProgress ||
                        !peersState.isPauseSearchingPeersInProgress() ||
                        peersState.isPauseSearchingPeersWindUp())
                    return peersState;
                return PeersState.PeersStateBuilder.builder()
                        .setPauseSearchingPeersInProgress(false)
                        .setPauseSearchingPeersWindUp(true)
                        .build();
            case RESUME_SEARCHING_PEERS_IN_PROGRESS:
                if (peersState.isResumeSearchingPeersInProgress() ||
                        peersState.isResumeSearchingPeersWindUp())
                    return peersState;
                return PeersState.PeersStateBuilder.builder()
                        .setResumeSearchingPeersInProgress(true)
                        .build();
            case RESUME_SEARCHING_PEERS_WIND_UP:
                if (isSomethingRemovedOrInRemoveOrInProgress ||
                        !peersState.isResumeSearchingPeersInProgress() ||
                        peersState.isResumeSearchingPeersWindUp())
                    return peersState;
                return PeersState.PeersStateBuilder.builder()
                        .setResumeSearchingPeersInProgress(false)
                        .setResumeSearchingPeersWindUp(true)
                        .build();
        }

        return lastState.getPeersState();
    }
}
