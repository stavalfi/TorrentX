package main.downloader;

import main.TorrentInfo;
import main.peer.InitializePeersCommunication;
import main.peer.PeersCommunicator;
import main.peer.PeersProvider;
import main.peer.peerMessages.PeerMessage;
import main.tracker.TrackerConnection;
import main.tracker.TrackerProvider;
import reactor.core.publisher.ConnectableFlux;
import reactor.core.publisher.Flux;

public abstract class TorrentDownloader implements DownloadControl {

    private TorrentInfo torrentInfo;
    private Downloader downloader;
    private TrackerProvider trackerProvider;
    private PeersProvider peersProvider;
    private Flux<Double> torrentDownloadSpeedFlux;
    private Flux<Double> torrentUploadSpeedFlux;
    private Flux<PeersCommunicator> peersCommunicatorFlux;
    private ConnectableFlux<TrackerConnection> trackerConnectionConnectableFlux;

    public TorrentDownloader(TorrentInfo torrentInfo, Downloader downloader,
                             TrackerProvider trackerProvider, PeersProvider peersProvider,
                             ConnectableFlux<TrackerConnection> trackerConnectionConnectableFlux,
                             Flux<PeersCommunicator> peersCommunicatorFlux) {
        this.torrentInfo = torrentInfo;
        this.downloader = downloader;
        this.trackerProvider = trackerProvider;
        this.peersProvider = peersProvider;
        this.peersCommunicatorFlux = peersCommunicatorFlux;
        this.trackerConnectionConnectableFlux = trackerConnectionConnectableFlux;

        this.torrentDownloadSpeedFlux = this.peersCommunicatorFlux
                .flatMap(PeersCommunicator::getPeerDownloadSpeedFlux);

        this.torrentUploadSpeedFlux = this.peersCommunicatorFlux
                .flatMap(PeersCommunicator::getPeerUploadSpeedFlux);
    }

    public TorrentDownloader(TorrentInfo torrentInfo, Downloader downloader,
                             ConnectableFlux<TrackerConnection> trackerConnectionConnectableFlux,
                             PeersProvider peersProvider, TrackerProvider trackerProvider) {
        this(torrentInfo, downloader, trackerProvider, peersProvider, trackerConnectionConnectableFlux,
                peersProvider.connectToPeers(trackerConnectionConnectableFlux));
    }

    public TorrentDownloader(TorrentInfo torrentInfo, Downloader downloader, TrackerProvider trackerProvider) {
        this(torrentInfo,
                downloader,
                trackerProvider.connectToTrackers(),
                new PeersProvider(torrentInfo, trackerProvider, new InitializePeersCommunication(torrentInfo)),
                trackerProvider);
    }

    public TorrentDownloader(TorrentInfo torrentInfo, Downloader downloader) {
        this(torrentInfo, downloader, new TrackerProvider(torrentInfo));
    }

    @Override
    public TorrentInfo getTorrentInfo() {
        return torrentInfo;
    }

    public Downloader getDownloader() {
        return downloader;
    }

    protected Flux<PeersCommunicator> getPeersCommunicatorFlux() {
        return peersCommunicatorFlux;
    }

    public Flux<Double> getTorrentDownloadSpeedFlux() {
        return torrentDownloadSpeedFlux;
    }

    public Flux<Double> getTorrentUploadSpeedFlux() {
        return torrentUploadSpeedFlux;
    }

    public ConnectableFlux<TrackerConnection> getTrackerConnectionConnectableFlux() {
        return trackerConnectionConnectableFlux;
    }

    protected PeersProvider getPeersProvider() {
        return peersProvider;
    }

    protected TrackerProvider getTrackerProvider() {
        return trackerProvider;
    }

    public abstract Flux<PeerMessage> getPeersMessagesFlux();
}