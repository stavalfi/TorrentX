package main.torrent.status.reducers;

import main.torrent.status.TorrentStatusAction;
import main.torrent.status.state.tree.DownloadState;
import main.torrent.status.state.tree.TorrentStatusState;

import java.util.function.Supplier;

public class DownloadStateReducer {
    public static Supplier<DownloadState> defaultDownloadStateSupplier = () ->
            DownloadState.DownloadStateBuilder.builder()
                    .setStartDownloadInProgress(false)
                    .setStartDownloadSelfResolved(false)
                    .setStartDownloadWindUp(false)
                    .setResumeDownloadInProgress(false)
                    .setResumeDownloadSelfResolved(false)
                    .setResumeDownloadWindUp(false)
                    .setPauseDownloadInProgress(false)
                    .setPauseDownloadSelfResolved(false)
                    .setPauseDownloadWindUp(true)
                    .setCompletedDownloadingInProgress(false)
                    .setCompletedDownloadingSelfResolved(false)
                    .setCompletedDownloadingWindUp(false)
                    .setStartUploadInProgress(false)
                    .setStartUploadSelfResolved(false)
                    .setStartUploadWindUp(false)
                    .setResumeUploadInProgress(false)
                    .setResumeUploadSelfResolved(false)
                    .setResumeUploadWindUp(false)
                    .setPauseUploadInProgress(false)
                    .setPauseUploadSelfResolved(false)
                    .setPauseUploadWindUp(true)
                    .build();

    public DownloadState reducer(TorrentStatusState lastState, TorrentStatusAction torrentStatusAction) {
        DownloadState downloadState = lastState.getDownloadState();

        boolean isCompletedOrInProgress = downloadState.isCompletedDownloadingInProgress() ||
                downloadState.isCompletedDownloadingSelfResolved() ||
                downloadState.isCompletedDownloadingWindUp();

        boolean isSomethingRemovedOrInRemoveOrInProgress = lastState.getTorrentFileSystemState().isFilesRemovedInProgress() ||
                lastState.getTorrentFileSystemState().isFilesRemovedSelfResolved() ||
                lastState.getTorrentFileSystemState().isFilesRemovedWindUp() ||
                lastState.getTorrentFileSystemState().isTorrentRemovedInProgress() ||
                lastState.getTorrentFileSystemState().isTorrentRemovedSelfResolved() ||
                lastState.getTorrentFileSystemState().isTorrentRemovedWindUp();

        switch (torrentStatusAction) {
            case START_DOWNLOAD_IN_PROGRESS:
                if (isSomethingRemovedOrInRemoveOrInProgress ||
                        isCompletedOrInProgress ||
                        downloadState.isStartDownloadInProgress() ||
                        downloadState.isStartDownloadWindUp())
                    return lastState.getDownloadState();
                return DownloadState.DownloadStateBuilder.builder(lastState.getDownloadState())
                        .setStartDownloadInProgress(true)
                        .build();
            case START_DOWNLOAD_SELF_RESOLVED:
                if (isSomethingRemovedOrInRemoveOrInProgress ||
                        isCompletedOrInProgress ||
                        !downloadState.isStartDownloadInProgress() ||
                        downloadState.isStartDownloadSelfResolved() ||
                        downloadState.isStartDownloadWindUp())
                    return lastState.getDownloadState();
                return DownloadState.DownloadStateBuilder.builder(lastState.getDownloadState())
                        .setStartDownloadSelfResolved(true)
                        .build();
            case START_DOWNLOAD_WIND_UP:
                if (isSomethingRemovedOrInRemoveOrInProgress ||
                        isCompletedOrInProgress ||
                        !downloadState.isStartDownloadInProgress() ||
                        !downloadState.isStartDownloadSelfResolved() ||
                        downloadState.isStartDownloadWindUp())
                    return lastState.getDownloadState();
                return DownloadState.DownloadStateBuilder.builder(lastState.getDownloadState())
                        .setStartDownloadInProgress(false)
                        .setStartDownloadSelfResolved(false)
                        .setStartDownloadWindUp(true)
                        .build();
            case PAUSE_DOWNLOAD_IN_PROGRESS:
                if (!downloadState.isResumeDownloadWindUp() ||
                        !downloadState.isStartDownloadWindUp() ||
                        downloadState.isPauseDownloadInProgress() ||
                        downloadState.isPauseDownloadWindUp())
                    return lastState.getDownloadState();
                return DownloadState.DownloadStateBuilder.builder(lastState.getDownloadState())
                        .setPauseDownloadInProgress(true)
                        .build();
            case PAUSE_DOWNLOAD_SELF_RESOLVED:
                if (!downloadState.isResumeDownloadWindUp() ||
                        !downloadState.isPauseDownloadInProgress() ||
                        downloadState.isPauseDownloadSelfResolved() ||
                        downloadState.isPauseDownloadWindUp())
                    return lastState.getDownloadState();
                return DownloadState.DownloadStateBuilder.builder(lastState.getDownloadState())
                        .setPauseDownloadSelfResolved(true)
                        .build();
            case PAUSE_DOWNLOAD_WIND_UP:
                if (!downloadState.isResumeDownloadWindUp() ||
                        !downloadState.isPauseDownloadInProgress() ||
                        !downloadState.isPauseDownloadSelfResolved() ||
                        downloadState.isPauseDownloadWindUp())
                    return lastState.getDownloadState();
                return DownloadState.DownloadStateBuilder.builder(lastState.getDownloadState())
                        .setPauseDownloadInProgress(false)
                        .setPauseDownloadSelfResolved(false)
                        .setPauseDownloadWindUp(true)
                        .setResumeDownloadWindUp(false)
                        .build();
            case RESUME_DOWNLOAD_IN_PROGRESS:
                if (isSomethingRemovedOrInRemoveOrInProgress ||
                        isCompletedOrInProgress ||
                        !downloadState.isStartDownloadWindUp() ||
                        !downloadState.isPauseDownloadWindUp() ||
                        downloadState.isResumeDownloadInProgress() ||
                        downloadState.isResumeDownloadWindUp())
                    return lastState.getDownloadState();
                return DownloadState.DownloadStateBuilder.builder(lastState.getDownloadState())
                        .setResumeDownloadInProgress(true)
                        .build();
            case RESUME_DOWNLOAD_SELF_RESOLVED:
                if (isSomethingRemovedOrInRemoveOrInProgress ||
                        isCompletedOrInProgress ||
                        !downloadState.isStartDownloadWindUp() ||
                        !downloadState.isPauseDownloadWindUp() ||
                        !downloadState.isResumeDownloadInProgress() ||
                        downloadState.isResumeDownloadSelfResolved() ||
                        downloadState.isResumeDownloadWindUp())
                    return lastState.getDownloadState();
                return DownloadState.DownloadStateBuilder.builder(lastState.getDownloadState())
                        .setResumeDownloadSelfResolved(true)
                        .build();
            case RESUME_DOWNLOAD_WIND_UP:
                if (isSomethingRemovedOrInRemoveOrInProgress ||
                        isCompletedOrInProgress ||
                        !downloadState.isStartDownloadWindUp() ||
                        !downloadState.isPauseDownloadWindUp() ||
                        !downloadState.isResumeDownloadInProgress() ||
                        !downloadState.isResumeDownloadSelfResolved() ||
                        downloadState.isResumeDownloadWindUp())
                    return lastState.getDownloadState();
                return DownloadState.DownloadStateBuilder.builder(lastState.getDownloadState())
                        .setResumeDownloadInProgress(false)
                        .setResumeDownloadSelfResolved(false)
                        .setResumeDownloadWindUp(true)
                        .setPauseDownloadWindUp(false)
                        .build();


            case START_UPLOAD_IN_PROGRESS:
                if (isSomethingRemovedOrInRemoveOrInProgress ||
                        downloadState.isStartUploadInProgress() ||
                        downloadState.isStartUploadWindUp())
                    return lastState.getDownloadState();
                return DownloadState.DownloadStateBuilder.builder(lastState.getDownloadState())
                        .setStartUploadInProgress(true)
                        .build();
            case START_UPLOAD_SELF_RESOLVED:
                if (isSomethingRemovedOrInRemoveOrInProgress ||
                        !downloadState.isStartUploadInProgress() ||
                        downloadState.isStartUploadSelfResolved() ||
                        downloadState.isStartUploadWindUp())
                    return lastState.getDownloadState();
                return DownloadState.DownloadStateBuilder.builder(lastState.getDownloadState())
                        .setStartUploadSelfResolved(true)
                        .build();
            case START_UPLOAD_WIND_UP:
                if (isSomethingRemovedOrInRemoveOrInProgress ||
                        !downloadState.isStartUploadInProgress() ||
                        downloadState.isStartUploadWindUp())
                    return lastState.getDownloadState();
                return DownloadState.DownloadStateBuilder.builder(lastState.getDownloadState())
                        .setStartUploadInProgress(false)
                        .setStartUploadSelfResolved(false)
                        .setStartUploadWindUp(true)
                        .build();
            case PAUSE_UPLOAD_IN_PROGRESS:
                if (!downloadState.isResumeUploadWindUp() ||
                        !downloadState.isStartUploadWindUp() ||
                        downloadState.isPauseUploadInProgress() ||
                        downloadState.isPauseUploadWindUp())
                    return lastState.getDownloadState();
                return DownloadState.DownloadStateBuilder.builder(lastState.getDownloadState())
                        .setPauseUploadInProgress(true)
                        .build();
            case PAUSE_UPLOAD_SELF_RESOLVED:
                if (!downloadState.isResumeUploadWindUp() ||
                        !downloadState.isPauseUploadInProgress() ||
                        downloadState.isPauseUploadSelfResolved() ||
                        downloadState.isPauseUploadWindUp())
                    return lastState.getDownloadState();
                return DownloadState.DownloadStateBuilder.builder(lastState.getDownloadState())
                        .setPauseUploadSelfResolved(true)
                        .build();
            case PAUSE_UPLOAD_WIND_UP:
                if (!downloadState.isResumeUploadWindUp() ||
                        !downloadState.isPauseUploadInProgress() ||
                        !downloadState.isPauseUploadSelfResolved() ||
                        downloadState.isPauseUploadWindUp())
                    return lastState.getDownloadState();
                return DownloadState.DownloadStateBuilder.builder(lastState.getDownloadState())
                        .setPauseUploadInProgress(false)
                        .setPauseUploadSelfResolved(false)
                        .setPauseUploadWindUp(true)
                        .setResumeUploadWindUp(false)
                        .build();
            case RESUME_UPLOAD_IN_PROGRESS:
                if (isSomethingRemovedOrInRemoveOrInProgress ||
                        !downloadState.isStartUploadWindUp() ||
                        !downloadState.isPauseUploadWindUp() ||
                        downloadState.isResumeUploadInProgress() ||
                        downloadState.isResumeUploadWindUp())
                    return lastState.getDownloadState();
                return DownloadState.DownloadStateBuilder.builder(lastState.getDownloadState())
                        .setResumeUploadInProgress(true)
                        .build();
            case RESUME_UPLOAD_SELF_RESOLVED:
                if (isSomethingRemovedOrInRemoveOrInProgress ||
                        !downloadState.isStartUploadWindUp() ||
                        !downloadState.isPauseUploadWindUp() ||
                        !downloadState.isResumeUploadInProgress() ||
                        downloadState.isResumeUploadSelfResolved() ||
                        downloadState.isResumeUploadWindUp())
                    return lastState.getDownloadState();
                return DownloadState.DownloadStateBuilder.builder(lastState.getDownloadState())
                        .setResumeUploadSelfResolved(true)
                        .build();
            case RESUME_UPLOAD_WIND_UP:
                if (isSomethingRemovedOrInRemoveOrInProgress ||
                        !downloadState.isStartUploadWindUp() ||
                        !downloadState.isPauseUploadWindUp() ||
                        !downloadState.isResumeUploadInProgress() ||
                        !downloadState.isResumeUploadSelfResolved() ||
                        downloadState.isResumeUploadWindUp())
                    return lastState.getDownloadState();
                return DownloadState.DownloadStateBuilder.builder(lastState.getDownloadState())
                        .setResumeUploadInProgress(false)
                        .setResumeUploadSelfResolved(false)
                        .setResumeUploadWindUp(true)
                        .setPauseUploadWindUp(false)
                        .build();


            case COMPLETED_DOWNLOADING_IN_PROGRESS:
                if (isSomethingRemovedOrInRemoveOrInProgress ||
                        isCompletedOrInProgress ||
                        !downloadState.isStartDownloadWindUp())
                    return lastState.getDownloadState();
                return DownloadState.DownloadStateBuilder.builder(lastState.getDownloadState())
                        .setCompletedDownloadingInProgress(true)
                        .build();
            case COMPLETED_DOWNLOADING_SELF_RESOLVED:
                if (isSomethingRemovedOrInRemoveOrInProgress ||
                        !downloadState.isCompletedDownloadingInProgress() ||
                        downloadState.isCompletedDownloadingSelfResolved() ||
                        downloadState.isCompletedDownloadingWindUp() ||
                        !downloadState.isStartDownloadWindUp())
                    return lastState.getDownloadState();
                return DownloadState.DownloadStateBuilder.builder(lastState.getDownloadState())
                        .setCompletedDownloadingSelfResolved(true)
                        .build();
            case COMPLETED_DOWNLOADING_WIND_UP:
                if (isSomethingRemovedOrInRemoveOrInProgress ||
                        !downloadState.isCompletedDownloadingInProgress() ||
                        !downloadState.isCompletedDownloadingSelfResolved() ||
                        downloadState.isCompletedDownloadingWindUp() ||
                        !downloadState.isStartDownloadWindUp() ||
                        !downloadState.isPauseDownloadWindUp() ||
                        !lastState.getSearchPeersState().isPauseSearchingPeersWindUp())
                    return lastState.getDownloadState();
                return DownloadState.DownloadStateBuilder.builder(lastState.getDownloadState())
                        .setCompletedDownloadingInProgress(false)
                        .setCompletedDownloadingSelfResolved(false)
                        .setCompletedDownloadingWindUp(true)
                        .build();

            case REMOVE_FILES_WIND_UP:
                if (!lastState.getTorrentFileSystemState().isFilesRemovedInProgress() ||
                        !lastState.getTorrentFileSystemState().isFilesRemovedSelfResolved() ||
                        lastState.getTorrentFileSystemState().isFilesRemovedWindUp() ||
                        !lastState.getDownloadState().isPauseDownloadWindUp() ||
                        !lastState.getDownloadState().isPauseUploadWindUp() ||
                        !lastState.getSearchPeersState().isPauseSearchingPeersWindUp())
                    return lastState.getDownloadState();
                return DownloadState.DownloadStateBuilder.builder(lastState.getDownloadState())
                        .setCompletedDownloadingInProgress(false)
                        .setCompletedDownloadingSelfResolved(false)
                        .setCompletedDownloadingWindUp(false)
                        .setStartDownloadInProgress(false)
                        .setStartDownloadSelfResolved(false)
                        .setStartUploadInProgress(false)
                        .setStartUploadSelfResolved(false)
                        .setResumeDownloadInProgress(false)
                        .setResumeDownloadSelfResolved(false)
                        .setResumeUploadInProgress(false)
                        .setResumeUploadSelfResolved(false)
                        .build();

            case REMOVE_TORRENT_WIND_UP:
                if (!lastState.getTorrentFileSystemState().isTorrentRemovedInProgress() ||
                        !lastState.getTorrentFileSystemState().isTorrentRemovedSelfResolved() ||
                        lastState.getTorrentFileSystemState().isTorrentRemovedWindUp() ||
                        !lastState.getDownloadState().isPauseDownloadWindUp() ||
                        !lastState.getDownloadState().isPauseUploadWindUp() ||
                        !lastState.getSearchPeersState().isPauseSearchingPeersWindUp())
                    return lastState.getDownloadState();
                return DownloadState.DownloadStateBuilder.builder(lastState.getDownloadState())
                        .setCompletedDownloadingInProgress(false)
                        .setCompletedDownloadingSelfResolved(false)
                        .setCompletedDownloadingWindUp(false)
                        .setStartDownloadInProgress(false)
                        .setStartDownloadSelfResolved(false)
                        .setStartUploadInProgress(false)
                        .setStartUploadSelfResolved(false)
                        .setResumeDownloadInProgress(false)
                        .setResumeDownloadSelfResolved(false)
                        .setResumeUploadInProgress(false)
                        .setResumeUploadSelfResolved(false)
                        .build();
        }
        return lastState.getDownloadState();
    }
}
