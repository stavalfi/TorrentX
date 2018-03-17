package main.downloader;

import main.TorrentInfo;
import main.algorithms.BittorrentAlgorithmImpl;
import main.file.system.ActiveTorrents;
import main.file.system.DownloaderImpl;
import main.peer.PeersCommunicator;
import main.peer.PeersProvider;
import main.statistics.TorrentSpeedSpeedStatisticsImpl;
import main.tracker.TrackerConnection;
import main.tracker.TrackerProvider;
import reactor.core.publisher.ConnectableFlux;
import reactor.core.publisher.Flux;

public class DefaultTorrentDownloader extends TorrentDownloader {

    private TrackerProvider trackerProvider;
    private PeersProvider peersProvider;
    private ConnectableFlux<TrackerConnection> trackerConnectionFlux;
    private Flux<PeersCommunicator> peersCommunicatorFlux;

    private DefaultTorrentDownloader(TorrentInfo torrentInfo, String downloadPath,
                                     TrackerProvider trackerProvider,
                                     PeersProvider peersProvider,
                                     ConnectableFlux<TrackerConnection> trackerConnectionFlux,
                                     Flux<PeersCommunicator> peersCommunicatorFlux) {
        super(torrentInfo,
                new BittorrentAlgorithmImpl(torrentInfo, peersCommunicatorFlux),
                new DownloadControlImpl(torrentInfo, trackerConnectionFlux),
                new DownloaderImpl(ActiveTorrents.getInstance().createActiveTorrentMono(torrentInfo, downloadPath),
                        peersCommunicatorFlux.flatMap(peersCommunicator ->
                                peersCommunicator.receiveMessages.getPieceMessageResponseFlux())),
                new TorrentSpeedSpeedStatisticsImpl(torrentInfo, peersCommunicatorFlux.map(PeersCommunicator::getPeerSpeedStatistics)));
        this.trackerProvider = trackerProvider;
        this.peersProvider = peersProvider;
        this.trackerConnectionFlux = trackerConnectionFlux;
        this.peersCommunicatorFlux = peersCommunicatorFlux;
    }

    private DefaultTorrentDownloader(TorrentInfo torrentInfo, String downloadPath,
                                     TrackerProvider trackerProvider,
                                     PeersProvider peersProvider,
                                     ConnectableFlux<TrackerConnection> trackerConnectionFlux) {
        this(torrentInfo, downloadPath, trackerProvider, peersProvider, trackerConnectionFlux,
                peersProvider.getPeersCommunicatorFromTrackerFlux(trackerConnectionFlux));
    }

    private DefaultTorrentDownloader(TorrentInfo torrentInfo, String downloadPath,
                                     TrackerProvider trackerProvider) {
        this(torrentInfo, downloadPath, trackerProvider, new PeersProvider(torrentInfo),
                trackerProvider.connectToTrackersFlux());
    }

    public DefaultTorrentDownloader(TorrentInfo torrentInfo, String downloadPath) {
        this(torrentInfo, downloadPath, new TrackerProvider(torrentInfo));
    }
}
