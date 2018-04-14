package main.torrent.status;

import main.TorrentInfo;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface TorrentStatus {
    TorrentInfo getTorrentInfo();

    Flux<TorrentStatusType> getStatusTypeFlux();

    Flux<Boolean> isStartedDownloadingFlux();

    Flux<Boolean> isStartedUploadingFlux();

    Flux<Boolean> isDownloadingFlux();

    Flux<Boolean> isUploadingFlux();

    Flux<Boolean> isCompletedDownloadingFlux();

    Flux<Boolean> isTorrentRemovedFlux();

    Flux<Boolean> isFilesRemovedFlux();

    Mono<TorrentStatusType> notifyWhenStartedDownloading();

    Mono<TorrentStatusType> notifyWhenStartedUploading();

    Flux<TorrentStatusType> notifyWhenDownloading();

    Flux<TorrentStatusType> notifyWhenUploading();

    Mono<TorrentStatusType> notifyWhenCompletedDownloading();

    Mono<TorrentStatusType> notifyWhenTorrentRemoved();

    Mono<TorrentStatusType> notifyWhenFilesRemoved();
}
