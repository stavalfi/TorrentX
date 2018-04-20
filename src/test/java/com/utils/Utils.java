package com.utils;

import christophedetroyer.torrent.TorrentFile;
import christophedetroyer.torrent.TorrentParser;
import lombok.SneakyThrows;
import main.TorrentInfo;
import main.algorithms.BittorrentAlgorithm;
import main.algorithms.impls.BittorrentAlgorithmInitializer;
import main.downloader.TorrentDownloader;
import main.downloader.TorrentDownloaders;
import main.file.system.ActiveTorrentFile;
import main.file.system.ActiveTorrents;
import main.file.system.TorrentFileSystemManager;
import main.peer.*;
import main.peer.peerMessages.PeerMessage;
import main.peer.peerMessages.RequestMessage;
import main.statistics.SpeedStatistics;
import main.statistics.TorrentSpeedSpeedStatisticsImpl;
import main.torrent.status.TorrentStatusController;
import main.torrent.status.TorrentStatusControllerImpl;
import main.tracker.TrackerConnection;
import main.tracker.TrackerProvider;
import reactor.core.publisher.ConnectableFlux;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.SocketException;
import java.util.*;
import java.util.stream.Collectors;

public class Utils {
    public static PeersListener peersListener;

    public static TorrentInfo createTorrentInfo(String torrentFilePath) throws IOException {
        String torrentFilesPath = "src/test/resources/torrents/" + torrentFilePath;
        return new TorrentInfo(torrentFilesPath, TorrentParser.parseTorrent(torrentFilesPath));
    }

    public static void removeEverythingRelatedToTorrent(TorrentInfo torrentInfo) {
        TorrentDownloaders.getInstance()
                .findTorrentDownloader(torrentInfo.getTorrentInfoHash())
                .map(TorrentDownloader::getTorrentStatusController)
                .ifPresent(torrentStatusController -> {
                    torrentStatusController.pauseDownload();
                    torrentStatusController.pauseUpload();
                    torrentStatusController.removeFiles();
                    torrentStatusController.removeTorrent();
                });

        if (peersListener != null) {
            try {
                peersListener.stopListenForNewPeers();
            } catch (IOException e) {

            }
            peersListener = null;
        }

        TorrentDownloaders.getInstance()
                .deleteTorrentDownloader(torrentInfo.getTorrentInfoHash());

        // some tests directly create ActiveTorrent object without creating
        // TorrentDownloader object so we must remove ActiveTorrent also.
        ActiveTorrents.getInstance()
                .findActiveTorrentByHashMono(torrentInfo.getTorrentInfoHash())
                .filter(Optional::isPresent)
                .map(Optional::get)
                .flatMap(activeTorrent -> activeTorrent
                        .deleteFileOnlyMono(torrentInfo.getTorrentInfoHash())
                        .flatMap(isDeleted -> activeTorrent
                                .deleteActiveTorrentOnlyMono(torrentInfo.getTorrentInfoHash())))
                .subscribe();

        // delete download folder from last test
        Utils.deleteDownloadFolder();
    }

    public static TorrentDownloader createDefaultTorrentDownloader(TorrentInfo torrentInfo, String downloadPath) {
        return createDefaultTorrentDownloader(torrentInfo, downloadPath,
                TorrentStatusControllerImpl.createDefaultTorrentStatusController(torrentInfo));
    }

    public static TorrentDownloader createDefaultTorrentDownloader(TorrentInfo torrentInfo, String downloadPath,
                                                                   TorrentStatusController torrentStatusController) {
        TrackerProvider trackerProvider = new TrackerProvider(torrentInfo);
        PeersProvider peersProvider = new PeersProvider(torrentInfo);

        Flux<TrackerConnection> trackerConnectionConnectableFlux =
                trackerProvider.connectToTrackersFlux()
                        .autoConnect();

        peersListener = new PeersListener();

        Flux<Link> peersCommunicatorFlux =
                Flux.merge(torrentStatusController.isStartedDownloadingFlux(),
                        torrentStatusController.isStartedUploadingFlux())
                        .filter(isStarted -> isStarted)
                        .take(1)
                        .flatMap(__ ->
                                Flux.merge(peersListener.getPeersConnectedToMeFlux()
                                                .autoConnect(),
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

    public static TorrentDownloader createCustomTorrentDownloader(TorrentInfo torrentInfo,
                                                                  TorrentFileSystemManager torrentFileSystemManager,
                                                                  Flux<TrackerConnection> trackerConnectionConnectableFlux) {
        TrackerProvider trackerProvider = new TrackerProvider(torrentInfo);
        PeersProvider peersProvider = new PeersProvider(torrentInfo);

        ConnectableFlux<Link> peersCommunicatorFromTrackerFlux =
                peersProvider.getPeersCommunicatorFromTrackerFlux(trackerConnectionConnectableFlux);

        peersListener = new PeersListener();

        TorrentStatusController torrentStatusController =
                TorrentStatusControllerImpl.createDefaultTorrentStatusController(torrentInfo);

        Flux<Link> peersCommunicatorFlux =
                Flux.merge(torrentStatusController.isStartedDownloadingFlux(),
                        torrentStatusController.isStartedUploadingFlux())
                        .filter(isStarted -> isStarted)
                        .take(1)
                        .flatMap(__ ->
                                Flux.merge(peersListener.getPeersConnectedToMeFlux()
                                                .autoConnect()
                                                // SocketException == When I shutdown the SocketServer after/before
                                                // the tests inside Utils::removeEverythingRelatedToTorrent.
                                                .onErrorResume(SocketException.class, throwable -> Flux.empty()),
                                        peersProvider.getPeersCommunicatorFromTrackerFlux(trackerConnectionConnectableFlux)
                                                .autoConnect()))
                        // multiple subscriptions will activate flatMap(__ -> multiple times and it will cause
                        // multiple calls to getPeersCommunicatorFromTrackerFlux which create new hot-flux
                        // every time and then I will connect to all the peers again and again...
                        .publish()
                        .autoConnect();

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

    public static Mono<SendPeerMessages> sendFakeMessage(Link link, PeerMessageType peerMessageType) {
        switch (peerMessageType) {
            case HaveMessage:
                return link.sendMessages().sendHaveMessage(0);
            case PortMessage:
                return link.sendMessages().sendPortMessage((short) link.getMe().getPeerPort());
            case ChokeMessage:
                return link.sendMessages().sendChokeMessage();
            case PieceMessage:
                return link.sendMessages().sendPieceMessage(0, 0, new byte[10]);
            case CancelMessage:
                return link.sendMessages().sendCancelMessage(0, 0, 10);
            case KeepAliveMessage:
                return link.sendMessages().sendKeepAliveMessage();
            case RequestMessage:
                return link.sendMessages().sendRequestMessage(0, 0, 10);
            case UnchokeMessage:
                return link.sendMessages().sendUnchokeMessage();
            case BitFieldMessage:
                return link.sendMessages().sendBitFieldMessage(BitSet.valueOf(new byte[10]));
            case InterestedMessage:
                return link.sendMessages().sendInterestedMessage();
            case NotInterestedMessage:
                return link.sendMessages().sendNotInterestedMessage();
            default:
                throw new IllegalArgumentException(peerMessageType.toString());
        }
    }

    public static Flux<? extends PeerMessage> getSpecificMessageResponseFluxByMessageType(Link link, PeerMessageType peerMessageType) {
        switch (peerMessageType) {
            case HaveMessage:
                return link.receivePeerMessages().getHaveMessageResponseFlux();
            case PortMessage:
                return link.receivePeerMessages().getPortMessageResponseFlux();
            case ChokeMessage:
                return link.receivePeerMessages().getChokeMessageResponseFlux();
            case PieceMessage:
                return link.receivePeerMessages().getPieceMessageResponseFlux();
            case CancelMessage:
                return link.receivePeerMessages().getCancelMessageResponseFlux();
            case KeepAliveMessage:
                return link.receivePeerMessages().getKeepMessageResponseFlux();
            case RequestMessage:
                return link.receivePeerMessages().getRequestMessageResponseFlux();
            case UnchokeMessage:
                return link.receivePeerMessages().getUnchokeMessageResponseFlux();
            case BitFieldMessage:
                return link.receivePeerMessages().getBitFieldMessageResponseFlux();
            case InterestedMessage:
                return link.receivePeerMessages().getInterestedMessageResponseFlux();
            case NotInterestedMessage:
                return link.receivePeerMessages().getNotInterestedMessageResponseFlux();
            case ExtendedMessage:
                return link.receivePeerMessages().getExtendedMessageResponseFlux();
            default:
                throw new IllegalArgumentException(peerMessageType.toString());
        }
    }

    public static long folderSize(File directory) {
        long length = 0;
        for (File file : Objects.requireNonNull(directory.listFiles())) {
            if (file.isFile())
                length += file.length();
            else
                length += folderSize(file);
        }
        return length;
    }

    @SneakyThrows
    public static byte[] readFromFile(TorrentInfo torrentInfo, String downloadPath, RequestMessage requestMessage) {
        List<TorrentFile> fileList = torrentInfo.getFileList();

        List<ActiveTorrentFile> activeTorrentFileList = new ArrayList<>();
        String fullFilePath = downloadPath;
        if (!torrentInfo.isSingleFileTorrent())
            fullFilePath += torrentInfo.getName() + "/";
        long position = 0;
        for (TorrentFile torrentFile : fileList) {
            String completeFilePath = torrentFile.getFileDirs()
                    .stream()
                    .collect(Collectors.joining("/", fullFilePath, ""));
            long from = position;
            long to = position + torrentFile.getFileLength();
            position = to;

            ActiveTorrentFile activeTorrentFile = new ActiveTorrentFile(completeFilePath, from, to);
            activeTorrentFileList.add(activeTorrentFile);
        }

        // read from the file

        byte[] result = new byte[requestMessage.getBlockLength()];
        int resultFreeIndex = 0;
        long from = requestMessage.getIndex() * torrentInfo.getPieceLength() + requestMessage.getBegin();
        long to = requestMessage.getIndex() * torrentInfo.getPieceLength() + requestMessage.getBegin() + requestMessage.getBlockLength();

        for (ActiveTorrentFile activeTorrentFile : activeTorrentFileList) {
            if (activeTorrentFile.getFrom() <= from && from <= activeTorrentFile.getTo()) {
                RandomAccessFile randomAccessFile = new RandomAccessFile(activeTorrentFile.getFilePath(), "rw");
                randomAccessFile.seek(from);
                if (activeTorrentFile.getTo() < to) {
                    byte[] tempResult = new byte[(int) (activeTorrentFile.getTo() - from)];
                    randomAccessFile.read(tempResult);
                    for (byte aTempResult : tempResult)
                        result[resultFreeIndex++] = aTempResult;
                } else {
                    byte[] tempResult = new byte[(int) (to - from)];
                    randomAccessFile.read(tempResult);
                    for (byte aTempResult : tempResult)
                        result[resultFreeIndex++] = aTempResult;
                    return result;
                }
            }
        }
        throw new Exception("we shouldn't be here - never!");
    }

    public static void deleteDownloadFolder() {
        // delete download folder
        try {
            File file = new File(System.getProperty("user.dir") + "/torrents-test/");
            if (file.exists()) {
                boolean deleted = deleteDirectory(file);
                if (!deleted)
                    System.out.println("could not delete torrent-test folder: " +
                            System.getProperty("user.dir") + "/torrents-test");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static boolean deleteDirectory(File directoryToBeDeleted) {
        File[] allContents = directoryToBeDeleted.listFiles();
        if (allContents != null) {
            for (File file : allContents) {
                if (!deleteDirectory(file)) {
                    System.out.println("could not delete: " +
                            file.getAbsolutePath());
                    return false;
                }
            }
        }
        return directoryToBeDeleted.delete();
    }
}
