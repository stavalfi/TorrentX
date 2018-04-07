package main.downloader;

import main.TorrentInfo;
import main.algorithms.BittorrentAlgorithm;
import main.algorithms.BittorrentAlgorithmImpl;
import main.file.system.ActiveTorrents;
import main.file.system.TorrentFileSystemManager;
import main.peer.*;
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
                                                                  Flux<PeersCommunicator> peersCommunicatorFlux) {
        Optional<TorrentDownloader> torrentDownloader1 = findTorrentDownloader(torrentInfo.getTorrentInfoHash());
        if (torrentDownloader1.isPresent())
            return torrentDownloader1.get();

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

        Flux<PeersCommunicator> peersCommunicatorFlux =
                Flux.merge(peersListener.getPeersConnectedToMeFlux()
                                .autoConnect(),
                        peersProvider.getPeersCommunicatorFromTrackerFlux(trackerConnectionConnectableFlux)
                                .autoConnect());

        TorrentStatusController torrentStatusController = new TorrentStatusControllerImpl(torrentInfo,
                false,
                false,
                false,
                false,
                false,
                false,
                false);

        TorrentFileSystemManager torrentFileSystemManager = ActiveTorrents.getInstance()
                .createActiveTorrentMono(torrentInfo, downloadPath, torrentStatusController,
                        peersCommunicatorFlux.map(PeersCommunicator::receivePeerMessages)
                                .flatMap(ReceiveMessages::getPieceMessageResponseFlux))
                .block();

        BittorrentAlgorithm bittorrentAlgorithm =
                new BittorrentAlgorithmImpl(torrentInfo,
                        torrentStatusController,
                        torrentFileSystemManager,
                        peersCommunicatorFlux);

        SpeedStatistics torrentSpeedStatistics =
                new TorrentSpeedSpeedStatisticsImpl(torrentInfo,
                        peersCommunicatorFlux.map(PeersCommunicator::getPeerSpeedStatistics));

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
