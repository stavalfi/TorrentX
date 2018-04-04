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
import main.torrent.status.TorrentStatusType;
import main.tracker.TrackerConnection;
import main.tracker.TrackerProvider;
import reactor.core.publisher.ConnectableFlux;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

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
                        .getTorrentInfoHash()
                        .equals(torrentInfoHash))
                .findFirst();
    }

    public synchronized TorrentDownloader createDefaultTorrentDownloader(TorrentInfo torrentInfo, String downloadPath) {
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
                false,
                false);

        torrentStatusController.getStatusTypeFlux()
                .subscribe(new Consumer<TorrentStatusType>() {
                    private AtomicBoolean isConnected = new AtomicBoolean(false);

                    @Override
                    public synchronized void accept(TorrentStatusType torrentStatusType) {
                        switch (torrentStatusType) {
                            case START_DOWNLOAD:
                                if (!this.isConnected.get())
                                    peersCommunicatorFlux.connect();
                                break;
                        }
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

        return createTorrentDownloader(torrentInfo,
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
