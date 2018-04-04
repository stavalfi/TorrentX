package main.torrent.status;

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
                .doOnNext(this::setCurrentState)
                .publish()
                .autoConnect();
    }

    @Override
    public Flux<TorrentStatusType> getStatusTypeFlux() {
        return this.statusTypeFlux;
    }

    @Override
    public synchronized void startDownload() {
        if (!this.isStartedDownload.get() &&
                !this.isCompletedDownloading.get() &&
                !this.isTorrentRemoved.get() &&
                !this.isFilesRemoved.get()) {
            this.statusTypeFluxSink.next(TorrentStatusType.START_DOWNLOAD);
            this.statusTypeFluxSink.next(TorrentStatusType.RESUME_DOWNLOAD);
            this.statusTypeFluxSink.next(TorrentStatusType.RESUME_UPLOAD);
        }
    }

    @Override
    public synchronized void startUpload() {
        if (!this.isStartedUpload.get() &&
                !this.isTorrentRemoved.get() &&
                !this.isFilesRemoved.get()) {
            this.statusTypeFluxSink.next(TorrentStatusType.START_UPLOAD);
            this.statusTypeFluxSink.next(TorrentStatusType.RESUME_DOWNLOAD);
            this.statusTypeFluxSink.next(TorrentStatusType.RESUME_UPLOAD);
        }
    }

    @Override
    public synchronized void resumeDownload() {
        if (!this.isDownloading.get() &&
                !this.isTorrentRemoved.get() &&
                !this.isFilesRemoved.get() &&
                !this.isCompletedDownloading.get()) {
            this.statusTypeFluxSink.next(TorrentStatusType.RESUME_DOWNLOAD);
        }
    }

    @Override
    public synchronized void resumeUpload() {
        if (!this.isUploading.get() &&
                !this.isTorrentRemoved.get() &&
                !this.isFilesRemoved.get()) {
            this.statusTypeFluxSink.next(TorrentStatusType.RESUME_UPLOAD);
        }
    }

    @Override
    public synchronized void pauseDownload() {
        if (this.isDownloading.get() &&
                !this.isTorrentRemoved.get() &&
                !this.isFilesRemoved.get() &&
                !this.isCompletedDownloading.get()) {
            this.statusTypeFluxSink.next(TorrentStatusType.PAUSE_DOWNLOAD);
        }
    }

    @Override
    public synchronized void pauseUpload() {
        if (this.isUploading.get() &&
                !this.isTorrentRemoved.get() &&
                !this.isFilesRemoved.get()) {
            this.statusTypeFluxSink.next(TorrentStatusType.PAUSE_UPLOAD);
        }
    }

    @Override
    public synchronized void completedDownloading() {
        if (this.isCompletedDownloading.compareAndSet(false, true) &&
                !this.isTorrentRemoved.get() &&
                !this.isFilesRemoved.get()) {
            this.statusTypeFluxSink.next(TorrentStatusType.PAUSE_DOWNLOAD);
            this.statusTypeFluxSink.next(TorrentStatusType.COMPLETED_DOWNLOADING);
        }
    }

    @Override
    public synchronized void removeTorrent() {
        if (!this.isTorrentRemoved.get()) {
            if (this.isDownloading.get())
                this.statusTypeFluxSink.next(TorrentStatusType.PAUSE_DOWNLOAD);
            if (this.isUploading.get())
                this.statusTypeFluxSink.next(TorrentStatusType.PAUSE_UPLOAD);
            this.statusTypeFluxSink.next(TorrentStatusType.REMOVE_TORRENT);
        }
    }

    @Override
    public synchronized void removeFiles() {
        if (!this.isFilesRemoved.get()) {
            if (this.isDownloading.get())
                this.statusTypeFluxSink.next(TorrentStatusType.PAUSE_DOWNLOAD);
            if (this.isUploading.get())
                this.statusTypeFluxSink.next(TorrentStatusType.PAUSE_UPLOAD);
            this.statusTypeFluxSink.next(TorrentStatusType.REMOVE_FILES);
        }
    }

    @Override
    public boolean isStartedDownload() {
        return this.isStartedDownload.get();
    }

    @Override
    public boolean isStartedUpload() {
        return this.isStartedUpload.get();
    }

    @Override
    public boolean isCompletedDownloading() {
        return this.isCompletedDownloading.get();
    }

    @Override
    public boolean isUploading() {
        return this.isUploading.get();
    }

    @Override
    public boolean isDownloading() {
        return this.isDownloading.get();
    }

    @Override
    public boolean isTorrentRemoved() {
        return this.isTorrentRemoved.get();
    }

    @Override
    public boolean isFileRemoved() {
        return this.isFilesRemoved.get();
    }

    @Override
    public TorrentInfo getTorrentInfo() {
        return this.torrentInfo;
    }

    private void setCurrentState(TorrentStatusType torrentStatusType) {
        switch (torrentStatusType) {
            case START_DOWNLOAD:
                this.isStartedDownload.set(true);
                break;
            case START_UPLOAD:
                this.isStartedUpload.set(true);
                break;
            case PAUSE_DOWNLOAD:
                this.isDownloading.set(false);
                break;
            case RESUME_DOWNLOAD:
                this.isDownloading.set(true);
                break;
            case PAUSE_UPLOAD:
                this.isUploading.set(false);
                break;
            case RESUME_UPLOAD:
                this.isDownloading.set(true);
                break;
            case COMPLETED_DOWNLOADING:
                this.isCompletedDownloading.set(true);
                break;
            case REMOVE_TORRENT:
                this.isTorrentRemoved.set(true);
                break;
            case REMOVE_FILES:
                this.isFilesRemoved.set(true);
                break;
        }
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
