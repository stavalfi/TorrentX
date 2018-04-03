package main.downloader;

import main.TorrentInfo;
import main.algorithms.BittorrentAlgorithm;
import main.algorithms.BittorrentAlgorithmImpl;
import main.file.system.ActiveTorrents;
import main.file.system.TorrentFileSystemManager;
import main.peer.PeersCommunicator;
import main.peer.PeersListener;
import main.peer.PeersProvider;
import main.statistics.SpeedStatistics;
import main.statistics.TorrentSpeedSpeedStatisticsImpl;
import main.torrent.status.TorrentStatusController;
import main.torrent.status.TorrentStatusControllerImpl;
import main.tracker.TrackerConnection;
import main.tracker.TrackerProvider;
import reactor.core.publisher.ConnectableFlux;
import reactor.core.publisher.Flux;

public class TorrentDownloader {

    private TorrentInfo torrentInfo;
    private TorrentFileSystemManager torrentFileSystemManager;
    private BittorrentAlgorithm bittorrentAlgorithm;
    private TorrentStatusController torrentStatusController;
    private SpeedStatistics torrentSpeedStatistics;

    private TrackerProvider trackerProvider;
    private PeersProvider peersProvider;
    private Flux<TrackerConnection> trackerConnectionFlux;
    private Flux<PeersCommunicator> peersCommunicatorFlux;

    public TorrentDownloader(TorrentInfo torrentInfo,
                             TorrentFileSystemManager torrentFileSystemManager,
                             BittorrentAlgorithm bittorrentAlgorithm,
                             TorrentStatusController torrentStatusController,
                             SpeedStatistics torrentSpeedStatistics,
                             TrackerProvider trackerProvider,
                             PeersProvider peersProvider,
                             Flux<TrackerConnection> trackerConnectionFlux,
                             Flux<PeersCommunicator> peersCommunicatorFlux) {
        this.torrentInfo = torrentInfo;
        this.torrentFileSystemManager = torrentFileSystemManager;
        this.bittorrentAlgorithm = bittorrentAlgorithm;
        this.torrentStatusController = torrentStatusController;
        this.torrentSpeedStatistics = torrentSpeedStatistics;
        this.trackerProvider = trackerProvider;
        this.peersProvider = peersProvider;
        this.trackerConnectionFlux = trackerConnectionFlux;
        this.peersCommunicatorFlux = peersCommunicatorFlux;
    }

    public BittorrentAlgorithm getBittorrentAlgorithm() {
        return bittorrentAlgorithm;
    }

    public TorrentStatusController getTorrentStatusController() {
        return torrentStatusController;
    }

    public TorrentFileSystemManager getTorrentFileSystemManager() {
        return torrentFileSystemManager;
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

    public static TorrentDownloader defaultTorrentDownloader(TorrentInfo torrentInfo, String downloadPath) {
        TrackerProvider trackerProvider = new TrackerProvider(torrentInfo);
        PeersProvider peersProvider = new PeersProvider(torrentInfo);

        Flux<TrackerConnection> trackerConnectionConnectableFlux =
                trackerProvider.connectToTrackersFlux()
                        .autoConnect();

        ConnectableFlux<PeersCommunicator> peersCommunicatorFlux =
                Flux.merge(PeersListener.getInstance().getPeersConnectedToMeFlux().autoConnect(),
                        peersProvider.getPeersCommunicatorFromTrackerFlux(trackerConnectionConnectableFlux).autoConnect())
                        .publish();

        TorrentStatusController torrentStatusController = new TorrentStatusControllerImpl(torrentInfo,
                false,
                false,
                false,
                false,
                false,
                false);

        torrentStatusController.getStatusTypeFlux()
                .subscribe(torrentStatusType -> {
                    switch (torrentStatusType) {
                        case STARTED:
                            peersCommunicatorFlux.connect();
                            break;
                    }
                });

        BittorrentAlgorithm bittorrentAlgorithm =
                new BittorrentAlgorithmImpl(torrentInfo, torrentStatusController, peersCommunicatorFlux);

        TorrentFileSystemManager torrentFileSystemManager = ActiveTorrents.getInstance()
                .createActiveTorrentMono(torrentInfo, downloadPath, torrentStatusController,
                        bittorrentAlgorithm.receiveTorrentMessagesMessagesFlux().getPieceMessageResponseFlux())
                .block();

        SpeedStatistics torrentSpeedStatistics =
                new TorrentSpeedSpeedStatisticsImpl(torrentInfo,
                        peersCommunicatorFlux.map(PeersCommunicator::getPeerSpeedStatistics));

        return new TorrentDownloader(torrentInfo,
                torrentFileSystemManager,
                bittorrentAlgorithm,
                torrentStatusController,
                torrentSpeedStatistics,
                trackerProvider,
                peersProvider,
                trackerConnectionConnectableFlux,
                peersCommunicatorFlux);
    }

    public TorrentInfo getTorrentInfo() {
        return torrentInfo;
    }
}