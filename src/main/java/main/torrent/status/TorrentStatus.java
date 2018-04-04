package main.torrent.status;

import main.TorrentInfo;
import reactor.core.publisher.Flux;

public interface TorrentStatus {
    TorrentInfo getTorrentInfo();

    boolean isStartedDownload();

    boolean isStartedUpload();

    boolean isTorrentRemoved();

    boolean isFileRemoved();

    boolean isCompletedDownloading();

    boolean isUploading();

    boolean isDownloading();

    Flux<TorrentStatusType> getStatusTypeFlux();
}
