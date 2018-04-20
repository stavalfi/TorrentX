package main.downloader;

import main.TorrentInfo;
import main.algorithms.BittorrentAlgorithm;
import main.algorithms.impl.BittorrentAlgorithmInitializer;
import main.file.system.ActiveTorrents;
import main.file.system.TorrentFileSystemManager;
import main.peer.Link;
import main.peer.PeersListener;
import main.peer.PeersProvider;
import main.peer.ReceivePeerMessages;
import main.statistics.SpeedStatistics;
import main.statistics.TorrentSpeedSpeedStatisticsImpl;
import main.torrent.status.TorrentStatusController;
import main.torrent.status.TorrentStatusControllerImpl;
import main.tracker.TrackerConnection;
import main.tracker.TrackerProvider;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TorrentDownloaders {

    private List<TorrentDownloader> torrentDownloaderList = new ArrayList<>();

    public synchronized TorrentDownloader createTorrentDownloader(TorrentInfo torrentInfo,
                                                                  TorrentFileSystemManager torrentFileSystemManager,
                                                                  BittorrentAlgorithm bittorrentAlgorithm,
                                                                  TorrentStatusController torrentStatusController,
                                                                  SpeedStatistics torrentSpeedStatistics,
                                                                  TrackerProvider trackerProvider,
                                                                  PeersProvider peersProvider,
                                                                  Flux<TrackerConnection> trackerConnectionFlux,
                                                                  Flux<Link> peersCommunicatorFlux) {
        return findTorrentDownloader(torrentInfo.getTorrentInfoHash())
                .orElseGet(() -> {
                    TorrentDownloader torrentDownloader = new TorrentDownloader(torrentInfo,
                            torrentFileSystemManager,
                            bittorrentAlgorithm,
                            torrentStatusController,
                            torrentSpeedStatistics,
                            trackerProvider,
                            peersProvider,
                            trackerConnectionFlux,
                            peersCommunicatorFlux);

                    this.torrentDownloaderList.add(torrentDownloader);

                    return torrentDownloader;
                });
    }

    /**
     * This method is only for tests because if the client want to delete the torrent but not the file,
     * he can do that using TorrentStatusController::removeTorrent.
     * There is no reason to remove the TorrentDownloader object also.
     *
     * @param torrentInfoHash of torrent we need to delete it's TorrentDownload object
     * @return boolean which indicated if the deletion was successful.
     */
    public synchronized boolean deleteTorrentDownloader(String torrentInfoHash) {
        Optional<TorrentDownloader> torrentDownloaderOptional = findTorrentDownloader(torrentInfoHash);
        torrentDownloaderOptional.ifPresent(torrentDownloader ->
                this.torrentDownloaderList.remove(torrentDownloader));
        return torrentDownloaderOptional.isPresent();
    }

    public synchronized Optional<TorrentDownloader> findTorrentDownloader(String torrentInfoHash) {
        return this.torrentDownloaderList
                .stream()
                .filter(torrentDownloader -> torrentDownloader.getTorrentInfo()
                        .getTorrentInfoHash().toLowerCase().equals(torrentInfoHash.toLowerCase()))
                .findFirst();
    }

    public static TorrentDownloader createDefaultTorrentDownloader(TorrentInfo torrentInfo, String downloadPath) {
        TrackerProvider trackerProvider = new TrackerProvider(torrentInfo);
        PeersProvider peersProvider = new PeersProvider(torrentInfo);

        Flux<TrackerConnection> trackerConnectionConnectableFlux =
                trackerProvider.connectToTrackersFlux()
                        .autoConnect();

        PeersListener peersListener = new PeersListener();

        TorrentStatusController torrentStatusController =
                TorrentStatusControllerImpl.createDefaultTorrentStatusController(torrentInfo);

        Flux<Link> peersCommunicatorFlux =
                Flux.merge(torrentStatusController.isStartedDownloadingFlux(),
                        torrentStatusController.isStartedUploadingFlux())
                        .filter(isStarted -> isStarted)
                        .take(1)
                        .flatMap(__ ->
                                Flux.merge(peersListener.getPeersConnectedToMeFlux(),
                                        peersProvider.getPeersCommunicatorFromTrackerFlux(trackerConnectionConnectableFlux)
                                                .autoConnect()))
                        // multiple subscriptions will activate flatMap(__ -> multiple times and it will cause
                        // multiple calls to getPeersCommunicatorFromTrackerFlux which create new hot-flux
                        // every time and then I will connect to all the peers again and again...
                        .publish()
                        .autoConnect();

        TorrentFileSystemManager torrentFileSystemManager = ActiveTorrents.getInstance()
                .createActiveTorrentMono(torrentInfo, downloadPath, torrentStatusController,
                        peersCommunicatorFlux.map(Link::receivePeerMessages)
                                .flatMap(ReceivePeerMessages::getPieceMessageResponseFlux))
                .block();

        BittorrentAlgorithm bittorrentAlgorithm =
                BittorrentAlgorithmInitializer.v1(torrentInfo,
                        torrentStatusController,
                        torrentFileSystemManager,
                        peersCommunicatorFlux);

        SpeedStatistics torrentSpeedStatistics =
                new TorrentSpeedSpeedStatisticsImpl(torrentInfo,
                        peersCommunicatorFlux.map(Link::getPeerSpeedStatistics));

        return TorrentDownloaders.getInstance()
                .createTorrentDownloader(torrentInfo,
                        torrentFileSystemManager,
                        bittorrentAlgorithm,
                        torrentStatusController,
                        torrentSpeedStatistics,
                        trackerProvider,
                        peersProvider,
                        trackerConnectionConnectableFlux,
                        peersCommunicatorFlux);
    }

    private TorrentDownloaders() {
    }

    private static TorrentDownloaders instance = new TorrentDownloaders();

    public static TorrentDownloaders getInstance() {
        return instance;
    }
}
