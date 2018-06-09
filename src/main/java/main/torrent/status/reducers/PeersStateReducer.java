package main.torrent.status.reducers;

import main.torrent.status.TorrentStatusAction;
import main.torrent.status.state.tree.SearchPeersState;
import main.torrent.status.state.tree.TorrentStatusState;

import java.util.function.Supplier;

public class PeersStateReducer {
    public static Supplier<SearchPeersState> defaultPeersStateSupplier = () ->
            SearchPeersState.PeersStateBuilder.builder()
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

    public SearchPeersState reducer(TorrentStatusState lastState, TorrentStatusAction torrentStatusAction) {
        SearchPeersState searchPeersState = lastState.getSearchPeersState();

        boolean isCompletedOrInProgress = lastState.getDownloadState().isCompletedDownloadingInProgress() ||
                lastState.getDownloadState().isCompletedDownloadingWindUp();

        boolean isSomethingRemovedOrInRemoveOrInProgress = lastState.getTorrentFileSystemState().isFilesRemovedInProgress() ||
                lastState.getTorrentFileSystemState().isFilesRemovedWindUp() ||
                lastState.getTorrentFileSystemState().isTorrentRemovedInProgress() ||
                lastState.getTorrentFileSystemState().isTorrentRemovedWindUp();

        switch (torrentStatusAction) {
            case START_SEARCHING_PEERS_IN_PROGRESS:
                if (isCompletedOrInProgress ||
                        isSomethingRemovedOrInRemoveOrInProgress ||
                        searchPeersState.isStartedSearchingPeersInProgress() ||
                        searchPeersState.isStartedSearchingPeersWindUp())
                    return lastState.getSearchPeersState();
                return SearchPeersState.PeersStateBuilder.builder(searchPeersState)
                        .setStartedSearchingPeersInProgress(true)
                        .build();
            case START_SEARCHING_PEERS_SELF_RESOLVED:
                if (isSomethingRemovedOrInRemoveOrInProgress ||
                        isCompletedOrInProgress ||
                        !searchPeersState.isStartedSearchingPeersInProgress() ||
                        searchPeersState.isStartedSearchingPeersSelfResolved() ||
                        searchPeersState.isStartedSearchingPeersWindUp())
                    return lastState.getSearchPeersState();
                return SearchPeersState.PeersStateBuilder.builder(searchPeersState)
                        .setStartedSearchingPeersSelfResolved(true)
                        .build();
            case START_SEARCHING_PEERS_WIND_UP:
                if (isCompletedOrInProgress ||
                        isSomethingRemovedOrInRemoveOrInProgress ||
                        !searchPeersState.isStartedSearchingPeersInProgress() ||
                        !searchPeersState.isStartedSearchingPeersSelfResolved() ||
                        searchPeersState.isStartedSearchingPeersWindUp())
                    return lastState.getSearchPeersState();
                return SearchPeersState.PeersStateBuilder.builder(searchPeersState)
                        .setStartedSearchingPeersInProgress(false)
                        .setStartedSearchingPeersSelfResolved(false)
                        .setStartedSearchingPeersWindUp(true)
                        .build();
            case PAUSE_SEARCHING_PEERS_IN_PROGRESS:
                if (!searchPeersState.isStartedSearchingPeersWindUp() ||
                        !searchPeersState.isResumeSearchingPeersWindUp() ||
                        searchPeersState.isPauseSearchingPeersInProgress() ||
                        searchPeersState.isPauseSearchingPeersWindUp())
                    return lastState.getSearchPeersState();
                return SearchPeersState.PeersStateBuilder.builder(searchPeersState)
                        .setPauseSearchingPeersInProgress(true)
                        .build();
            case PAUSE_SEARCHING_PEERS_SELF_RESOLVED:
                if (!searchPeersState.isPauseSearchingPeersInProgress() ||
                        searchPeersState.isPauseSearchingPeersSelfResolved() ||
                        searchPeersState.isPauseSearchingPeersWindUp())
                    return lastState.getSearchPeersState();
                return SearchPeersState.PeersStateBuilder.builder(searchPeersState)
                        .setPauseSearchingPeersSelfResolved(true)
                        .build();
            case PAUSE_SEARCHING_PEERS_WIND_UP:
                if (!searchPeersState.isPauseSearchingPeersInProgress() ||
                        searchPeersState.isPauseSearchingPeersWindUp())
                    return lastState.getSearchPeersState();
                return SearchPeersState.PeersStateBuilder.builder(searchPeersState)
                        .setPauseSearchingPeersInProgress(false)
                        .setPauseSearchingPeersSelfResolved(false)
                        .setPauseSearchingPeersWindUp(true)
                        .setResumeSearchingPeersWindUp(false)
                        .build();
            case RESUME_SEARCHING_PEERS_IN_PROGRESS:
                if (isCompletedOrInProgress ||
                        isSomethingRemovedOrInRemoveOrInProgress ||
                        !searchPeersState.isPauseSearchingPeersWindUp() ||
                        !searchPeersState.isStartedSearchingPeersWindUp() ||
                        searchPeersState.isResumeSearchingPeersInProgress() ||
                        searchPeersState.isResumeSearchingPeersWindUp())
                    return lastState.getSearchPeersState();
                return SearchPeersState.PeersStateBuilder.builder(searchPeersState)
                        .setResumeSearchingPeersInProgress(true)
                        .build();
            case RESUME_SEARCHING_PEERS_SELF_RESOLVED:
                if (isCompletedOrInProgress ||
                        isSomethingRemovedOrInRemoveOrInProgress ||
                        !searchPeersState.isPauseSearchingPeersWindUp() ||
                        !searchPeersState.isResumeSearchingPeersInProgress() ||
                        searchPeersState.isResumeSearchingPeersSelfResolved() ||
                        searchPeersState.isResumeSearchingPeersWindUp())
                    return lastState.getSearchPeersState();
                return SearchPeersState.PeersStateBuilder.builder(searchPeersState)
                        .setResumeSearchingPeersSelfResolved(true)
                        .build();
            case RESUME_SEARCHING_PEERS_WIND_UP:
                if (isCompletedOrInProgress ||
                        isSomethingRemovedOrInRemoveOrInProgress ||
                        !searchPeersState.isPauseSearchingPeersWindUp() ||
                        !searchPeersState.isResumeSearchingPeersInProgress() ||
                        !searchPeersState.isResumeSearchingPeersSelfResolved() ||
                        searchPeersState.isResumeSearchingPeersWindUp())
                    return lastState.getSearchPeersState();
                return SearchPeersState.PeersStateBuilder.builder(searchPeersState)
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
                        !lastState.getSearchPeersState().isPauseSearchingPeersWindUp())
                    return lastState.getSearchPeersState();
                return SearchPeersState.PeersStateBuilder.builder(searchPeersState)
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
                        !lastState.getSearchPeersState().isPauseSearchingPeersWindUp())
                    return lastState.getSearchPeersState();
                return SearchPeersState.PeersStateBuilder.builder(searchPeersState)
                        .setStartedSearchingPeersInProgress(false)
                        .setStartedSearchingPeersSelfResolved(false)
                        .setResumeSearchingPeersInProgress(false)
                        .setResumeSearchingPeersSelfResolved(false)
                        .build();
        }

        return lastState.getSearchPeersState();
    }
}
