package main.torrent.status.reducers;

import main.torrent.status.Action;
import main.torrent.status.state.tree.DownloadState;
import main.torrent.status.state.tree.TorrentStatusState;

import java.util.function.Supplier;

public class DownloadStateReducer {
    public static Supplier<DownloadState> defaultDownloadStateSupplier = () ->
            DownloadState.DownloadStateBuilder.builder()
                    .setStartDownloadInProgress(false)
                    .setStartDownloadWindUp(false)
                    .setResumeDownloadInProgress(false)
                    .setResumeDownloadWindUp(false)
                    .setPauseDownloadInProgress(false)
                    .setPauseDownloadWindUp(true)
                    .setCompletedDownloadingInProgress(false)
                    .setCompletedDownloadingWindUp(false)
                    .setStartUploadInProgress(false)
                    .setStartUploadWindUp(false)
                    .setResumeUploadInProgress(false)
                    .setResumeUploadWindUp(false)
                    .setPauseUploadInProgress(false)
                    .setPauseUploadWindUp(true)
                    .build();

    public DownloadState reducer(TorrentStatusState lastState, Action action) {
        DownloadState downloadState = lastState.getDownloadState();

        boolean isCompletedOrInProgress = downloadState.isCompletedDownloadingInProgress() ||
                downloadState.isCompletedDownloadingWindUp();

        boolean isSomethingRemovedOrInRemoveOrInProgress = lastState.getTorrentFileSystemState().isFilesRemovedInProgress() ||
                lastState.getTorrentFileSystemState().isFilesRemovedWindUp() ||
                lastState.getTorrentFileSystemState().isTorrentRemovedInProgress() ||
                lastState.getTorrentFileSystemState().isTorrentRemovedWindUp();

        switch (action) {
            case INITIALIZE:
                return lastState.getDownloadState();
            default:
                return lastState.getDownloadState();
            case START_DOWNLOAD_IN_PROGRESS:
                if (isSomethingRemovedOrInRemoveOrInProgress ||
                        isCompletedOrInProgress ||
                        downloadState.isStartDownloadInProgress() ||
                        downloadState.isStartDownloadWindUp())
                    return lastState.getDownloadState();
                return DownloadState.DownloadStateBuilder.builder(lastState.getDownloadState())
                        .setStartDownloadInProgress(true)
                        .build();
            case START_DOWNLOAD_WIND_UP:
                if (isSomethingRemovedOrInRemoveOrInProgress ||
                        isCompletedOrInProgress ||
                        !downloadState.isStartDownloadInProgress() ||
                        downloadState.isStartDownloadWindUp())
                    return lastState.getDownloadState();
                return DownloadState.DownloadStateBuilder.builder(lastState.getDownloadState())
                        .setStartDownloadInProgress(false)
                        .setStartDownloadWindUp(true)
                        .build();
            case PAUSE_DOWNLOAD_IN_PROGRESS:
                if (!downloadState.isStartDownloadWindUp() ||
                        downloadState.isPauseDownloadInProgress() ||
                        downloadState.isPauseDownloadWindUp())
                    return lastState.getDownloadState();
                return DownloadState.DownloadStateBuilder.builder(lastState.getDownloadState())
                        .setPauseDownloadInProgress(true)
                        .setResumeDownloadInProgress(false)
                        .build();
            case PAUSE_DOWNLOAD_WIND_UP:
                if (!downloadState.isPauseDownloadInProgress() ||
                        downloadState.isPauseDownloadWindUp())
                    return lastState.getDownloadState();
                return DownloadState.DownloadStateBuilder.builder(lastState.getDownloadState())
                        .setPauseDownloadInProgress(false)
                        .setPauseDownloadWindUp(true)
                        .setResumeDownloadWindUp(false)
                        .build();
            case RESUME_DOWNLOAD_IN_PROGRESS:
                if (isSomethingRemovedOrInRemoveOrInProgress ||
                        isCompletedOrInProgress ||
                        !downloadState.isStartDownloadWindUp() ||
                        downloadState.isPauseDownloadInProgress() ||
                        downloadState.isResumeDownloadInProgress() ||
                        downloadState.isResumeDownloadWindUp())
                    return lastState.getDownloadState();
                return DownloadState.DownloadStateBuilder.builder(lastState.getDownloadState())
                        .setResumeDownloadInProgress(true)
                        .build();
            case RESUME_DOWNLOAD_WIND_UP:
                if (isSomethingRemovedOrInRemoveOrInProgress ||
                        isCompletedOrInProgress ||
                        !downloadState.isStartDownloadWindUp() ||
                        downloadState.isPauseDownloadInProgress() ||
                        downloadState.isResumeDownloadInProgress() ||
                        downloadState.isResumeDownloadWindUp())
                    return lastState.getDownloadState();
                return DownloadState.DownloadStateBuilder.builder(lastState.getDownloadState())
                        .setResumeDownloadInProgress(false)
                        .setResumeDownloadWindUp(true)
                        .setPauseDownloadWindUp(true)
                        .build();


            case START_UPLOAD_IN_PROGRESS:
                if (isSomethingRemovedOrInRemoveOrInProgress ||
                        downloadState.isStartUploadInProgress() ||
                        downloadState.isStartUploadWindUp())
                    return lastState.getDownloadState();
                return DownloadState.DownloadStateBuilder.builder(lastState.getDownloadState())
                        .setStartUploadInProgress(true)
                        .build();
            case START_UPLOAD_WIND_UP:
                if (isSomethingRemovedOrInRemoveOrInProgress ||
                        !downloadState.isStartUploadInProgress() ||
                        downloadState.isStartUploadWindUp())
                    return lastState.getDownloadState();
                return DownloadState.DownloadStateBuilder.builder(lastState.getDownloadState())
                        .setStartUploadInProgress(false)
                        .setStartUploadWindUp(true)
                        .build();
            case PAUSE_UPLOAD_IN_PROGRESS:
                if (!downloadState.isStartUploadWindUp() ||
                        downloadState.isPauseUploadInProgress() ||
                        downloadState.isPauseUploadWindUp())
                    return lastState.getDownloadState();
                return DownloadState.DownloadStateBuilder.builder(lastState.getDownloadState())
                        .setPauseUploadInProgress(true)
                        .setResumeUploadInProgress(false)
                        .build();
            case PAUSE_UPLOAD_WIND_UP:
                if (!downloadState.isPauseUploadInProgress() ||
                        downloadState.isPauseUploadWindUp())
                    return lastState.getDownloadState();
                return DownloadState.DownloadStateBuilder.builder(lastState.getDownloadState())
                        .setPauseUploadInProgress(false)
                        .setPauseUploadWindUp(true)
                        .setResumeUploadWindUp(false)
                        .build();
            case RESUME_UPLOAD_IN_PROGRESS:
                if (isSomethingRemovedOrInRemoveOrInProgress ||
                        !downloadState.isStartUploadWindUp() ||
                        downloadState.isPauseUploadInProgress() ||
                        downloadState.isResumeUploadInProgress() ||
                        downloadState.isResumeUploadWindUp())
                    return lastState.getDownloadState();
                return DownloadState.DownloadStateBuilder.builder(lastState.getDownloadState())
                        .setResumeUploadInProgress(true)
                        .build();
            case RESUME_UPLOAD_WIND_UP:
                if (isSomethingRemovedOrInRemoveOrInProgress ||
                        !downloadState.isStartUploadWindUp() ||
                        downloadState.isPauseUploadInProgress() ||
                        downloadState.isResumeUploadInProgress() ||
                        downloadState.isResumeUploadWindUp())
                    return lastState.getDownloadState();
                return DownloadState.DownloadStateBuilder.builder(lastState.getDownloadState())
                        .setResumeUploadInProgress(false)
                        .setResumeUploadWindUp(true)
                        .setPauseUploadWindUp(true)
                        .build();


            case COMPLETED_DOWNLOADING_IN_PROGRESS:
                if (isSomethingRemovedOrInRemoveOrInProgress ||
                        isCompletedOrInProgress ||
                        !downloadState.isStartDownloadWindUp())
                    return lastState.getDownloadState();
                return DownloadState.DownloadStateBuilder.builder(lastState.getDownloadState())
                        .setCompletedDownloadingInProgress(true)
                        .build();
            case COMPLETED_DOWNLOADING_WIND_UP:
                if (isSomethingRemovedOrInRemoveOrInProgress ||
                        !downloadState.isCompletedDownloadingInProgress() ||
                        downloadState.isCompletedDownloadingWindUp() ||
                        !downloadState.isStartDownloadWindUp() ||
                        !downloadState.isPauseDownloadWindUp())
                    return lastState.getDownloadState();
                return DownloadState.DownloadStateBuilder.builder(lastState.getDownloadState())
                        .setCompletedDownloadingInProgress(false)
                        .setCompletedDownloadingWindUp(true)
                        .build();
        }
    }
}
