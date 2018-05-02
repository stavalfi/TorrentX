package main.downloader;

import main.TorrentInfo;
import main.algorithms.BittorrentAlgorithm;
import main.algorithms.impls.BittorrentAlgorithmInitializer;
import main.file.system.ActiveTorrents;
import main.file.system.FileSystemLink;
import main.peer.Link;
import main.peer.PeersListener;
import main.peer.PeersProvider;
import main.peer.ReceiveMessagesNotifications;
import main.statistics.SpeedStatistics;
import main.statistics.TorrentSpeedSpeedStatisticsImpl;
import main.torrent.status.Status;
import main.torrent.status.StatusChanger;
import main.tracker.TrackerConnection;
import main.tracker.TrackerProvider;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class TorrentDownloaders {

    private List<TorrentDownloader> torrentDownloaderList = new ArrayList<>();

    public synchronized Flux<TorrentDownloader> getTorrentDownloadersFlux() {
        // TODO: I can't go over this list and delete it in the same time. I will get concurrenctodificationException.
        // I should not save this list in the first place. for now I will copy it.

        return Flux.fromIterable(this.torrentDownloaderList.stream().collect(Collectors.toList()));
    }

    public synchronized TorrentDownloader createTorrentDownloader(TorrentInfo torrentInfo,
                                                                  FileSystemLink fileSystemLink,
                                                                  BittorrentAlgorithm bittorrentAlgorithm,
                                                                  StatusChanger statusChanger,
                                                                  SpeedStatistics torrentSpeedStatistics,
                                                                  TrackerProvider trackerProvider,
                                                                  PeersProvider peersProvider,
                                                                  Flux<TrackerConnection> trackerConnectionFlux,
                                                                  Flux<Link> peersCommunicatorFlux) {
        return findTorrentDownloader(torrentInfo.getTorrentInfoHash())
                .orElseGet(() -> {
                    TorrentDownloader torrentDownloader = new TorrentDownloader(torrentInfo,
                            fileSystemLink,
                            bittorrentAlgorithm,
                            statusChanger,
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

        // TODO: save the initial status in the mongodb.
        StatusChanger statusChanger = new StatusChanger(new Status(
                false,
                false,
                false,
                false,
                false, false,
                false,
                false,
                false,
                false,
                false));

        PeersListener peersListener = new PeersListener(statusChanger);

        Flux<Link> searchingPeers$ = statusChanger.getStatus$()
                .filter(Status::isStartedSearchingPeers)
                .take(1)
                .flatMap(__ ->
                        peersProvider.getPeersCommunicatorFromTrackerFlux(trackerConnectionConnectableFlux)
                                .autoConnect(0));

        Flux<Link> peersCommunicatorFlux =
                Flux.merge(peersListener.getPeersConnectedToMeFlux(), searchingPeers$)
                        // multiple subscriptions will activate flatMap(__ -> multiple times and it will cause
                        // multiple calls to getPeersCommunicatorFromTrackerFlux which create new hot-flux
                        // every time and then I will connect to all the peers again and again...
                        .publish()
                        .autoConnect(0);

        FileSystemLink fileSystemLink = ActiveTorrents.getInstance()
                .createActiveTorrentMono(torrentInfo, downloadPath, statusChanger,
                        peersCommunicatorFlux.map(Link::receivePeerMessages)
                                .flatMap(ReceiveMessagesNotifications::getPieceMessageResponseFlux))
                .block();

        BittorrentAlgorithm bittorrentAlgorithm =
                BittorrentAlgorithmInitializer.v1(torrentInfo,
                        statusChanger,
                        fileSystemLink,
                        peersCommunicatorFlux);

        SpeedStatistics torrentSpeedStatistics =
                new TorrentSpeedSpeedStatisticsImpl(torrentInfo,
                        peersCommunicatorFlux.map(Link::getPeerSpeedStatistics));

        return TorrentDownloaders.getInstance()
                .createTorrentDownloader(torrentInfo,
                        fileSystemLink,
                        bittorrentAlgorithm,
                        statusChanger,
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
