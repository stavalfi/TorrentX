package main.torrent.status;

import main.TorrentInfo;
import reactor.core.publisher.Flux;

public interface TorrentStatus {
    TorrentInfo getTorrentInfo();

    Flux<TorrentStatusType> getStatusTypeFlux();

    Flux<Boolean> isStartedDownloadingFlux();

    Flux<Boolean> isStartedUploadingFlux();

    Flux<Boolean> isTorrentRemovedFlux();

    Flux<Boolean> isFilesRemovedFlux();

    Flux<Boolean> isDownloadingFlux();

    Flux<Boolean> isUploadingFlux();

    Flux<Boolean> isCompletedDownloadingFlux();
}
