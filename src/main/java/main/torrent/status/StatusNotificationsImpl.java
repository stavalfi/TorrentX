package main.torrent.status;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class StatusNotificationsImpl implements StatusNotifications {
    private Mono<Status> notifyWhenStartDownloading;
    private Mono<Status> notifyWhenStartUploading;
    private Flux<Status> notifyWhenDownloading;
    private Flux<Status> notifyWhenNotDownloading;
    private Flux<Status> notifyWhenUploading;
    private Flux<Status> notifyWhenNotUploading;
    private Mono<Status> notifyWhenCompletedDownloading;
    private Mono<Status> notifyWhenTorrentRemoved;
    private Mono<Status> notifyWhenFilesRemoved;
    private Mono<Status> notifyWhenStartedListeningToIncomingPeers;
    private Flux<Status> notifyWhenListeningToIncomingPeers;
    private Flux<Status> notifyWhenNotListeningToIncomingPeers;
    private Mono<Status> notifyWhenStartSearchingPeers;
    private Flux<Status> notifyWhenSearchingPeers;
    private Flux<Status> notifyWhenNotSearchingPeers;

    public StatusNotificationsImpl(Flux<Status> latestStatus$) {
        this.notifyWhenStartDownloading = latestStatus$
                .filter(status -> status.isStartedDownload())
                .take(1)
                .single();

        this.notifyWhenStartUploading = latestStatus$
                .filter(status -> status.isStartedUpload())
                .take(1)
                .single();

        this.notifyWhenDownloading = latestStatus$
                .filter(status -> status.isDownloading());

        this.notifyWhenNotDownloading = latestStatus$
                .filter(status -> !status.isDownloading());

        this.notifyWhenUploading = latestStatus$
                .filter(status -> status.isUploading());

        this.notifyWhenNotUploading = latestStatus$
                .filter(status -> !status.isUploading());

        this.notifyWhenCompletedDownloading = latestStatus$
                .filter(status -> status.isCompletedDownloading())
                .take(1)
                .single();

        this.notifyWhenTorrentRemoved = latestStatus$
                .filter(status -> status.isTorrentRemoved())
                .take(1)
                .single();

        this.notifyWhenFilesRemoved = latestStatus$
                .filter(status -> status.isFilesRemoved())
                .take(1)
                .single();

        this.notifyWhenStartedListeningToIncomingPeers = latestStatus$
                .filter(status -> status.isStartedListeningToIncomingPeers())
                .take(1)
                .single();

        this.notifyWhenListeningToIncomingPeers = latestStatus$
                .filter(status -> status.isListeningToIncomingPeers());

        this.notifyWhenNotListeningToIncomingPeers = latestStatus$
                .filter(status -> !status.isListeningToIncomingPeers());

        this.notifyWhenStartSearchingPeers = latestStatus$
                .filter(status -> status.isStartedSearchingPeers())
                .take(1)
                .single();

        this.notifyWhenSearchingPeers = latestStatus$
                .filter(status -> status.isSearchingPeers());

        this.notifyWhenNotSearchingPeers = latestStatus$
                .filter(status -> !status.isSearchingPeers());


    }

    @Override
    public Mono<Status> notifyWhenStartDownloading() {
        return this.notifyWhenStartDownloading;
    }

    @Override
    public Mono<Status> notifyWhenStartUploading() {
        return this.notifyWhenStartUploading;
    }

    @Override
    public Flux<Status> notifyWhenResumeDownload() {
        return this.notifyWhenDownloading;
    }

    @Override
    public Flux<Status> notifyWhenResumeUpload() {
        return this.notifyWhenUploading;
    }

    @Override
    public Mono<Status> notifyWhenCompletedDownloading() {
        return notifyWhenCompletedDownloading;
    }

    @Override
    public Mono<Status> notifyWhenTorrentRemoved() {
        return notifyWhenTorrentRemoved;
    }

    @Override
    public Mono<Status> notifyWhenFilesRemoved() {
        return notifyWhenFilesRemoved;
    }

    @Override
    public Mono<Status> notifyWhenStartedListeningToIncomingPeers() {
        return notifyWhenStartedListeningToIncomingPeers;
    }

    @Override
    public Flux<Status> notifyWhenListeningToIncomingPeers() {
        return notifyWhenListeningToIncomingPeers;
    }

    @Override
    public Mono<Status> notifyWhenStartSearchingPeers() {
        return notifyWhenStartSearchingPeers;
    }

    @Override
    public Flux<Status> notifyWhenSearchingPeers() {
        return notifyWhenSearchingPeers;
    }

    @Override
    public Flux<Status> notifyWhenNotDownloading() {
        return notifyWhenNotDownloading;
    }

    @Override
    public Flux<Status> notifyWhenNotUploading() {
        return notifyWhenNotUploading;
    }

    @Override
    public Flux<Status> notifyWhenNotListeningToIncomingPeers() {
        return notifyWhenNotListeningToIncomingPeers;
    }

    @Override
    public Flux<Status> notifyWhenNotSearchingPeers() {
        return notifyWhenNotSearchingPeers;
    }
}
