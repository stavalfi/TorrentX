package main.downloader;

import main.TorrentInfo;
import main.peer.PeersCommunicator;
import main.peer.PeersProvider;
import main.peer.peerMessages.PeerMessage;
import main.tracker.TrackerConnection;
import main.tracker.TrackerProvider;
import reactor.core.publisher.ConnectableFlux;
import reactor.core.publisher.Flux;

public class TorrentDownloaderImpl extends TorrentDownloader {

    public TorrentDownloaderImpl(TorrentInfo torrentInfo, Downloader downloader, TrackerProvider trackerProvider, PeersProvider peersProvider, ConnectableFlux<TrackerConnection> trackerConnectionConnectableFlux, Flux<PeersCommunicator> peersCommunicatorFlux) {
        super(torrentInfo, downloader, trackerProvider, peersProvider, trackerConnectionConnectableFlux, peersCommunicatorFlux);
    }

    public TorrentDownloaderImpl(TorrentInfo torrentInfo, Downloader downloader, ConnectableFlux<TrackerConnection> trackerConnectionConnectableFlux, PeersProvider peersProvider, TrackerProvider trackerProvider) {
        super(torrentInfo, downloader, trackerConnectionConnectableFlux, peersProvider, trackerProvider);
    }

    public TorrentDownloaderImpl(TorrentInfo torrentInfo, Downloader downloader, TrackerProvider trackerProvider) {
        super(torrentInfo, downloader, trackerProvider);
    }

    public TorrentDownloaderImpl(TorrentInfo torrentInfo, Downloader downloader) {
        super(torrentInfo, downloader);
    }

    @Override
    public Flux<PeerMessage> getPeersMessagesFlux() {
        return super.getPeersCommunicatorFlux()
                .flatMap(PeersCommunicator::receive);
    }

    @Override
    public void start() {
        this.getTrackerConnectionConnectableFlux().connect();
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
}
