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

        this.isStartedDownload = new AtomicBoolean(isStartedDownload);
        this.isStartedUpload = new AtomicBoolean(isStartedUpload);
        this.isTorrentRemoved = new AtomicBoolean(isTorrentRemoved);
        this.isFilesRemoved = new AtomicBoolean(isFilesRemoved);
        this.isUploading = new AtomicBoolean(isUploading);
        this.isDownloading = new AtomicBoolean(isDownloading);
        this.isCompletedDownloading = new AtomicBoolean(isCompletedDownloading);

        this.statusTypeFlux = Flux.<TorrentStatusType>create(sink -> this.statusTypeFluxSink = sink)
                .doOnNext(x-> System.out.println(x))
                // we have to make it publishOn and not subscribeOn because if we use subscribeOn,
                // than when we do autoConnect(0), we may not come to create-method immediately and we
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
            statusTypeFluxSink.next(TorrentStatusType.START_DOWNLOAD);
        else
            statusTypeFluxSink.next(TorrentStatusType.NOT_START_DOWNLOAD);
        if (isStartedUpload)
            statusTypeFluxSink.next(TorrentStatusType.START_UPLOAD);
        else
            statusTypeFluxSink.next(TorrentStatusType.NOT_START_UPLOAD);
        if (isTorrentRemoved)
            statusTypeFluxSink.next(TorrentStatusType.REMOVE_TORRENT);
        else
            statusTypeFluxSink.next(TorrentStatusType.NOT_REMOVE_TORRENT);
        if (isFilesRemoved)
            statusTypeFluxSink.next(TorrentStatusType.REMOVE_FILES);
        else
            statusTypeFluxSink.next(TorrentStatusType.NOT_REMOVE_FILES);
        if (isUploading)
            statusTypeFluxSink.next(TorrentStatusType.RESUME_UPLOAD);
        else
            statusTypeFluxSink.next(TorrentStatusType.PAUSE_UPLOAD);
        if (isDownloading)
            statusTypeFluxSink.next(TorrentStatusType.RESUME_DOWNLOAD);
        else
            statusTypeFluxSink.next(TorrentStatusType.PAUSE_DOWNLOAD);
        if (isCompletedDownloading)
            statusTypeFluxSink.next(TorrentStatusType.COMPLETED_DOWNLOADING);
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
        if (this.isStartedDownload.compareAndSet(false, true) &&
                !this.isCompletedDownloading.get() &&
                !this.isTorrentRemoved.get() &&
                !this.isFilesRemoved.get()) {
            this.isDownloading.set(true);
            this.statusTypeFluxSink.next(TorrentStatusType.START_DOWNLOAD);
            this.statusTypeFluxSink.next(TorrentStatusType.RESUME_DOWNLOAD);
        }
    }

    @Override
    public synchronized void startUpload() {
        if (this.isStartedUpload.compareAndSet(false, true) &&
                !this.isTorrentRemoved.get() &&
                !this.isFilesRemoved.get()) {
            this.isUploading.set(true);
            this.statusTypeFluxSink.next(TorrentStatusType.START_UPLOAD);
            this.statusTypeFluxSink.next(TorrentStatusType.RESUME_UPLOAD);
        }
    }

    @Override
    public synchronized void resumeDownload() {
        if (this.isDownloading.compareAndSet(false, true) &&
                !this.isTorrentRemoved.get() &&
                !this.isFilesRemoved.get() &&
                !this.isCompletedDownloading.get()) {
            this.statusTypeFluxSink.next(TorrentStatusType.RESUME_DOWNLOAD);
        }
    }

    @Override
    public synchronized void resumeUpload() {
        if (this.isUploading.compareAndSet(false, true) &&
                !this.isTorrentRemoved.get() &&
                !this.isFilesRemoved.get()) {
            this.statusTypeFluxSink.next(TorrentStatusType.RESUME_UPLOAD);
        }
    }

    @Override
    public synchronized void pauseDownload() {
        if (this.isDownloading.compareAndSet(true, false) &&
                !this.isTorrentRemoved.get() &&
                !this.isFilesRemoved.get() &&
                !this.isCompletedDownloading.get()) {
            this.statusTypeFluxSink.next(TorrentStatusType.PAUSE_DOWNLOAD);
        }
    }

    @Override
    public synchronized void pauseUpload() {
        if (this.isUploading.compareAndSet(true, false) &&
                !this.isTorrentRemoved.get() &&
                !this.isFilesRemoved.get()) {
            this.statusTypeFluxSink.next(TorrentStatusType.PAUSE_UPLOAD);
        }
    }

    @Override
    public synchronized void completedDownloading() {
        if (this.isCompletedDownloading.compareAndSet(false, true) &&
                this.isStartedDownload.get() &&
                !this.isTorrentRemoved.get() &&
                !this.isFilesRemoved.get()) {
            if (this.isDownloading.compareAndSet(true, false)) {
                this.statusTypeFluxSink.next(TorrentStatusType.PAUSE_DOWNLOAD);
            }
            this.statusTypeFluxSink.next(TorrentStatusType.COMPLETED_DOWNLOADING);
        }
    }

    @Override
    public synchronized void removeTorrent() {
        if (this.isTorrentRemoved.compareAndSet(false, true)) {
            boolean wasDownloading = this.isDownloading.compareAndSet(true, false);
            boolean wasUploading = this.isUploading.compareAndSet(true, false);
            if (wasDownloading) {
                this.statusTypeFluxSink.next(TorrentStatusType.PAUSE_DOWNLOAD);
            }
            if (wasUploading) {
                this.statusTypeFluxSink.next(TorrentStatusType.PAUSE_UPLOAD);
            }
            this.statusTypeFluxSink.next(TorrentStatusType.REMOVE_TORRENT);
        }
    }

    @Override
    public synchronized void removeFiles() {
        if (this.isFilesRemoved.compareAndSet(false, true)) {
            boolean wasDownloading = this.isDownloading.compareAndSet(true, false);
            boolean wasUploading = this.isUploading.compareAndSet(true, false);
            if (wasDownloading) {
                this.statusTypeFluxSink.next(TorrentStatusType.PAUSE_DOWNLOAD);
            }
            if (wasUploading) {
                this.statusTypeFluxSink.next(TorrentStatusType.PAUSE_UPLOAD);
            }
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
