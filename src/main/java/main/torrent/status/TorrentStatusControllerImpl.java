package main.torrent.status;

import main.App;
import main.TorrentInfo;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import java.util.concurrent.atomic.AtomicBoolean;

public class TorrentStatusControllerImpl implements TorrentStatusController {

    private TorrentInfo torrentInfo;
    private AtomicBoolean isStartedDownload;
    private AtomicBoolean isStartedUpload;
    private AtomicBoolean isTorrentRemoved;
    private AtomicBoolean isFilesRemoved;
    private AtomicBoolean isUploading;
    private AtomicBoolean isDownloading;
    private AtomicBoolean isCompletedDownloading;
    private Flux<TorrentStatusType> statusTypeFlux;
    private FluxSink<TorrentStatusType> statusTypeFluxSink;
    private Flux<Boolean> isStartedDownloadingFlux;
    private Flux<Boolean> isStartedUploadingFlux;
    private Flux<Boolean> isTorrentRemovedFlux;
    private Flux<Boolean> isFilesRemovedFlux;
    private Flux<Boolean> isDownloadingFlux;
    private Flux<Boolean> isUploadingFlux;
    private Flux<Boolean> isCompletedDownloadingFlux;

    public TorrentStatusControllerImpl(TorrentInfo torrentInfo,
                                       boolean isStartedDownload,
                                       boolean isStartedUpload,
                                       boolean isTorrentRemoved,
                                       boolean isFilesRemoved,
                                       boolean isUploading,
                                       boolean isDownloading,
                                       boolean isCompletedDownloading) {
        this.torrentInfo = torrentInfo;

        // initialize all properties in false and at the end of the constructor, I will change it.
        this.isStartedDownload = new AtomicBoolean(false);
        this.isStartedUpload = new AtomicBoolean(false);
        this.isTorrentRemoved = new AtomicBoolean(false);
        this.isFilesRemoved = new AtomicBoolean(false);
        this.isUploading = new AtomicBoolean(false);
        this.isDownloading = new AtomicBoolean(false);
        this.isCompletedDownloading = new AtomicBoolean(false);

        this.statusTypeFlux = Flux.<TorrentStatusType>create(sink -> this.statusTypeFluxSink = sink)
                // we have to make it publishOn and not subscribeOn because if we use subscribeOn,
                // than when we do autoConnect(0), we may not come to create immediately and we
                // must initialize statusTypeFluxSink asap.
                .publishOn(App.MyScheduler)
                .publish()
                .autoConnect(0);

        this.isStartedDownloadingFlux = this.statusTypeFlux
                .filter(torrentStatusType -> torrentStatusType.equals(TorrentStatusType.START_DOWNLOAD) ||
                        torrentStatusType.equals(TorrentStatusType.NOT_START_DOWNLOAD))
                .map(torrentStatusType -> torrentStatusType.equals(TorrentStatusType.START_DOWNLOAD))
                .replay(1)
                .autoConnect(0);

        this.isStartedUploadingFlux = this.statusTypeFlux
                .filter(torrentStatusType -> torrentStatusType.equals(TorrentStatusType.START_UPLOAD) ||
                        torrentStatusType.equals(TorrentStatusType.NOT_START_UPLOAD))
                .map(torrentStatusType -> torrentStatusType.equals(TorrentStatusType.START_UPLOAD))
                .replay(1)
                .autoConnect(0);

        this.isDownloadingFlux = this.statusTypeFlux
                .filter(torrentStatusType -> torrentStatusType.equals(TorrentStatusType.RESUME_DOWNLOAD) ||
                        torrentStatusType.equals(TorrentStatusType.PAUSE_DOWNLOAD))
                .map(torrentStatusType -> torrentStatusType.equals(TorrentStatusType.RESUME_DOWNLOAD))
                .replay(1)
                .autoConnect(0);

        this.isUploadingFlux = this.statusTypeFlux
                .filter(torrentStatusType -> torrentStatusType.equals(TorrentStatusType.RESUME_UPLOAD) ||
                        torrentStatusType.equals(TorrentStatusType.PAUSE_UPLOAD))
                .map(torrentStatusType -> torrentStatusType.equals(TorrentStatusType.RESUME_UPLOAD))
                .replay(1)
                .autoConnect(0);

        this.isCompletedDownloadingFlux = this.statusTypeFlux
                .filter(torrentStatusType -> torrentStatusType.equals(TorrentStatusType.COMPLETED_DOWNLOADING) ||
                        torrentStatusType.equals(TorrentStatusType.NOT_COMPLETED_DOWNLOADING))
                .map(torrentStatusType -> torrentStatusType.equals(TorrentStatusType.COMPLETED_DOWNLOADING))
                .replay(1)
                .autoConnect(0);

        this.isTorrentRemovedFlux = this.statusTypeFlux
                .filter(torrentStatusType -> torrentStatusType.equals(TorrentStatusType.REMOVE_TORRENT) ||
                        torrentStatusType.equals(TorrentStatusType.NOT_REMOVE_TORRENT))
                .map(torrentStatusType -> torrentStatusType.equals(TorrentStatusType.REMOVE_TORRENT))
                .replay(1)
                .autoConnect(0);

        this.isFilesRemovedFlux = this.statusTypeFlux
                .filter(torrentStatusType -> torrentStatusType.equals(TorrentStatusType.REMOVE_FILES) ||
                        torrentStatusType.equals(TorrentStatusType.NOT_REMOVE_FILES))
                .map(torrentStatusType -> torrentStatusType.equals(TorrentStatusType.REMOVE_FILES))
                .replay(1)
                .autoConnect(0);

        // signal initial state:

        if (isStartedDownload)
            startDownload();
        else
            statusTypeFluxSink.next(TorrentStatusType.NOT_START_DOWNLOAD);
        if (isStartedUpload)
            startUpload();
        else
            statusTypeFluxSink.next(TorrentStatusType.NOT_START_UPLOAD);
        if (isTorrentRemoved)
            removeTorrent();
        else
            statusTypeFluxSink.next(TorrentStatusType.NOT_REMOVE_TORRENT);
        if (isFilesRemoved)
            removeFiles();
        else
            statusTypeFluxSink.next(TorrentStatusType.NOT_REMOVE_FILES);
        if (isUploading)
            resumeUpload();
        else
            statusTypeFluxSink.next(TorrentStatusType.PAUSE_UPLOAD);
        if (isDownloading)
            startDownload();
        else
            statusTypeFluxSink.next(TorrentStatusType.PAUSE_DOWNLOAD);
        if (isCompletedDownloading)
            completedDownloading();
        else
            statusTypeFluxSink.next(TorrentStatusType.NOT_COMPLETED_DOWNLOADING);

    }

    @Override
    public Flux<TorrentStatusType> getStatusTypeFlux() {
        return this.statusTypeFlux;
    }

    @Override
    public Flux<Boolean> isStartedDownloadingFlux() {
        return this.isStartedDownloadingFlux;
    }

    @Override
    public Flux<Boolean> isStartedUploadingFlux() {
        return this.isStartedUploadingFlux;
    }

    @Override
    public Flux<Boolean> isTorrentRemovedFlux() {
        return this.isTorrentRemovedFlux;
    }

    @Override
    public Flux<Boolean> isFilesRemovedFlux() {
        return this.isFilesRemovedFlux;
    }

    @Override
    public Flux<Boolean> isDownloadingFlux() {
        return this.isDownloadingFlux;
    }

    @Override
    public Flux<Boolean> isUploadingFlux() {
        return this.isUploadingFlux;
    }

    @Override
    public Flux<Boolean> isCompletedDownloadingFlux() {
        return this.isCompletedDownloadingFlux;
    }

    @Override
    public synchronized void startDownload() {
        if (!this.isStartedDownload.get() &&
                !this.isCompletedDownloading.get() &&
                !this.isTorrentRemoved.get() &&
                !this.isFilesRemoved.get()) {
            this.isStartedDownload.set(true);
            this.statusTypeFluxSink.next(TorrentStatusType.START_DOWNLOAD);
            this.isDownloading.set(true);
            this.statusTypeFluxSink.next(TorrentStatusType.RESUME_DOWNLOAD);
        }
    }

    @Override
    public synchronized void startUpload() {
        if (!this.isStartedUpload.get() &&
                !this.isTorrentRemoved.get() &&
                !this.isFilesRemoved.get()) {
            this.isStartedUpload.set(true);
            this.statusTypeFluxSink.next(TorrentStatusType.START_UPLOAD);
            this.isUploading.set(true);
            this.statusTypeFluxSink.next(TorrentStatusType.RESUME_UPLOAD);
        }
    }

    @Override
    public synchronized void resumeDownload() {
        if (!this.isDownloading.get() &&
                !this.isTorrentRemoved.get() &&
                !this.isFilesRemoved.get() &&
                !this.isCompletedDownloading.get()) {
            this.isDownloading.set(true);
            this.statusTypeFluxSink.next(TorrentStatusType.RESUME_DOWNLOAD);
        }
    }

    @Override
    public synchronized void resumeUpload() {
        if (!this.isUploading.get() &&
                !this.isTorrentRemoved.get() &&
                !this.isFilesRemoved.get()) {
            this.isUploading.set(true);
            this.statusTypeFluxSink.next(TorrentStatusType.RESUME_UPLOAD);
        }
    }

    @Override
    public synchronized void pauseDownload() {
        if (this.isDownloading.get() &&
                !this.isTorrentRemoved.get() &&
                !this.isFilesRemoved.get() &&
                !this.isCompletedDownloading.get()) {
            this.isDownloading.set(false);
            this.statusTypeFluxSink.next(TorrentStatusType.PAUSE_DOWNLOAD);
        }
    }

    @Override
    public synchronized void pauseUpload() {
        if (this.isUploading.get() &&
                !this.isTorrentRemoved.get() &&
                !this.isFilesRemoved.get()) {
            this.isUploading.set(false);
            this.statusTypeFluxSink.next(TorrentStatusType.PAUSE_UPLOAD);
        }
    }

    @Override
    public synchronized void completedDownloading() {
        if (this.isCompletedDownloading.compareAndSet(false, true) &&
                !this.isTorrentRemoved.get() &&
                !this.isFilesRemoved.get()) {
            this.isDownloading.set(false);
            this.statusTypeFluxSink.next(TorrentStatusType.PAUSE_DOWNLOAD);
            this.isCompletedDownloading.set(true);
            this.statusTypeFluxSink.next(TorrentStatusType.COMPLETED_DOWNLOADING);
        }
    }

    @Override
    public synchronized void removeTorrent() {
        if (!this.isTorrentRemoved.get()) {
            if (this.isDownloading.get()) {
                this.isDownloading.set(false);
                this.statusTypeFluxSink.next(TorrentStatusType.PAUSE_DOWNLOAD);
            }
            if (this.isUploading.get()) {
                this.isUploading.set(false);
                this.statusTypeFluxSink.next(TorrentStatusType.PAUSE_UPLOAD);
            }
            this.isTorrentRemoved.set(true);
            this.statusTypeFluxSink.next(TorrentStatusType.REMOVE_TORRENT);
        }
    }

    @Override
    public synchronized void removeFiles() {
        if (!this.isFilesRemoved.get()) {
            if (this.isDownloading.get()) {
                this.isDownloading.set(false);
                this.statusTypeFluxSink.next(TorrentStatusType.PAUSE_DOWNLOAD);
            }
            if (this.isUploading.get()) {
                this.isUploading.set(false);
                this.statusTypeFluxSink.next(TorrentStatusType.PAUSE_UPLOAD);
            }
            this.isFilesRemoved.set(true);
            this.statusTypeFluxSink.next(TorrentStatusType.REMOVE_FILES);
        }
    }

    @Override
    public TorrentInfo getTorrentInfo() {
        return this.torrentInfo;
    }

    public static TorrentStatusController createDefaultTorrentStatusController(TorrentInfo torrentInfo) {
        return new TorrentStatusControllerImpl(torrentInfo,
                false,
                false,
                false,
                false,
                false,
                false,
                false);
    }
}
