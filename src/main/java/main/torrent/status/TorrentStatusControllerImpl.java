package main.torrent.status;

import main.TorrentInfo;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import java.util.concurrent.atomic.AtomicBoolean;

public class TorrentStatusControllerImpl implements TorrentStatusController {

    private TorrentInfo torrentInfo;
    private AtomicBoolean isStarted;
    private AtomicBoolean isRemoved;
    private AtomicBoolean isUploading;
    private AtomicBoolean isDownloading;
    private AtomicBoolean isCompletedDownloading;
    private Flux<TorrentStatusType> statusTypeFlux;
    private FluxSink<TorrentStatusType> statusTypeFluxSink;


    public TorrentStatusControllerImpl(TorrentInfo torrentInfo,
                                       boolean isStarted,
                                       boolean isRemoved,
                                       boolean isUploading,
                                       boolean isDownloading,
                                       boolean isCompletedDownloading) {
        this.torrentInfo = torrentInfo;
        this.isStarted = new AtomicBoolean(isStarted);
        this.isRemoved = new AtomicBoolean(isRemoved);
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
    public synchronized void start() {
        if (!this.isStarted.get() &&
                !this.isCompletedDownloading.get() &&
                !this.isRemoved.get()) {
            this.statusTypeFluxSink.next(TorrentStatusType.STARTED);
            this.statusTypeFluxSink.next(TorrentStatusType.RESUME_DOWNLOAD);
            this.statusTypeFluxSink.next(TorrentStatusType.RESUME_UPLOAD);
        }
    }

    @Override
    public synchronized void resumeDownload() {
        if (!this.isDownloading.get() &&
                !this.isRemoved.get() &&
                !this.isCompletedDownloading.get()) {
            this.statusTypeFluxSink.next(TorrentStatusType.RESUME_DOWNLOAD);
        }
    }

    @Override
    public synchronized void resumeUpload() {
        if (!this.isUploading.get() &&
                !this.isRemoved.get()) {
            this.statusTypeFluxSink.next(TorrentStatusType.RESUME_UPLOAD);
        }
    }

    @Override
    public synchronized void pauseDownload() {
        if (this.isDownloading.get() &&
                !this.isRemoved.get() &&
                !this.isCompletedDownloading.get()) {
            this.statusTypeFluxSink.next(TorrentStatusType.PAUSE_DOWNLOAD);
        }
    }

    @Override
    public synchronized void pauseUpload() {
        if (this.isUploading.get() &&
                !this.isRemoved.get()) {
            this.statusTypeFluxSink.next(TorrentStatusType.PAUSE_UPLOAD);
        }
    }

    @Override
    public synchronized void remove() {
        if (!this.isRemoved.get()) {
            this.statusTypeFluxSink.next(TorrentStatusType.PAUSE_DOWNLOAD);
            this.statusTypeFluxSink.next(TorrentStatusType.PAUSE_UPLOAD);
            this.statusTypeFluxSink.next(TorrentStatusType.REMOVED);
        }
    }

    @Override
    public synchronized void completedDownloading() {
        if (this.isCompletedDownloading.compareAndSet(false, true) &&
                !this.isRemoved.get()) {
            this.statusTypeFluxSink.next(TorrentStatusType.PAUSE_DOWNLOAD);
            this.statusTypeFluxSink.next(TorrentStatusType.COMPLETED_DOWNLOADING);
        }
    }

    @Override
    public boolean isStarted() {
        return this.isStarted.get();
    }

    @Override
    public boolean isRemoved() {
        return this.isRemoved.get();
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
    public TorrentInfo getTorrentInfo() {
        return this.torrentInfo;
    }

    private void setCurrentState(TorrentStatusType torrentStatusType) {
        switch (torrentStatusType) {
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
            case REMOVED:
                this.isRemoved.set(true);
                break;
            case STARTED:
                this.isStarted.set(true);
                break;
            case COMPLETED_DOWNLOADING:
                this.isCompletedDownloading.set(true);
                break;
        }
    }
}
