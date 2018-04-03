package main.downloader;

import main.TorrentInfo;
import reactor.core.publisher.Flux;

public interface DownloadStatus {
    TorrentInfo getTorrentInfo();

    boolean isPaused();

    boolean isStarted();

    boolean isRemoved();

    boolean isCompletedDownloading();

    boolean isUploading();

    boolean isDownloading();

    Flux<TorrentStatusType> getStatusTypeFlux();
}
