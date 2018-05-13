package main.downloader;

import main.TorrentInfo;
import main.algorithms.BittorrentAlgorithm;
import main.file.system.FileSystemLink;
import main.peer.Link;
import main.peer.PeersProvider;
import main.statistics.SpeedStatistics;
import main.torrent.status.StatusChanger;
import main.tracker.TrackerConnection;
import main.tracker.TrackerProvider;
import reactor.core.publisher.Flux;

public class TorrentDownloader {

    private TorrentInfo torrentInfo;
    private FileSystemLink fileSystemLink;
    private BittorrentAlgorithm bittorrentAlgorithm;
    private StatusChanger statusChanger;
    private SpeedStatistics torrentSpeedStatistics;

    private TrackerProvider trackerProvider;
    private PeersProvider peersProvider;
    private Flux<TrackerConnection> trackerConnectionFlux;
    private Flux<Link> peersCommunicatorFlux;

    public TorrentDownloader(TorrentInfo torrentInfo,
                             FileSystemLink fileSystemLink,
                             BittorrentAlgorithm bittorrentAlgorithm,
                             StatusChanger statusChanger,
                             SpeedStatistics torrentSpeedStatistics,
                             TrackerProvider trackerProvider,
                             PeersProvider peersProvider,
                             Flux<TrackerConnection> trackerConnectionFlux,
                             Flux<Link> peersCommunicatorFlux) {
        this.torrentInfo = torrentInfo;
        this.fileSystemLink = fileSystemLink;
        this.bittorrentAlgorithm = bittorrentAlgorithm;
        this.statusChanger = statusChanger;
        this.torrentSpeedStatistics = torrentSpeedStatistics;
        this.trackerProvider = trackerProvider;
        this.peersProvider = peersProvider;
        this.trackerConnectionFlux = trackerConnectionFlux;
        this.peersCommunicatorFlux = peersCommunicatorFlux;
    }

    public BittorrentAlgorithm getBittorrentAlgorithm() {
        return bittorrentAlgorithm;
    }

    public StatusChanger getStatusChanger() {
        return statusChanger;
    }

    public FileSystemLink getFileSystemLink() {
        return fileSystemLink;
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