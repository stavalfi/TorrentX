package com.utils;

import christophedetroyer.torrent.TorrentFile;
import christophedetroyer.torrent.TorrentParser;
import main.TorrentInfo;
import main.algorithms.BittorrentAlgorithm;
import main.algorithms.impls.BittorrentAlgorithmInitializer;
import main.downloader.PieceEvent;
import main.downloader.TorrentDownloader;
import main.downloader.TorrentDownloaders;
import main.file.system.*;
import main.peer.*;
import main.peer.peerMessages.PeerMessage;
import main.peer.peerMessages.PieceMessage;
import main.peer.peerMessages.RequestMessage;
import main.statistics.SpeedStatistics;
import main.statistics.TorrentSpeedSpeedStatisticsImpl;
import main.torrent.status.Action;
import main.torrent.status.TorrentStatusStore;
import main.torrent.status.reducers.Reducer;
import main.torrent.status.state.tree.DownloadState;
import main.torrent.status.state.tree.PeersState;
import main.torrent.status.state.tree.TorrentFileSystemState;
import main.torrent.status.state.tree.TorrentStatusState;
import main.tracker.TrackerConnection;
import main.tracker.TrackerProvider;
import org.junit.Assert;
import reactor.core.publisher.ConnectableFlux;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.File;
import java.io.IOException;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.Semaphore;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;

public class Utils {
    public static PeersListener peersListener;

    public static TorrentInfo createTorrentInfo(String torrentFilePath) throws IOException {
        String torrentFilesPath = "src" + File.separator +
                "test" + File.separator +
                "resources" + File.separator +
                "torrents" + File.separator +
                torrentFilePath;
        return new TorrentInfo(torrentFilesPath, TorrentParser.parseTorrent(torrentFilesPath));
    }

    public static void removeEverythingRelatedToLastTest() {

        BlocksAllocatorImpl.getInstance().freeAll();

        Mono<List<FileSystemLink>> activeTorrentsListMono = ActiveTorrents.getInstance()
                .getActiveTorrentsFlux()
                .flatMap(activeTorrent -> activeTorrent.deleteFileOnlyMono()
                        // if the test deleted the files then I will get NoSuchFileException and we will not delete the FileSystemLinkImpl object.
                        .onErrorResume(Throwable.class, throwable -> Mono.just(activeTorrent)))
                .flatMap(activeTorrent -> activeTorrent.deleteActiveTorrentOnlyMono()
                        // if the test deleted the FileSystemLinkImpl object then I may get an exception which I clearly don't want.
                        .onErrorResume(Throwable.class, throwable -> Mono.just(activeTorrent)))

                .collectList();
        try {
            activeTorrentsListMono.block();
        } catch (Exception e) {
            //e.printStackTrace();
        }


        Mono<List<TorrentStatusStore>> torrentDownloadersListMono = TorrentDownloaders.getInstance()
                .getTorrentDownloadersFlux()
                .doOnNext(torrentDownloader -> TorrentDownloaders.getInstance().deleteTorrentDownloader(torrentDownloader.getTorrentInfo().getTorrentInfoHash()))
                .map(TorrentDownloader::getTorrentStatusStore)
                .doOnNext(statusChanger -> {
                    // TODO: uncomment
//                    statusChanger.changeState(Action.PAUSE_LISTENING_TO_INCOMING_PEERS).block();
//                    statusChanger.changeState(Action.PAUSE_SEARCHING_PEERS).block();
//                    statusChanger.changeState(Action.PAUSE_DOWNLOAD).block();
//                    statusChanger.changeState(Action.PAUSE_UPLOAD).block();
                })
                .collectList();
        try {
            torrentDownloadersListMono.block();
        } catch (Exception e) {
            //e.printStackTrace();
        }
        // TODO: uncomment
//        if (peersListener != null) {
//            try {
////                peersListener.stopListenForNewPeers();
//            } catch (IOException e) {
//
//            }
//            peersListener = null;
//        }

        // delete download folder from last test
        Utils.deleteDownloadFolder();

        BlocksAllocatorImpl.getInstance()
                .freeAll()
                .flatMap(__ -> BlocksAllocatorImpl.getInstance().updateAllocations(2, 1_000_000))
                .block();
    }

    public static TorrentStatusState getTorrentStatusState(TorrentInfo torrentInfo, Action lastAction, List<Action> actions) {
        DownloadState downloadState = DownloadState.DownloadStateBuilder.builder()
                .setStartDownloadInProgress(actions.contains(Action.START_DOWNLOAD_IN_PROGRESS))
                .setStartDownloadWindUp(actions.contains(Action.START_DOWNLOAD_WIND_UP))
                .setResumeDownloadInProgress(actions.contains(Action.RESUME_DOWNLOAD_IN_PROGRESS))
                .setResumeDownloadWindUp(actions.contains(Action.RESUME_DOWNLOAD_WIND_UP))
                .setPauseDownloadInProgress(actions.contains(Action.PAUSE_DOWNLOAD_IN_PROGRESS))
                .setPauseDownloadWindUp(actions.contains(Action.PAUSE_DOWNLOAD_WIND_UP))
                .setCompletedDownloadingInProgress(actions.contains(Action.COMPLETED_DOWNLOADING_IN_PROGRESS))
                .setCompletedDownloadingWindUp(actions.contains(Action.COMPLETED_DOWNLOADING_WIND_UP))
                .setStartUploadInProgress(actions.contains(Action.START_UPLOAD_IN_PROGRESS))
                .setStartUploadWindUp(actions.contains(Action.START_UPLOAD_WIND_UP))
                .setResumeUploadInProgress(actions.contains(Action.RESUME_UPLOAD_IN_PROGRESS))
                .setResumeUploadWindUp(actions.contains(Action.RESUME_UPLOAD_WIND_UP))
                .setPauseUploadInProgress(actions.contains(Action.PAUSE_UPLOAD_IN_PROGRESS))
                .setPauseUploadWindUp(actions.contains(Action.PAUSE_UPLOAD_WIND_UP))
                .build();

        PeersState peersState = PeersState.PeersStateBuilder.builder()
                .setStartedSearchingPeersInProgress(actions.contains(Action.START_SEARCHING_PEERS_IN_PROGRESS))
                .setStartedSearchingPeersSelfResolved(actions.contains(Action.START_SEARCHING_PEERS_SELF_RESOLVED))
                .setStartedSearchingPeersWindUp(actions.contains(Action.START_SEARCHING_PEERS_WIND_UP))
                .setPauseSearchingPeersInProgress(actions.contains(Action.PAUSE_SEARCHING_PEERS_IN_PROGRESS))
                .setPauseSearchingPeersSelfResolved(actions.contains(Action.PAUSE_SEARCHING_PEERS_SELF_RESOLVED))
                .setPauseSearchingPeersWindUp(actions.contains(Action.PAUSE_SEARCHING_PEERS_WIND_UP))
                .setResumeSearchingPeersInProgress(actions.contains(Action.RESUME_SEARCHING_PEERS_IN_PROGRESS))
                .setResumeSearchingPeersSelfResolved(actions.contains(Action.RESUME_SEARCHING_PEERS_SELF_RESOLVED))
                .setResumeSearchingPeersWindUp(actions.contains(Action.RESUME_SEARCHING_PEERS_WIND_UP))
                .build();

        TorrentFileSystemState torrentFileSystemState = TorrentFileSystemState.TorrentFileSystemStateBuilder.builder()
                .setFilesRemovedInProgress(actions.contains(Action.REMOVE_FILES_IN_PROGRESS))
                .setFilesRemovedWindUp(actions.contains(Action.REMOVE_FILES_WIND_UP))
                .setTorrentRemovedInProgress(actions.contains(Action.REMOVE_TORRENT_IN_PROGRESS))
                .setTorrentRemovedWindUp(actions.contains(Action.REMOVE_TORRENT_WIND_UP))
                .build();

        return new TorrentStatusState(torrentInfo, lastAction, downloadState, peersState, torrentFileSystemState);
    }

    public static TorrentDownloader createDefaultTorrentDownloader(TorrentInfo torrentInfo, String downloadPath) {
        TorrentStatusStore torrentStatusStore = new TorrentStatusStore();
        torrentStatusStore.initializeState(Reducer.defaultTorrentStateSupplier.apply(torrentInfo)).block();
        return createDefaultTorrentDownloader(torrentInfo, downloadPath,
                torrentStatusStore);
    }

    public static TorrentDownloader createDefaultTorrentDownloader(TorrentInfo torrentInfo, String downloadPath,
                                                                   Flux<TrackerConnection> trackerConnectionConnectableFlux) {
        TrackerProvider trackerProvider = new TrackerProvider(torrentInfo);
        PeersProvider peersProvider = new PeersProvider(torrentInfo);

        TorrentStatusStore torrentStatusStore = new TorrentStatusStore();
        torrentStatusStore.initializeState(Reducer.defaultTorrentStateSupplier.apply(torrentInfo)).block();

        peersListener = new PeersListener(torrentStatusStore);

        // TODO: in case the test doesn't want the SearchPeers to get more peers from the tracker, I need to take care of it.
        SearchPeers searchPeers = new SearchPeers(torrentInfo, torrentStatusStore, trackerProvider,
                peersProvider, peersProvider.getPeersCommunicatorFromTrackerFlux(trackerConnectionConnectableFlux)
                .autoConnect(0));

        Flux<Link> peersCommunicatorFlux =
                Flux.merge(peersListener.getPeersConnectedToMeFlux()
                        // SocketException == When I shutdown the SocketServer after/before
                        // the tests inside Utils::removeEverythingRelatedToTorrent.
                        .onErrorResume(SocketException.class, throwable -> Flux.empty()), searchPeers.getPeers$())
                        // multiple subscriptions will activate flatMap(__ -> multiple times and it will cause
                        // multiple calls to getPeersCommunicatorFromTrackerFlux which waitForMessage new hot-flux
                        // every time and then I will connect to all the peers again and again...
                        .publish()
                        .autoConnect(0);

        FileSystemLink fileSystemLink = ActiveTorrents.getInstance()
                .createActiveTorrentMono(torrentInfo, downloadPath, torrentStatusStore,
                        peersCommunicatorFlux.map(Link::receivePeerMessages)
                                .flatMap(ReceiveMessagesNotifications::getPieceMessageResponseFlux))
                .block();

        BittorrentAlgorithm bittorrentAlgorithm =
                BittorrentAlgorithmInitializer.v1(torrentInfo,
                        torrentStatusStore,
                        fileSystemLink,
                        peersCommunicatorFlux);

        SpeedStatistics torrentSpeedStatistics =
                new TorrentSpeedSpeedStatisticsImpl(torrentInfo,
                        peersCommunicatorFlux.map(Link::getPeerSpeedStatistics));

        return TorrentDownloaders.getInstance()
                .createTorrentDownloader(torrentInfo,
                        peersListener,
                        searchPeers,
                        fileSystemLink,
                        bittorrentAlgorithm,
                        torrentStatusStore,
                        torrentSpeedStatistics,
                        peersCommunicatorFlux);
    }

    public static TorrentDownloader createDefaultTorrentDownloader(TorrentInfo torrentInfo, String downloadPath,
                                                                   TorrentStatusStore torrentStatusStore) {
        peersListener = new PeersListener(torrentStatusStore);

        // TODO: in case the test doesn't want the SearchPeers to get more peers from the tracker, I need to take care of it.
        SearchPeers searchPeers = new SearchPeers(torrentInfo, torrentStatusStore);

        Flux<Link> peersCommunicatorFlux =
                Flux.merge(peersListener.getPeersConnectedToMeFlux(), searchPeers.getPeers$())
                        // multiple subscriptions will activate flatMap(__ -> multiple times and it will cause
                        // multiple calls to getPeersCommunicatorFromTrackerFlux which waitForMessage new hot-flux
                        // every time and then I will connect to all the peers again and again...
                        .publish()
                        .autoConnect(0);

        FileSystemLink fileSystemLink = ActiveTorrents.getInstance()
                .createActiveTorrentMono(torrentInfo, downloadPath, torrentStatusStore,
                        peersCommunicatorFlux.map(Link::receivePeerMessages)
                                .flatMap(ReceiveMessagesNotifications::getPieceMessageResponseFlux))
                .block();

        BittorrentAlgorithm bittorrentAlgorithm =
                BittorrentAlgorithmInitializer.v1(torrentInfo,
                        torrentStatusStore,
                        fileSystemLink,
                        peersCommunicatorFlux);

        SpeedStatistics torrentSpeedStatistics =
                new TorrentSpeedSpeedStatisticsImpl(torrentInfo,
                        peersCommunicatorFlux.map(Link::getPeerSpeedStatistics));

        return TorrentDownloaders.getInstance()
                .createTorrentDownloader(torrentInfo,
                        peersListener,
                        searchPeers,
                        fileSystemLink,
                        bittorrentAlgorithm,
                        torrentStatusStore,
                        torrentSpeedStatistics,
                        peersCommunicatorFlux);
    }

    public static TorrentDownloader createCustomTorrentDownloader(TorrentInfo torrentInfo,
                                                                  TorrentStatusStore torrentStatusStore,
                                                                  FileSystemLink fileSystemLink,
                                                                  Flux<TrackerConnection> trackerConnectionConnectableFlux) {
        TrackerProvider trackerProvider = new TrackerProvider(torrentInfo);
        PeersProvider peersProvider = new PeersProvider(torrentInfo);

        peersListener = new PeersListener(torrentStatusStore);

        // TODO: in case the test doesn't want the SearchPeers to get more peers from the tracker, I need to take care of it.
        SearchPeers searchPeers = new SearchPeers(torrentInfo, torrentStatusStore, trackerProvider,
                peersProvider, peersProvider.getPeersCommunicatorFromTrackerFlux(trackerConnectionConnectableFlux)
                .autoConnect(0));

        Flux<Link> incomingPeers$ = peersListener.getPeersConnectedToMeFlux()
                // SocketException == When I shutdown the SocketServer after/before
                // the tests inside Utils::removeEverythingRelatedToTorrent.
                .onErrorResume(SocketException.class, throwable -> Flux.empty());

        Flux<Link> peersCommunicatorFlux =
                Flux.merge(incomingPeers$, searchPeers.getPeers$())
                        // multiple subscriptions will activate flatMap(__ -> multiple times and it will cause
                        // multiple calls to getPeersCommunicatorFromTrackerFlux which waitForMessage new hot-flux
                        // every time and then I will connect to all the peers again and again...
                        .publish()
                        .autoConnect(0);

        BittorrentAlgorithm bittorrentAlgorithm =
                BittorrentAlgorithmInitializer.v1(torrentInfo,
                        torrentStatusStore,
                        fileSystemLink,
                        peersCommunicatorFlux);

        SpeedStatistics torrentSpeedStatistics =
                new TorrentSpeedSpeedStatisticsImpl(torrentInfo,
                        peersCommunicatorFlux.map(Link::getPeerSpeedStatistics));

        return TorrentDownloaders.getInstance()
                .createTorrentDownloader(torrentInfo,
                        peersListener,
                        searchPeers,
                        fileSystemLink,
                        bittorrentAlgorithm,
                        torrentStatusStore,
                        torrentSpeedStatistics,
                        peersCommunicatorFlux);
    }

    public static Mono<SendMessagesNotifications> sendFakeMessage(TorrentInfo torrentInfo, String downloadPath, Link link, PeerMessageType peerMessageType) {
        switch (peerMessageType) {
            case HaveMessage:
                return link.sendMessages().sendHaveMessage(0);
            case PortMessage:
                return link.sendMessages().sendPortMessage((short) link.getMe().getPeerPort());
            case ChokeMessage:
                return link.sendMessages().sendChokeMessage();
            case PieceMessage:
                return createAndSendFakePieceMessage(torrentInfo, downloadPath, link);
            case CancelMessage:
                return link.sendMessages().sendCancelMessage(2, 0, 10);
            case KeepAliveMessage:
                return link.sendMessages().sendKeepAliveMessage();
            case RequestMessage:
                return link.sendMessages().sendRequestMessage(1, 0, 3);
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

    private static Mono<SendMessagesNotifications> createAndSendFakePieceMessage(TorrentInfo torrentInfo, String downloadPath, Link link) {
        ActiveTorrents activeTorrents = ActiveTorrents.getInstance();
        int pieceIndex = 3;
        int pieceLength = torrentInfo.getPieceLength(pieceIndex);
        int begin = 0;
        int blockLength = pieceLength;
        return BlocksAllocatorImpl.getInstance()
                .updateAllocations(4, blockLength)
                .flatMap(allocatorState -> BlocksAllocatorImpl.getInstance()
                        .createPieceMessage(link.getPeer(), link.getMe(), pieceIndex, begin, blockLength, allocatorState.getBlockLength()))
                .doOnNext(pieceMessageToSave -> {
                    for (int i = 0; i < blockLength; i++)
                        pieceMessageToSave.getAllocatedBlock().getBlock()[i] = 11;
                })
                .flatMap(pieceMessageToSave -> {
                    ConnectableFlux<PieceMessage> pieceMessageFlux = Flux.just(pieceMessageToSave).publish();
                    TorrentStatusStore torrentStatusStore = new TorrentStatusStore();
                    torrentStatusStore.initializeState(Reducer.defaultTorrentStateSupplier.apply(torrentInfo)).block();
                    return activeTorrents.createActiveTorrentMono(link.getTorrentInfo(), downloadPath, torrentStatusStore, pieceMessageFlux)
                            .flatMap(fileSystemLink -> {
                                Flux<PieceEvent> savedPieces$ = fileSystemLink.savedBlockFlux()
                                        .replay()
                                        .autoConnect(0);
                                pieceMessageFlux.connect();
                                // wait until we saved the piece and then send it to the fake peer.
                                return savedPieces$
                                        .doOnNext(pieceEvent -> Assert.assertEquals("the piece I saved is not " +
                                                "the pieces that was saved.", pieceIndex, pieceEvent.getReceivedPiece().getIndex()))
                                        .map(PieceEvent::getReceivedPiece)
                                        .map(PieceMessage::getAllocatedBlock)
                                        .flatMap(allocatedBlock -> BlocksAllocatorImpl.getInstance().free(allocatedBlock))
                                        .map(__ -> new RequestMessage(link.getMe(), link.getPeer(), pieceIndex, begin, blockLength))
                                        .flatMap(requestMessage -> fileSystemLink.buildPieceMessage(requestMessage))
                                        .flatMap(pieceMessage -> link.sendMessages().sendPieceMessage(pieceMessage)
                                                .flatMap(sendMessagesNotifications -> fileSystemLink.deleteActiveTorrentOnlyMono()
                                                        .flatMap(__ -> fileSystemLink.deleteFileOnlyMono())
                                                        .map(__ -> sendMessagesNotifications)))
                                        .take(1)
                                        .single();
                            });
                });
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

    public static Mono<PieceMessage> readFromFile(TorrentInfo torrentInfo, String downloadPath, RequestMessage requestMessage) {
        List<TorrentFile> fileList = torrentInfo.getFileList();

        List<ActualFileImpl> actualFileImplList = new ArrayList<>();
        String fullFilePath = downloadPath;
        if (!torrentInfo.isSingleFileTorrent())
            fullFilePath += torrentInfo.getName() + File.separator;
        long position = 0;
        for (TorrentFile torrentFile : fileList) {
            String completeFilePath = torrentFile.getFileDirs()
                    .stream()
                    .collect(Collectors.joining(File.separator, fullFilePath, ""));
            long from = position;
            long to = position + torrentFile.getFileLength();
            position = to;

            ActualFileImpl actualFileImpl = new ActualFileImpl(completeFilePath, from, to, null);
            actualFileImplList.add(actualFileImpl);
        }

        // read from the file:

        return BlocksAllocatorImpl.getInstance()
                .createPieceMessage(requestMessage.getTo(), requestMessage.getFrom(),
                        requestMessage.getIndex(), requestMessage.getBegin(),
                        requestMessage.getBlockLength(), torrentInfo.getPieceLength(requestMessage.getIndex()))
                .flatMap(pieceMessage -> {
                    long from = torrentInfo.getPieceStartPosition(requestMessage.getIndex()) + requestMessage.getBegin();
                    long to = from + requestMessage.getBlockLength();

                    int resultFreeIndex = 0;
                    long amountOfBytesOfFileWeCovered = 0;
                    for (ActualFileImpl actualFileImpl : actualFileImplList) {
                        if (actualFileImpl.getFrom() <= from && from <= actualFileImpl.getTo()) {

                            OpenOption[] options = {StandardOpenOption.READ};
                            SeekableByteChannel seekableByteChannel = null;
                            try {
                                seekableByteChannel = Files.newByteChannel(Paths.get(actualFileImpl.getFilePath()), options);
                            } catch (IOException e) {
                                return Mono.error(e);
                            }

                            long fromWhereToReadInThisFile = from - amountOfBytesOfFileWeCovered;
                            try {
                                seekableByteChannel.position(fromWhereToReadInThisFile);
                            } catch (IOException e) {
                                return Mono.error(e);
                            }

                            // to,from are taken from the requestMessage message object so "to-from" must be valid integer.
                            int howMuchToReadFromThisFile = (int) Math.min(actualFileImpl.getTo() - from, to - from);
                            ByteBuffer block = ByteBuffer.allocate(howMuchToReadFromThisFile);
                            try {
                                seekableByteChannel.read(block);
                            } catch (IOException e) {
                                return Mono.error(e);
                            }

                            for (byte aTempResult : block.array())
                                pieceMessage.getAllocatedBlock().getBlock()[resultFreeIndex++] = aTempResult;
                            from += howMuchToReadFromThisFile;

                            try {
                                seekableByteChannel.close();
                            } catch (IOException e) {
                                return Mono.error(e);
                            }
                        }
                        if (from == to)
                            return Mono.just(pieceMessage);
                        amountOfBytesOfFileWeCovered = actualFileImpl.getTo();
                    }
                    return Mono.error(new Exception("we shouldn't be here! never!"));
                });
    }

    public static void deleteDownloadFolder() {
        try {
            File file = new File(System.getProperty("user.dir") + File.separator + "torrents-test");
            if (file.exists()) {
                deleteDirectory(file);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void deleteDirectory(File directoryToBeDeleted) throws IOException {
        Files.walkFileTree(directoryToBeDeleted.toPath(), new HashSet<>(), Integer.MAX_VALUE, new FileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
                    throws IOException {
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                    throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc)
                    throws IOException {
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc)
                    throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    public static Flux<PieceMessage> createRandomPieceMessages(TorrentInfo torrentInfo,
                                                               Semaphore semaphore,
                                                               BlockOfPiece blockOfPiece,
                                                               int allocatedBlockLength) {
        int pieceIndex = blockOfPiece.getPieceIndex();
        int pieceLength = torrentInfo.getPieceLength(pieceIndex);

        int begin = Math.min(blockOfPiece.getFrom(), pieceLength - 1);
        // calculate what is the over all size of all blocks I'm going to create.
        int totalBlockLength = begin + blockOfPiece.getLength() > pieceLength ?
                pieceLength - begin :
                blockOfPiece.getLength() - begin;

        // here I will use allocatedBlockLength to split totalBlockLength to small blocks:
        return Flux.<BlockOfPiece, Integer>generate(() -> begin, (blockStartPosition, sink) -> {
            if (blockStartPosition >= totalBlockLength) {
                sink.complete();
                return blockStartPosition;
            }

            try {
                semaphore.acquire();// wait until downstream finish working on the last signal.
            } catch (InterruptedException e) {
                sink.error(e);
            }

            int blockLength = Math.min(allocatedBlockLength, totalBlockLength - blockStartPosition);
            sink.next(new BlockOfPiece(pieceIndex, blockStartPosition, blockLength));

            return blockStartPosition + blockLength;
        })
                .flatMap(smallBlock -> {
                    BlocksAllocator instance = BlocksAllocatorImpl.getInstance();
                    return instance.createPieceMessage(null, null, smallBlock.getPieceIndex(), smallBlock.getFrom(), smallBlock.getLength(), pieceLength)
                            .doOnNext(pieceMessage -> Assert.assertEquals("I didn't proceed the length as good as I should have.",
                                    smallBlock.getLength().longValue(),
                                    pieceMessage.getAllocatedBlock().getLength()));
                })
                .doOnNext(pieceMessage -> {
                    for (int i = 0; i < pieceMessage.getAllocatedBlock().getLength(); i++)
                        pieceMessage.getAllocatedBlock().getBlock()[i] = (byte) i;
                });
    }

    public static BlockOfPiece fixBlockOfPiece(BlockOfPiece blockOfPiece, TorrentInfo torrentInfo, int allocatedBlockLength) {
        int pieceIndex = blockOfPiece.getPieceIndex() >= 0 ?
                blockOfPiece.getPieceIndex() :
                torrentInfo.getPieces().size() + blockOfPiece.getPieceIndex();

        int pieceLength = torrentInfo.getPieceLength(pieceIndex);

        // replace the nulls and the "-1"s:
        int fixedFrom = blockOfPiece.getFrom() == null ?
                pieceLength - 1 :
                blockOfPiece.getFrom().equals(-1) ? allocatedBlockLength - 1 : blockOfPiece.getFrom();
        int fixedBlockLength = blockOfPiece.getLength() == null ?
                pieceLength :
                blockOfPiece.getLength().equals(-1) ? allocatedBlockLength : blockOfPiece.getLength();

        return new BlockOfPiece(pieceIndex, fixedFrom, fixedBlockLength);
    }

    public static <T, U> void assertListEqualNotByOrder(List<T> expected, List<U> actual, BiPredicate<T, U> areElementsEqual) {
        Assert.assertTrue(expected.stream().allMatch(t1 -> {
            boolean b = actual.stream()
                    .anyMatch(t2 -> areElementsEqual.test(t1, t2));
            if (!b)
                System.out.println(t1 + " -  expected is not inside actual.");
            return b;
        }));
        Assert.assertTrue(actual.stream().allMatch(t2 -> {
            boolean b = expected.stream()
                    .anyMatch(t1 -> areElementsEqual.test(t1, t2));
            if (!b)
                System.out.println(t2 + " -  actual is not inside expected.");
            return b;
        }));
    }
}
