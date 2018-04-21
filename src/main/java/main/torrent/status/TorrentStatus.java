package main.torrent.status;

import main.TorrentInfo;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface TorrentStatus {
    TorrentInfo getTorrentInfo();

    Flux<TorrentStatusType> getStatusTypeFlux();

    Flux<Boolean> isStartDownloadingFlux();

    Flux<Boolean> isStartUploadingFlux();

    Flux<Boolean> isDownloadingFlux();

    Flux<Boolean> isUploadingFlux();

    Flux<Boolean> isCompletedDownloadingFlux();

    Flux<Boolean> isTorrentRemovedFlux();

    Flux<Boolean> isFilesRemovedFlux();

    Flux<Boolean> isStartListeningToIncomingPeersFlux();

    Flux<Boolean> isListeningToIncomingPeersFlux();

    Flux<Boolean> isStartSearchingPeersFlux();

    Flux<Boolean> isSearchingPeersFlux();

    Mono<TorrentStatusType> notifyWhenStartDownloading();

    Mono<TorrentStatusType> notifyWhenStartUploading();

    Flux<TorrentStatusType> notifyWhenResumeDownload();

    Flux<TorrentStatusType> notifyWhenResumeUpload();

    Mono<TorrentStatusType> notifyWhenCompletedDownloading();

    Mono<TorrentStatusType> notifyWhenTorrentRemoved();

    Mono<TorrentStatusType> notifyWhenFilesRemoved();

    Mono<TorrentStatusType> notifyWhenStartedListeningToIncomingPeers();

    Flux<TorrentStatusType> notifyWhenListeningToIncomingPeers();

    Mono<TorrentStatusType> notifyWhenStartSearchingPeers();

    Flux<TorrentStatusType> notifySearchingPeers();
}
