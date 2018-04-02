package main.downloader;

import main.TorrentInfo;
import main.peer.PeersCommunicator;
import reactor.core.Disposable;
import reactor.core.publisher.ConnectableFlux;

import java.util.concurrent.atomic.AtomicBoolean;

public class DownloadControlImpl implements DownloadControl {

    private TorrentInfo torrentInfo;
    private AtomicBoolean isDownloading;
    private ConnectableFlux<PeersCommunicator> peersCommunicatorFlux;
    private Disposable connect;

    public DownloadControlImpl(TorrentInfo torrentInfo, ConnectableFlux<PeersCommunicator> peersCommunicatorFlux) {
        this.torrentInfo = torrentInfo;
        this.isDownloading = new AtomicBoolean(false);
        this.peersCommunicatorFlux = peersCommunicatorFlux;
    }


    @Override
    public void start() {
        this.isDownloading.set(true);
        if (this.connect == null)
            this.connect = peersCommunicatorFlux.connect();
    }

    @Override
    public void resume() {
        this.isDownloading.set(true);
    }

    @Override
    public void pause() {
        this.isDownloading.set(false);
    }

    @Override
    public void stop() {
        this.isDownloading.set(false);
    }

    @Override
    public void remove() {
        this.isDownloading.set(false);
    }

    @Override
    public boolean isDownloading() {
        return this.isDownloading.get();
    }

    @Override
    public TorrentInfo getTorrentInfo() {
        return torrentInfo;
    }
}
