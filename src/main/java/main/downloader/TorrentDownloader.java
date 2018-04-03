package main.downloader;

import main.TorrentInfo;
import main.algorithms.BittorrentAlgorithm;
import main.algorithms.BittorrentAlgorithmImpl;
import main.file.system.ActiveTorrents;
import main.file.system.Downloader;
import main.peer.PeersCommunicator;
import main.peer.PeersListener;
import main.peer.PeersProvider;
import main.statistics.SpeedStatistics;
import main.statistics.TorrentSpeedSpeedStatisticsImpl;
import main.tracker.TrackerConnection;
import main.tracker.TrackerProvider;
import reactor.core.publisher.ConnectableFlux;
import reactor.core.publisher.Flux;

public class TorrentDownloader {

    private TorrentInfo torrentInfo;
    private Downloader downloader;
    private BittorrentAlgorithm bittorrentAlgorithm;
    private DownloadControl downloadControl;
    private SpeedStatistics torrentSpeedStatistics;

    private TrackerProvider trackerProvider;
    private PeersProvider peersProvider;
    private Flux<TrackerConnection> trackerConnectionFlux;
    private Flux<PeersCommunicator> peersCommunicatorFlux;

    public TorrentDownloader(TorrentInfo torrentInfo,
                             Downloader downloader,
                             BittorrentAlgorithm bittorrentAlgorithm,
                             DownloadControl downloadControl,
                             SpeedStatistics torrentSpeedStatistics,
                             TrackerProvider trackerProvider,
                             PeersProvider peersProvider,
                             Flux<TrackerConnection> trackerConnectionFlux,
                             Flux<PeersCommunicator> peersCommunicatorFlux) {
        this.torrentInfo = torrentInfo;
        this.downloader = downloader;
        this.bittorrentAlgorithm = bittorrentAlgorithm;
        this.downloadControl = downloadControl;
        this.torrentSpeedStatistics = torrentSpeedStatistics;
        this.trackerProvider = trackerProvider;
        this.peersProvider = peersProvider;
        this.trackerConnectionFlux = trackerConnectionFlux;
        this.peersCommunicatorFlux = peersCommunicatorFlux;
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

        DownloadControl downloadControl = new DownloadControlImpl(torrentInfo, peersCommunicatorFlux);

        BittorrentAlgorithm bittorrentAlgorithm =
                new BittorrentAlgorithmImpl(torrentInfo, downloadControl, peersCommunicatorFlux);

        Flux<SpeedStatistics> peerSpeedStatisticsFlux = peersCommunicatorFlux.map(PeersCommunicator::getPeerSpeedStatistics);

        SpeedStatistics torrentSpeedStatistics =
                new TorrentSpeedSpeedStatisticsImpl(torrentInfo, peerSpeedStatisticsFlux);

        Downloader downloader = ActiveTorrents.getInstance()
                .createActiveTorrentMono(torrentInfo, downloadPath,
                        bittorrentAlgorithm.receiveTorrentMessagesMessagesFlux().getPieceMessageResponseFlux())
                .block();

        return new TorrentDownloader(torrentInfo,
                downloader,
                bittorrentAlgorithm,
                downloadControl,
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