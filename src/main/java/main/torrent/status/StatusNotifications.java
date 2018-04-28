package main.torrent.status;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface StatusNotifications {
    Mono<Status> notifyWhenStartDownloading();

    Mono<Status> notifyWhenStartUploading();

    Flux<Status> notifyWhenResumeDownload();

    Flux<Status> notifyWhenResumeUpload();

    Mono<Status> notifyWhenCompletedDownloading();

    Mono<Status> notifyWhenTorrentRemoved();

    Mono<Status> notifyWhenFilesRemoved();

    Mono<Status> notifyWhenStartedListeningToIncomingPeers();

    Flux<Status> notifyWhenListeningToIncomingPeers();

    Mono<Status> notifyWhenStartSearchingPeers();

    Flux<Status> notifyWhenSearchingPeers();

    Flux<Status> notifyWhenNotDownloading();

    Flux<Status> notifyWhenNotUploading();

    Flux<Status> notifyWhenNotListeningToIncomingPeers();

    Flux<Status> notifyWhenNotSearchingPeers();
}
