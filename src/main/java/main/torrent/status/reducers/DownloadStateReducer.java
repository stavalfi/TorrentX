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

        boolean isSomethingRemovedOrInRemove = lastState.getTorrentFileSystemState().isFilesRemovedInProgress() ||
                lastState.getTorrentFileSystemState().isFilesRemovedWindUp() ||
                lastState.getTorrentFileSystemState().isTorrentRemovedInProgress() ||
                lastState.getTorrentFileSystemState().isTorrentRemovedWindUp();

        switch (action) {
            case INITIALIZE:
                return lastState.getDownloadState();
            default:
                return lastState.getDownloadState();
            case START_DOWNLOAD_IN_PROGRESS:
                if (isSomethingRemovedOrInRemove ||
                        downloadState.isCompletedDownloadingInProgress() ||
                        downloadState.isCompletedDownloadingWindUp() ||
                        downloadState.isStartDownloadInProgress() ||
                        downloadState.isStartDownloadWindUp())
                    return lastState.getDownloadState();
                return DownloadState.DownloadStateBuilder.builder(lastState.getDownloadState())
                        .setStartDownloadInProgress(true)
                        .setStartDownloadWindUp(false)
                        .setResumeDownloadInProgress(false)
                        .setPauseDownloadInProgress(false)
                        .setCompletedDownloadingInProgress(false)
                        .setCompletedDownloadingWindUp(false)
                        .build();
            case START_DOWNLOAD_WIND_UP:
                if (isSomethingRemovedOrInRemove ||
                        downloadState.isCompletedDownloadingInProgress() ||
                        downloadState.isCompletedDownloadingWindUp() ||
                        !downloadState.isStartDownloadInProgress() ||
                        downloadState.isStartDownloadWindUp())
                    return lastState.getDownloadState();
                return DownloadState.DownloadStateBuilder.builder(lastState.getDownloadState())
                        .setStartDownloadInProgress(false)
                        .setStartDownloadWindUp(true)
                        .setResumeDownloadInProgress(false)
                        .setPauseDownloadInProgress(false)
                        .setCompletedDownloadingInProgress(false)
                        .setCompletedDownloadingWindUp(false)
                        .build();
            case PAUSE_DOWNLOAD_IN_PROGRESS:
                if (isSomethingRemovedOrInRemove ||
                        downloadState.isCompletedDownloadingInProgress() ||
                        downloadState.isCompletedDownloadingWindUp() ||
                        downloadState.isStartDownloadInProgress() ||
                        !downloadState.isStartDownloadWindUp() ||
                        downloadState.isPauseDownloadInProgress() ||
                        downloadState.isPauseDownloadWindUp())
                    return lastState.getDownloadState();
                return DownloadState.DownloadStateBuilder.builder(lastState.getDownloadState())
                        .setStartDownloadInProgress(false)
                        .setStartDownloadWindUp(true)
                        .setResumeDownloadInProgress(false)
                        .setResumeDownloadWindUp(false)
                        .setPauseDownloadInProgress(true)
                        .setPauseDownloadWindUp(false)
                        .setCompletedDownloadingInProgress(false)
                        .setCompletedDownloadingWindUp(false)
                        .build();
            case PAUSE_DOWNLOAD_WIND_UP:
                if (isSomethingRemovedOrInRemove ||
                        downloadState.isCompletedDownloadingInProgress() ||
                        downloadState.isCompletedDownloadingWindUp() ||
                        downloadState.isStartDownloadInProgress() ||
                        !downloadState.isStartDownloadWindUp() ||
                        !downloadState.isPauseDownloadInProgress() ||
                        downloadState.isPauseDownloadWindUp())
                    return lastState.getDownloadState();
                return DownloadState.DownloadStateBuilder.builder(lastState.getDownloadState())
                        .setStartDownloadInProgress(false)
                        .setStartDownloadWindUp(true)
                        .setResumeDownloadInProgress(false)
                        .setResumeDownloadWindUp(false)
                        .setPauseDownloadInProgress(false)
                        .setPauseDownloadWindUp(true)
                        .setCompletedDownloadingInProgress(false)
                        .setCompletedDownloadingWindUp(false)
                        .build();
            case RESUME_DOWNLOAD_IN_PROGRESS:
                if (isSomethingRemovedOrInRemove ||
                        downloadState.isCompletedDownloadingInProgress() ||
                        downloadState.isCompletedDownloadingWindUp() ||
                        downloadState.isStartDownloadInProgress() ||
                        !downloadState.isStartDownloadWindUp() ||
                        downloadState.isResumeDownloadInProgress() ||
                        downloadState.isResumeDownloadWindUp())
                    return lastState.getDownloadState();
                return DownloadState.DownloadStateBuilder.builder(lastState.getDownloadState())
                        .setStartDownloadInProgress(false)
                        .setStartDownloadWindUp(true)
                        .setResumeDownloadInProgress(true)
                        .setResumeDownloadWindUp(false)
                        .setPauseDownloadInProgress(false)
                        .setPauseDownloadWindUp(false)
                        .setCompletedDownloadingInProgress(false)
                        .setCompletedDownloadingWindUp(false)
                        .build();
            case RESUME_DOWNLOAD_WIND_UP:
                if (isSomethingRemovedOrInRemove ||
                        downloadState.isCompletedDownloadingInProgress() ||
                        downloadState.isCompletedDownloadingWindUp() ||
                        downloadState.isStartDownloadInProgress() ||
                        !downloadState.isStartDownloadWindUp() ||
                        !downloadState.isResumeDownloadInProgress() ||
                        downloadState.isResumeDownloadWindUp())
                    return lastState.getDownloadState();
                return DownloadState.DownloadStateBuilder.builder(lastState.getDownloadState())
                        .setStartDownloadInProgress(false)
                        .setStartDownloadWindUp(true)
                        .setResumeDownloadInProgress(false)
                        .setResumeDownloadWindUp(true)
                        .setPauseDownloadInProgress(false)
                        .setPauseDownloadWindUp(false)
                        .setCompletedDownloadingInProgress(false)
                        .setCompletedDownloadingWindUp(false)
                        .build();


            case START_UPLOAD_IN_PROGRESS:
                if (isSomethingRemovedOrInRemove ||
                        downloadState.isStartUploadInProgress() ||
                        downloadState.isStartUploadWindUp())
                    return lastState.getDownloadState();
                return DownloadState.DownloadStateBuilder.builder(lastState.getDownloadState())
                        .setStartUploadInProgress(true)
                        .setStartUploadWindUp(false)
                        .setResumeUploadInProgress(false)
                        .setPauseUploadInProgress(false)
                        .build();
            case START_UPLOAD_WIND_UP:
                if (isSomethingRemovedOrInRemove ||
                        !downloadState.isStartUploadInProgress() ||
                        downloadState.isStartUploadWindUp())
                    return lastState.getDownloadState();
                return DownloadState.DownloadStateBuilder.builder(lastState.getDownloadState())
                        .setStartUploadInProgress(false)
                        .setStartUploadWindUp(true)
                        .setResumeUploadInProgress(false)
                        .setPauseUploadInProgress(false)
                        .build();
            case PAUSE_UPLOAD_IN_PROGRESS:
                if (isSomethingRemovedOrInRemove ||
                        downloadState.isStartUploadInProgress() ||
                        !downloadState.isStartUploadWindUp() ||
                        downloadState.isPauseUploadInProgress() ||
                        downloadState.isPauseUploadWindUp())
                    return lastState.getDownloadState();
                return DownloadState.DownloadStateBuilder.builder(lastState.getDownloadState())
                        .setStartUploadInProgress(false)
                        .setStartUploadWindUp(true)
                        .setPauseUploadInProgress(true)
                        .setPauseUploadWindUp(false)
                        .setResumeUploadInProgress(false)
                        .setResumeUploadWindUp(false)
                        .build();
            case PAUSE_UPLOAD_WIND_UP:
                if (isSomethingRemovedOrInRemove ||
                        downloadState.isStartUploadInProgress() ||
                        !downloadState.isStartUploadWindUp() ||
                        !downloadState.isPauseUploadInProgress() ||
                        downloadState.isPauseUploadWindUp())
                    return lastState.getDownloadState();
                return DownloadState.DownloadStateBuilder.builder(lastState.getDownloadState())
                        .setStartUploadInProgress(false)
                        .setStartUploadWindUp(true)
                        .setPauseUploadInProgress(false)
                        .setPauseUploadWindUp(true)
                        .setResumeUploadInProgress(false)
                        .setResumeUploadWindUp(false)
                        .build();
            case RESUME_UPLOAD_IN_PROGRESS:
                if (isSomethingRemovedOrInRemove ||
                        downloadState.isStartUploadInProgress() ||
                        !downloadState.isStartUploadWindUp() ||
                        downloadState.isResumeUploadInProgress() ||
                        downloadState.isResumeUploadWindUp())
                    return lastState.getDownloadState();
                return DownloadState.DownloadStateBuilder.builder(lastState.getDownloadState())
                        .setStartUploadInProgress(false)
                        .setStartUploadWindUp(true)
                        .setPauseUploadInProgress(false)
                        .setPauseUploadWindUp(false)
                        .setResumeUploadInProgress(true)
                        .setResumeUploadWindUp(false)
                        .build();
            case RESUME_UPLOAD_WIND_UP:
                if (isSomethingRemovedOrInRemove ||
                        downloadState.isStartUploadInProgress() ||
                        !downloadState.isStartUploadWindUp() ||
                        !downloadState.isResumeUploadInProgress() ||
                        downloadState.isResumeUploadWindUp())
                    return lastState.getDownloadState();
                return DownloadState.DownloadStateBuilder.builder(lastState.getDownloadState())
                        .setStartUploadInProgress(false)
                        .setStartUploadWindUp(true)
                        .setPauseUploadInProgress(false)
                        .setPauseUploadWindUp(false)
                        .setResumeUploadInProgress(false)
                        .setResumeUploadWindUp(true)
                        .build();


            case COMPLETED_DOWNLOADING_IN_PROGRESS:
                if (isSomethingRemovedOrInRemove ||
                        downloadState.isCompletedDownloadingInProgress() ||
                        downloadState.isCompletedDownloadingWindUp() ||
                        downloadState.isStartDownloadInProgress() ||
                        !downloadState.isStartDownloadWindUp() ||
                        downloadState.isResumeDownloadInProgress() ||
                        !downloadState.isResumeDownloadWindUp() ||
                        downloadState.isPauseDownloadInProgress() ||
                        downloadState.isPauseDownloadWindUp())
                    return lastState.getDownloadState();
                return DownloadState.DownloadStateBuilder.builder(lastState.getDownloadState())
                        .setStartDownloadInProgress(false)
                        .setStartDownloadWindUp(true)
                        .setResumeDownloadInProgress(false)
                        .setResumeDownloadWindUp(false)
                        .setPauseDownloadInProgress(false)
                        .setPauseDownloadWindUp(true)
                        .setCompletedDownloadingInProgress(true)
                        .setCompletedDownloadingWindUp(false)
                        .build();
            case COMPLETED_DOWNLOADING_WIND_UP:
                if (isSomethingRemovedOrInRemove ||
                        !downloadState.isCompletedDownloadingInProgress() ||
                        downloadState.isCompletedDownloadingWindUp() ||
                        downloadState.isStartDownloadInProgress() ||
                        !downloadState.isStartDownloadWindUp() ||
                        downloadState.isResumeDownloadInProgress() ||
                        !downloadState.isResumeDownloadWindUp() ||
                        downloadState.isPauseDownloadInProgress() ||
                        downloadState.isPauseDownloadWindUp())
                    return lastState.getDownloadState();
                return DownloadState.DownloadStateBuilder.builder(lastState.getDownloadState())
                        .setStartDownloadInProgress(false)
                        .setStartDownloadWindUp(true)
                        .setResumeDownloadInProgress(false)
                        .setResumeDownloadWindUp(false)
                        .setPauseDownloadInProgress(false)
                        .setPauseDownloadWindUp(true)
                        .setCompletedDownloadingInProgress(false)
                        .setCompletedDownloadingWindUp(true)
                        .build();
        }
    }
}
