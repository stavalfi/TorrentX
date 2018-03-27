package main.downloader;

import main.algorithms.BittorrentAlgorithm;
import main.algorithms.BittorrentAlgorithmImpl;
import main.file.system.ActiveTorrent;
import main.file.system.Downloader;
import main.file.system.DownloaderImpl;
import main.peer.PeersCommunicator;
import main.peer.PeersProvider;
import main.peer.ReceiveMessages;
import main.peer.peerMessages.PieceMessage;
import main.statistics.SpeedStatistics;
import main.statistics.TorrentSpeedSpeedStatisticsImpl;
import main.tracker.TrackerConnection;
import main.tracker.TrackerProvider;
import reactor.core.publisher.ConnectableFlux;
import reactor.core.publisher.Flux;

public class TorrentDownloader {

    private ActiveTorrent activeTorrent;
    private BittorrentAlgorithm bittorrentAlgorithm;
    private DownloadControl downloadControl;
    private Downloader downloader;
    private SpeedStatistics torrentSpeedStatistics;

    private TrackerProvider trackerProvider;
    private PeersProvider peersProvider;
    private Flux<TrackerConnection> trackerConnectionFlux;
    private Flux<PeersCommunicator> peersCommunicatorFlux;

    public TorrentDownloader(ActiveTorrent activeTorrent,
                             BittorrentAlgorithm bittorrentAlgorithm,
                             DownloadControl downloadControl,
                             Downloader downloader,
                             SpeedStatistics torrentSpeedStatistics,
                             TrackerProvider trackerProvider,
                             PeersProvider peersProvider,
                             Flux<TrackerConnection> trackerConnectionFlux,
                             Flux<PeersCommunicator> peersCommunicatorFlux) {
        this.activeTorrent = activeTorrent;
        this.bittorrentAlgorithm = bittorrentAlgorithm;
        this.downloadControl = downloadControl;
        this.downloader = downloader;
        this.torrentSpeedStatistics = torrentSpeedStatistics;
        this.trackerProvider = trackerProvider;
        this.peersProvider = peersProvider;
        this.trackerConnectionFlux = trackerConnectionFlux;
        this.peersCommunicatorFlux = peersCommunicatorFlux;
    }

    public ActiveTorrent getActiveTorrent() {
        return activeTorrent;
    }

    public BittorrentAlgorithm getBittorrentAlgorithm() {
        return bittorrentAlgorithm;
    }

    public DownloadControl getDownloadControl() {
        return downloadControl;
    }

    public Downloader getDownloader() {
        return downloader;
    }

    public SpeedStatistics getTorrentSpeedStatistics() {
        return torrentSpeedStatistics;
    }

    public TrackerProvider getTrackerProvider() {
        return trackerProvider;
    }

    public PeersProvider getPeersProvider() {
        return peersProvider;
    }

    public Flux<TrackerConnection> getTrackerConnectionFlux() {
        return trackerConnectionFlux;
    }

    public Flux<PeersCommunicator> getPeersCommunicatorFlux() {
        return peersCommunicatorFlux;
    }

    public static TorrentDownloader defaultTorrentDownloader(ActiveTorrent activeTorrent) {
        TrackerProvider trackerProvider = new TrackerProvider(activeTorrent);
        PeersProvider peersProvider = new PeersProvider(activeTorrent);
        ConnectableFlux<TrackerConnection> trackerConnectionConnectableFlux =
                trackerProvider.connectToTrackersFlux();
        Flux<PeersCommunicator> peersCommunicatorFlux =
                peersProvider.getPeersCommunicatorFromTrackerFlux(trackerConnectionConnectableFlux);

        BittorrentAlgorithm bittorrentAlgorithm =
                new BittorrentAlgorithmImpl(activeTorrent, peersCommunicatorFlux);
        DownloadControl downloadControl =
                new DownloadControlImpl(activeTorrent, trackerConnectionConnectableFlux);
        Flux<PieceMessage> peerResponsesFlux = peersCommunicatorFlux
                .map(PeersCommunicator::receivePeerMessages)
                .flatMap(ReceiveMessages::getPieceMessageResponseFlux);
        Downloader downloader = new DownloaderImpl(activeTorrent, peerResponsesFlux);
        Flux<SpeedStatistics> peerSpeedStatisticsFlux = peersCommunicatorFlux.map(PeersCommunicator::getPeerSpeedStatistics);
        SpeedStatistics torrentSpeedStatistics =
                new TorrentSpeedSpeedStatisticsImpl(activeTorrent, peerSpeedStatisticsFlux);

        return new TorrentDownloader(activeTorrent,
                bittorrentAlgorithm,
                downloadControl,
                downloader,
                torrentSpeedStatistics,
                trackerProvider,
                peersProvider,
                trackerConnectionConnectableFlux,
                peersCommunicatorFlux);
    }

}