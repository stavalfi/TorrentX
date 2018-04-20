package main.downloader;

import main.TorrentInfo;
import main.algorithms.BittorrentAlgorithm;
import main.file.system.TorrentFileSystemManager;
import main.peer.Link;
import main.peer.PeersProvider;
import main.statistics.SpeedStatistics;
import main.torrent.status.TorrentStatusController;
import main.tracker.TrackerConnection;
import main.tracker.TrackerProvider;
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
    private Flux<Link> peersCommunicatorFlux;

    public TorrentDownloader(TorrentInfo torrentInfo,
                             TorrentFileSystemManager torrentFileSystemManager,
                             BittorrentAlgorithm bittorrentAlgorithm,
                             TorrentStatusController torrentStatusController,
                             SpeedStatistics torrentSpeedStatistics,
                             TrackerProvider trackerProvider,
                             PeersProvider peersProvider,
                             Flux<TrackerConnection> trackerConnectionFlux,
                             Flux<Link> peersCommunicatorFlux) {
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

    public Flux<Link> getPeersCommunicatorFlux() {
        return peersCommunicatorFlux;
    }

    public TorrentInfo getTorrentInfo() {
        return torrentInfo;
    }
}