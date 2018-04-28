package main.torrent.status;

import main.App;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;

public class StatusChanger {

    private FluxSink<Status> latestStatusSink;
    private Flux<Status> latestStatus$;
    private Flux<Status> history$;

    public StatusChanger(Status initialStatus) {
        this.latestStatus$ = Flux.<Status>create(sink -> this.latestStatusSink = sink)
                .publishOn(App.MyScheduler)
                .replay(1)
                .autoConnect(0);

        this.latestStatusSink.next(initialStatus);

        this.history$ = this.latestStatus$.replay(10) // how much statuses to save.
                .autoConnect(0);
    }

    public Mono<Status> getLatestStatus$() {
        return latestStatus$.take(1)
                .single();
    }

    public Flux<Status> getStatus$() {
        return latestStatus$;
    }

    public Flux<Status> getHistory$() {
        return history$;
    }

    public Mono<Status> changeStatus(StatusType change) {
        // TODO: replace this shit with something better.
        return this.latestStatus$
                .take(1)
                .single()
                .flatMap(lastStatus -> {
                    Status newStatus = changeStatus(lastStatus, change);
                    if (lastStatus.equals(newStatus))
                        return Mono.empty();
                    this.latestStatusSink.next(newStatus);
                    return Mono.just(newStatus)
                            // we need to assert that the new state is available.
                            .flatMapMany(status -> this.latestStatus$)
                            // status == "lastStatus" or "newStatus". there is no other option.
                            .filter(status -> status.equals(newStatus))
                            .take(1)
                            .single();
                });
    }

    /**
     * Get the last state and what has change and return a new state.
     * It's a pure function. (Redux style).
     *
     * @param lastStatus the old status.
     * @param change     what to change in the status.
     * @return the new status.
     */
    private Status changeStatus(Status lastStatus, StatusType change) {
        switch (change) {
            case START_DOWNLOAD:
                if (!lastStatus.isStartedDownload() &&
                        !lastStatus.isCompletedDownloading() &&
                        !lastStatus.isFilesRemoved() &&
                        !lastStatus.isTorrentRemoved())
                    return StatusBuilder.builder(lastStatus)
                            .setStartedDownload(true)
                            .setDownloading(true)
                            .build();
                break;
            case RESUME_DOWNLOAD:
                if (lastStatus.isStartedDownload() &&
                        !lastStatus.isDownloading() &&
                        !lastStatus.isCompletedDownloading() &&
                        !lastStatus.isFilesRemoved() &&
                        !lastStatus.isTorrentRemoved())
                    return StatusBuilder.builder(lastStatus)
                            .setDownloading(true)
                            .build();
                break;
            case PAUSE_DOWNLOAD:
                if (lastStatus.isStartedDownload() &&
                        lastStatus.isDownloading() &&
                        !lastStatus.isCompletedDownloading() &&
                        !lastStatus.isFilesRemoved() &&
                        !lastStatus.isTorrentRemoved())
                    return StatusBuilder.builder(lastStatus)
                            .setDownloading(false)
                            .build();

                break;
            case START_UPLOAD:
                if (!lastStatus.isStartedUpload() &&
                        !lastStatus.isFilesRemoved() &&
                        !lastStatus.isTorrentRemoved())
                    return StatusBuilder.builder(lastStatus)
                            .setStartedUpload(true)
                            .setUploading(true)
                            .build();
                break;
            case RESUME_UPLOAD:
                if (lastStatus.isStartedUpload() &&
                        !lastStatus.isUploading() &&
                        !lastStatus.isCompletedDownloading() &&
                        !lastStatus.isFilesRemoved() &&
                        !lastStatus.isTorrentRemoved())
                    return StatusBuilder.builder(lastStatus)
                            .setStartedUpload(true)
                            .setUploading(true)
                            .build();
                break;
            case PAUSE_UPLOAD:
                if (lastStatus.isStartedUpload() &&
                        lastStatus.isUploading() &&
                        !lastStatus.isCompletedDownloading() &&
                        !lastStatus.isFilesRemoved() &&
                        !lastStatus.isTorrentRemoved())
                    return StatusBuilder.builder(lastStatus)
                            .setUploading(false)
                            .build();

                break;
            case START_LISTENING_TO_INCOMING_PEERS:
                if (!lastStatus.isStartedListeningToIncomingPeers() &&
                        !lastStatus.isFilesRemoved() &&
                        !lastStatus.isTorrentRemoved())
                    return StatusBuilder.builder(lastStatus)
                            .setStartedListeningToIncomingPeers(true)
                            .setListeningToIncomingPeers(true)
                            .build();
                break;
            case RESUME_LISTENING_TO_INCOMING_PEERS:
                if (lastStatus.isStartedListeningToIncomingPeers() &&
                        !lastStatus.isListeningToIncomingPeers() &&
                        !lastStatus.isFilesRemoved() &&
                        !lastStatus.isTorrentRemoved())
                    return StatusBuilder.builder(lastStatus)
                            .setListeningToIncomingPeers(true)
                            .build();
                break;
            case PAUSE_LISTENING_TO_INCOMING_PEERS:
                if (lastStatus.isStartedListeningToIncomingPeers() &&
                        lastStatus.isListeningToIncomingPeers() &&
                        !lastStatus.isFilesRemoved() &&
                        !lastStatus.isTorrentRemoved())
                    return StatusBuilder.builder(lastStatus)
                            .setListeningToIncomingPeers(false)
                            .build();

                break;
            case START_SEARCHING_PEERS:
                if (!lastStatus.isStartedSearchingPeers() &&
                        !lastStatus.isCompletedDownloading() &&
                        !lastStatus.isFilesRemoved() &&
                        !lastStatus.isTorrentRemoved())
                    return StatusBuilder.builder(lastStatus)
                            .setStartedSearchingPeers(true)
                            .setSearchingPeers(true)
                            .build();
                break;
            case RESUME_SEARCHING_PEERS:
                if (lastStatus.isStartedSearchingPeers() &&
                        !lastStatus.isSearchingPeers() &&
                        !lastStatus.isCompletedDownloading() &&
                        !lastStatus.isFilesRemoved() &&
                        !lastStatus.isTorrentRemoved())
                    return StatusBuilder.builder(lastStatus)
                            .setSearchingPeers(true)
                            .build();
                break;
            case PAUSE_SEARCHING_PEERS:
                if (lastStatus.isStartedSearchingPeers() &&
                        lastStatus.isSearchingPeers() &&
                        !lastStatus.isCompletedDownloading() &&
                        !lastStatus.isFilesRemoved() &&
                        !lastStatus.isTorrentRemoved())
                    return StatusBuilder.builder(lastStatus)
                            .setSearchingPeers(false)
                            .build();


                break;
            case REMOVE_FILES:
                if (!lastStatus.isFilesRemoved())
                    return StatusBuilder.builder(lastStatus)
                            .setDownloading(false)
                            .setUploading(false)
                            .setListeningToIncomingPeers(false)
                            .setSearchingPeers(false)
                            .setFilesRemoved(true)
                            .build();
                break;
            case REMOVE_TORRENT:
                if (!lastStatus.isTorrentRemoved())
                    return StatusBuilder.builder(lastStatus)
                            .setDownloading(false)
                            .setUploading(false)
                            .setListeningToIncomingPeers(false)
                            .setSearchingPeers(false)
                            .setTorrentRemoved(true)
                            .build();
                break;

            case COMPLETED_DOWNLOADING:
                if (lastStatus.isStartedDownload() &&
                        lastStatus.isDownloading() &&
                        !lastStatus.isCompletedDownloading() &&
                        !lastStatus.isFilesRemoved() &&
                        !lastStatus.isTorrentRemoved())
                    return StatusBuilder.builder(lastStatus)
                            .setDownloading(false)
                            .setSearchingPeers(false)
                            .setCompletedDownloading(true)
                            .build();
                break;
        }
        return lastStatus;
    }

    private static class StatusBuilder {
        private boolean isStartedDownload;
        private boolean isStartedUpload;
        private boolean isTorrentRemoved;
        private boolean isFilesRemoved;
        private boolean isUploading;
        private boolean isDownloading;
        private boolean isCompletedDownloading;
        private boolean isStartedListeningToIncomingPeers;
        private boolean isListeningToIncomingPeers;
        private boolean isStartedSearchingPeers;
        private boolean isSearchingPeers;

        private StatusBuilder(Status lastStatus) {
            this.isStartedDownload = lastStatus.isStartedDownload();
            this.isStartedUpload = lastStatus.isStartedUpload();
            this.isTorrentRemoved = lastStatus.isTorrentRemoved();
            this.isFilesRemoved = lastStatus.isFilesRemoved();
            this.isUploading = lastStatus.isUploading();
            this.isDownloading = lastStatus.isDownloading();
            this.isCompletedDownloading = lastStatus.isCompletedDownloading();
            this.isStartedListeningToIncomingPeers = lastStatus.isStartedListeningToIncomingPeers();
            this.isListeningToIncomingPeers = lastStatus.isListeningToIncomingPeers();
            this.isStartedSearchingPeers = lastStatus.isStartedSearchingPeers();
            this.isSearchingPeers = lastStatus.isSearchingPeers();
        }

        public static StatusBuilder builder(Status lastStatus) {
            return new StatusBuilder(lastStatus);
        }

        public Status build() {
            return new Status(isStartedDownload,
                    isStartedUpload,
                    isTorrentRemoved,
                    isFilesRemoved,
                    isUploading,
                    isDownloading,
                    isCompletedDownloading,
                    isStartedListeningToIncomingPeers,
                    isListeningToIncomingPeers,
                    isStartedSearchingPeers,
                    isSearchingPeers);
        }

        public StatusBuilder setStartedDownload(boolean startedDownload) {
            isStartedDownload = startedDownload;
            return this;
        }

        public StatusBuilder setStartedUpload(boolean startedUpload) {
            isStartedUpload = startedUpload;
            return this;
        }

        public StatusBuilder setTorrentRemoved(boolean torrentRemoved) {
            isTorrentRemoved = torrentRemoved;
            return this;
        }

        public StatusBuilder setFilesRemoved(boolean filesRemoved) {
            isFilesRemoved = filesRemoved;
            return this;
        }

        public StatusBuilder setUploading(boolean uploading) {
            isUploading = uploading;
            return this;
        }

        public StatusBuilder setDownloading(boolean downloading) {
            isDownloading = downloading;
            return this;
        }

        public StatusBuilder setCompletedDownloading(boolean completedDownloading) {
            isCompletedDownloading = completedDownloading;
            return this;
        }

        public StatusBuilder setStartedListeningToIncomingPeers(boolean startedListeningToIncomingPeers) {
            isStartedListeningToIncomingPeers = startedListeningToIncomingPeers;
            return this;
        }

        public StatusBuilder setListeningToIncomingPeers(boolean listeningToIncomingPeers) {
            isListeningToIncomingPeers = listeningToIncomingPeers;
            return this;
        }

        public StatusBuilder setStartedSearchingPeers(boolean startedSearchingPeers) {
            isStartedSearchingPeers = startedSearchingPeers;
            return this;
        }

        public StatusBuilder setSearchingPeers(boolean searchingPeers) {
            isSearchingPeers = searchingPeers;
            return this;
        }
    }
}
