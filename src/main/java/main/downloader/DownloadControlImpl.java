package main.downloader;

import main.TorrentInfo;
import main.tracker.TrackerConnection;
import reactor.core.publisher.ConnectableFlux;

public class DownloadControlImpl implements DownloadControl {

    private TorrentInfo torrentInfo;
    private ConnectableFlux<TrackerConnection> trackerConnectionConnectableFlux;

    public DownloadControlImpl(TorrentInfo torrentInfo, ConnectableFlux<TrackerConnection> trackerConnectionConnectableFlux) {
        this.torrentInfo = torrentInfo;
        this.trackerConnectionConnectableFlux = trackerConnectionConnectableFlux;
    }


    @Override
    public void start() {
        this.trackerConnectionConnectableFlux.connect();
    }

    @Override
    public void resume() {

    }

    @Override
    public void pause() {

    }

    @Override
    public void stop() {

    }

    @Override
    public void remove() {

    }

    @Override
    public TorrentInfo getTorrentInfo() {
        return torrentInfo;
    }
}
