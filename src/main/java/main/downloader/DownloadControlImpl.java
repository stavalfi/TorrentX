package main.downloader;

import main.TorrentInfo;
import main.peer.PeersCommunicator;
import reactor.core.Disposable;
import reactor.core.publisher.ConnectableFlux;

import java.util.concurrent.atomic.AtomicBoolean;

public class DownloadControlImpl implements DownloadControl {

    private TorrentInfo torrentInfo;
    private AtomicBoolean isDownloading;
    private AtomicBoolean isStarted;
    private AtomicBoolean isRemoved;
    private ConnectableFlux<PeersCommunicator> peersCommunicatorFlux;
    private Disposable connect;

    public DownloadControlImpl(TorrentInfo torrentInfo, ConnectableFlux<PeersCommunicator> peersCommunicatorFlux) {
        this.torrentInfo = torrentInfo;
        this.isDownloading = new AtomicBoolean(false);
        this.isStarted = new AtomicBoolean(false);
        this.isRemoved = new AtomicBoolean(false);
        this.peersCommunicatorFlux = peersCommunicatorFlux;
    }


    @Override
    public synchronized void start() {
        this.isDownloading.set(true);
        this.isStarted.set(true);
        if (this.connect == null)
            this.connect = peersCommunicatorFlux.connect();
    }

    @Override
    public synchronized void resume() {
        this.isDownloading.set(true);
    }

    @Override
    public synchronized void pause() {
        this.isDownloading.set(false);
    }

    @Override
    public synchronized void remove() {
        this.isDownloading.set(false);
        this.isRemoved.set(true);
    }

    @Override
    public synchronized boolean isDownloading() {
        return this.isDownloading.get();
    }

    @Override
    public TorrentInfo getTorrentInfo() {
        return torrentInfo;
    }
}
