package main.torrent.status;

import main.TorrentInfo;
import reactor.core.publisher.Flux;

public interface TorrentStatus {
    TorrentInfo getTorrentInfo();

    boolean isStarted();

    boolean isRemoved();

    boolean isCompletedDownloading();

    boolean isUploading();

    boolean isDownloading();

    Flux<TorrentStatusType> getStatusTypeFlux();
}
