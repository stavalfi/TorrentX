package com.utils;

import christophedetroyer.torrent.TorrentFile;
import christophedetroyer.torrent.TorrentParser;
import com.steps.MyStepdefs;
import main.TorrentInfo;
import main.downloader.TorrentDownloader;
import main.downloader.TorrentDownloaders;
import main.file.system.ActualFileImpl;
import main.file.system.FileSystemLink;
import main.file.system.allocator.AllocatorStore;
import main.listener.ListenerAction;
import main.listener.reducers.ListenerReducer;
import main.listener.state.tree.ListenerState;
import main.peer.Link;
import main.peer.IncomingPeerMessagesNotifier;
import main.peer.SendMessagesNotifications;
import main.peer.peerMessages.PeerMessage;
import main.peer.peerMessages.PieceMessage;
import main.peer.peerMessages.RequestMessage;
import main.torrent.status.TorrentStatusAction;
import main.torrent.status.reducers.TorrentStatusReducer;
import main.torrent.status.state.tree.DownloadState;
import main.torrent.status.state.tree.SearchPeersState;
import main.torrent.status.state.tree.TorrentFileSystemState;
import main.torrent.status.state.tree.TorrentStatusState;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;
import redux.store.Store;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Utils {
    private static Logger logger = LoggerFactory.getLogger(Utils.class);

    public static TorrentInfo createTorrentInfo(String torrentFilePath) throws IOException {
        String torrentFilesPath = "src" + File.separator +
                "test" + File.separator +
                "resources" + File.separator +
                "torrents" + File.separator +
                torrentFilePath;
        return new TorrentInfo(torrentFilesPath, TorrentParser.parseTorrent(torrentFilesPath));
    }

    private static BiPredicate<ListenerState, ListenerState> isEqualByProperties = (defaultListenState, listenerState) ->
            defaultListenState.getAction().equals(listenerState.getAction()) &&
                    defaultListenState.isStartedListeningInProgress() == listenerState.isStartedListeningInProgress() &&
                    defaultListenState.isStartedListeningSelfResolved() == listenerState.isStartedListeningSelfResolved() &&
                    defaultListenState.isStartedListeningWindUp() == listenerState.isStartedListeningWindUp() &&
                    defaultListenState.isResumeListeningInProgress() == listenerState.isResumeListeningInProgress() &&
                    defaultListenState.isResumeListeningSelfResolved() == listenerState.isResumeListeningSelfResolved() &&
                    defaultListenState.isResumeListeningWindUp() == listenerState.isResumeListeningWindUp() &&
                    defaultListenState.isPauseListeningInProgress() == listenerState.isPauseListeningInProgress() &&
                    defaultListenState.isPauseListeningSelfResolved() == listenerState.isPauseListeningSelfResolved() &&
                    defaultListenState.isPauseListeningWindUp() == listenerState.isPauseListeningWindUp() &&
                    defaultListenState.isRestartListeningInProgress() == listenerState.isRestartListeningInProgress() &&
                    defaultListenState.isRestartListeningSelfResolved() == listenerState.isRestartListeningSelfResolved() &&
                    defaultListenState.isRestartListeningWindUp() == listenerState.isRestartListeningWindUp();

    public static void removeEverythingRelatedToLastTest() {

        TorrentDownloaders.getAllocatorStore()
                .latestState$()
                .doOnNext(allocatorState -> {
                    IntStream.range(0, allocatorState.getAmountOfBlocks())
                            .forEach(i -> {
                                if (!allocatorState.getFreeBlocksStatus().get(i)) {
                                    logger.error("fuck");
//                                    try {
//                                        Thread.sleep(100000000);
//                                    } catch (InterruptedException e) {
//                                        e.printStackTrace();
//                                    }
                                }
                                Assert.assertTrue("i: " + i + " - global app allocator: " + allocatorState.toString(),
                                        allocatorState.getFreeBlocksStatus().get(i));
                            });
                })
                .as(StepVerifier::create)
                .expectNextCount(1)
                .verifyComplete();

        MyStepdefs.globalFakePeerAllocator
                .latestState$()
                .doOnNext(allocatorState -> {
                    IntStream.range(0, allocatorState.getAmountOfBlocks())
                            .forEach(i -> {
                                if (!allocatorState.getFreeBlocksStatus().get(i)) {
                                    logger.error("fuck");
//                                    try {
//                                        Thread.sleep(100000000);
//                                    } catch (InterruptedException e) {
//                                        e.printStackTrace();
//                                    }
                                }
                                Assert.assertTrue("i: " + i + " - fake-peer allocator: " + allocatorState.toString(),
                                        allocatorState.getFreeBlocksStatus().get(i));
                            });
                })
                .as(StepVerifier::create)
                .expectNextCount(1)
                .verifyComplete();

        List<TorrentDownloader> torrentDownloaders = TorrentDownloaders.getInstance()
                .getTorrentDownloadersFlux()
                .collectList()
                .block();

        TorrentDownloaders.getInstance()
                .getTorrentDownloadersFlux()
                .filter(torrentDownloader -> torrentDownloader.getTorrentStatusStore() != null)
                .map(TorrentDownloader::getTorrentStatusStore)
                .flatMap(store -> store.dispatch(TorrentStatusAction.REMOVE_FILES_IN_PROGRESS)
                        .flatMap(__ -> store.dispatch(TorrentStatusAction.REMOVE_TORRENT_IN_PROGRESS)))
                .collectList()
                .as(StepVerifier::create)
                .expectNextCount(1)
                .verifyComplete();

        TorrentDownloaders.getInstance()
                .getTorrentDownloadersFlux()
                .filter(torrentDownloader -> torrentDownloader.getTorrentStatusStore() != null)
                .filter(torrentDownloader -> torrentDownloader.getTorrentStatesSideEffects() != null)
                .map(TorrentDownloader::getTorrentStatusStore)
                .flatMap(store -> store.notifyWhen(TorrentStatusAction.REMOVE_FILES_WIND_UP, store))
                .flatMap(store -> store.notifyWhen(TorrentStatusAction.REMOVE_TORRENT_WIND_UP, store))
                .collectList()
                .as(StepVerifier::create)
                .expectNextCount(1)
                .verifyComplete();

        TorrentDownloaders.getInstance()
                .getTorrentDownloadersFlux()
                .map(TorrentDownloader::getTorrentInfo)
                .map(TorrentInfo::getTorrentInfoHash)
                // TODO: in case I get the objects from remote server like mongodb one by one, then this flux will never be over so collectList will never emit next signal.
                .doOnNext(torrentInfoHash -> TorrentDownloaders.getInstance().deleteTorrentDownloader(torrentInfoHash))
                .collectList()
                .as(StepVerifier::create)
                .expectNextCount(1)
                .verifyComplete();

        torrentDownloaders.stream()
                .map(TorrentDownloader::getTorrentStatusStore)
                .forEach(Store::dispose);

        TorrentDownloaders.getListenStore()
                .dispatch(ListenerAction.RESTART_LISTENING_IN_PROGRESS)
                .flatMapMany(__ -> TorrentDownloaders.getListenStore().states$())
                .filter(listenerState -> isEqualByProperties.test(ListenerReducer.defaultListenState, listenerState))
                .take(1)
                .as(StepVerifier::create)
                .expectNextCount(1)
                .verifyComplete();


        // delete download folder from last test
        deleteAppDownloadFolder();
        deleteFakePeerDownloadFolder();
    }

    public static void changeListenerState(List<ListenerAction> changesActionList, Store<ListenerState, ListenerAction> listenStore) {
        Flux.fromIterable(changesActionList)
                .filter(action -> action.equals(ListenerAction.START_LISTENING_IN_PROGRESS) ||
                        action.equals(ListenerAction.START_LISTENING_SELF_RESOLVED) ||
                        action.equals(ListenerAction.RESUME_LISTENING_IN_PROGRESS) ||
                        action.equals(ListenerAction.RESUME_LISTENING_SELF_RESOLVED) ||
                        action.equals(ListenerAction.PAUSE_LISTENING_IN_PROGRESS) ||
                        action.equals(ListenerAction.PAUSE_LISTENING_SELF_RESOLVED) ||
                        action.equals(ListenerAction.RESTART_LISTENING_IN_PROGRESS) ||
                        action.equals(ListenerAction.RESTART_LISTENING_SELF_RESOLVED))
                .flatMap(action -> {
                    switch (action) {
                        case START_LISTENING_IN_PROGRESS:
                            return listenStore.dispatch(action)
                                    .flatMapMany(__ -> listenStore.states$())
                                    .filter(listenerState -> listenerState.isResumeListeningWindUp())
                                    .take(1)
                                    .single();
                        case START_LISTENING_SELF_RESOLVED:
                            return listenStore.states$()
                                    .filter(ListenerState::isStartedListeningInProgress)
                                    .take(1)
                                    .flatMap(__ -> listenStore.dispatch(action))
                                    .flatMap(__ -> listenStore.states$())
                                    .filter(ListenerState::isResumeListeningWindUp)
                                    .take(1)
                                    .single();
                        case RESUME_LISTENING_IN_PROGRESS:
                            return listenStore.dispatch(action)
                                    .flatMapMany(__ -> listenStore.states$())
                                    .filter(ListenerState::isResumeListeningWindUp)
                                    .take(1)
                                    .single();
                        case RESUME_LISTENING_SELF_RESOLVED:
                            return listenStore.states$()
                                    .filter(ListenerState::isResumeListeningInProgress)
                                    .take(1)
                                    .flatMap(__ -> listenStore.dispatch(action))
                                    .flatMap(__ -> listenStore.states$())
                                    .filter(ListenerState::isResumeListeningWindUp)
                                    .take(1)
                                    .single();
                        case PAUSE_LISTENING_IN_PROGRESS:
                            return listenStore.dispatch(action)
                                    .flatMapMany(__ -> listenStore.states$())
                                    .filter(ListenerState::isPauseListeningWindUp)
                                    .take(1)
                                    .single();
                        case PAUSE_LISTENING_SELF_RESOLVED:
                            return listenStore.states$()
                                    .filter(ListenerState::isPauseListeningInProgress)
                                    .take(1)
                                    .flatMap(__ -> listenStore.dispatch(action))
                                    .flatMap(__ -> listenStore.states$())
                                    .filter(ListenerState::isPauseListeningWindUp)
                                    .take(1)
                                    .single();
                        case RESTART_LISTENING_IN_PROGRESS:
                            return listenStore.dispatch(action)
                                    .flatMapMany(__ -> listenStore.states$())
                                    .filter(state -> isEqualByProperties.test(state, ListenerReducer.defaultListenState))
                                    .take(1)
                                    .single();
                        case RESTART_LISTENING_SELF_RESOLVED:
                            return listenStore.states$()
                                    .filter(ListenerState::isRestartListeningInProgress)
                                    .take(1)
                                    .flatMap(__ -> listenStore.dispatch(action))
                                    .flatMap(__ -> listenStore.states$())
                                    .filter(state -> isEqualByProperties.test(state, ListenerReducer.defaultListenState))
                                    .take(1)
                                    .single();
                        default:
                            return Mono.empty();
                    }
                }, changesActionList.size())
                .blockLast();
    }

    public static TorrentStatusState getTorrentStatusState(TorrentInfo torrentInfo, TorrentStatusAction lastTorrentStatusAction, List<TorrentStatusAction> torrentStatusActions) {
        DownloadState downloadState = DownloadState.DownloadStateBuilder.builder()
                .setStartDownloadInProgress(torrentStatusActions.contains(TorrentStatusAction.START_DOWNLOAD_IN_PROGRESS))
                .setStartDownloadWindUp(torrentStatusActions.contains(TorrentStatusAction.START_DOWNLOAD_WIND_UP))
                .setResumeDownloadInProgress(torrentStatusActions.contains(TorrentStatusAction.RESUME_DOWNLOAD_IN_PROGRESS))
                .setResumeDownloadWindUp(torrentStatusActions.contains(TorrentStatusAction.RESUME_DOWNLOAD_WIND_UP))
                .setPauseDownloadInProgress(torrentStatusActions.contains(TorrentStatusAction.PAUSE_DOWNLOAD_IN_PROGRESS))
                .setPauseDownloadWindUp(torrentStatusActions.contains(TorrentStatusAction.PAUSE_DOWNLOAD_WIND_UP))
                .setCompletedDownloadingInProgress(torrentStatusActions.contains(TorrentStatusAction.COMPLETED_DOWNLOADING_IN_PROGRESS))
                .setCompletedDownloadingWindUp(torrentStatusActions.contains(TorrentStatusAction.COMPLETED_DOWNLOADING_WIND_UP))
                .setStartUploadInProgress(torrentStatusActions.contains(TorrentStatusAction.START_UPLOAD_IN_PROGRESS))
                .setStartUploadWindUp(torrentStatusActions.contains(TorrentStatusAction.START_UPLOAD_WIND_UP))
                .setResumeUploadInProgress(torrentStatusActions.contains(TorrentStatusAction.RESUME_UPLOAD_IN_PROGRESS))
                .setResumeUploadWindUp(torrentStatusActions.contains(TorrentStatusAction.RESUME_UPLOAD_WIND_UP))
                .setPauseUploadInProgress(torrentStatusActions.contains(TorrentStatusAction.PAUSE_UPLOAD_IN_PROGRESS))
                .setPauseUploadWindUp(torrentStatusActions.contains(TorrentStatusAction.PAUSE_UPLOAD_WIND_UP))
                .build();

        SearchPeersState searchPeersState = SearchPeersState.PeersStateBuilder.builder()
                .setStartedSearchingPeersInProgress(torrentStatusActions.contains(TorrentStatusAction.START_SEARCHING_PEERS_IN_PROGRESS))
                .setStartedSearchingPeersSelfResolved(torrentStatusActions.contains(TorrentStatusAction.START_SEARCHING_PEERS_SELF_RESOLVED))
                .setStartedSearchingPeersWindUp(torrentStatusActions.contains(TorrentStatusAction.START_SEARCHING_PEERS_WIND_UP))
                .setPauseSearchingPeersInProgress(torrentStatusActions.contains(TorrentStatusAction.PAUSE_SEARCHING_PEERS_IN_PROGRESS))
                .setPauseSearchingPeersSelfResolved(torrentStatusActions.contains(TorrentStatusAction.PAUSE_SEARCHING_PEERS_SELF_RESOLVED))
                .setPauseSearchingPeersWindUp(torrentStatusActions.contains(TorrentStatusAction.PAUSE_SEARCHING_PEERS_WIND_UP))
                .setResumeSearchingPeersInProgress(torrentStatusActions.contains(TorrentStatusAction.RESUME_SEARCHING_PEERS_IN_PROGRESS))
                .setResumeSearchingPeersSelfResolved(torrentStatusActions.contains(TorrentStatusAction.RESUME_SEARCHING_PEERS_SELF_RESOLVED))
                .setResumeSearchingPeersWindUp(torrentStatusActions.contains(TorrentStatusAction.RESUME_SEARCHING_PEERS_WIND_UP))
                .build();

        TorrentFileSystemState torrentFileSystemState = TorrentFileSystemState.TorrentFileSystemStateBuilder.builder()
                .setFilesRemovedInProgress(torrentStatusActions.contains(TorrentStatusAction.REMOVE_FILES_IN_PROGRESS))
                .setFilesRemovedWindUp(torrentStatusActions.contains(TorrentStatusAction.REMOVE_FILES_WIND_UP))
                .setTorrentRemovedInProgress(torrentStatusActions.contains(TorrentStatusAction.REMOVE_TORRENT_IN_PROGRESS))
                .setTorrentRemovedWindUp(torrentStatusActions.contains(TorrentStatusAction.REMOVE_TORRENT_WIND_UP))
                .build();

        return new TorrentStatusState(null, lastTorrentStatusAction, downloadState, searchPeersState, torrentFileSystemState);
    }

    public static ListenerState getListenStatusState(ListenerAction lastAction, List<ListenerAction> actions) {
        return ListenerState.ListenStateBuilder.builder(lastAction)
                .setStartedListeningInProgress(actions.contains(ListenerAction.START_LISTENING_IN_PROGRESS))
                .setStartedListeningSelfResolved(actions.contains(ListenerAction.START_LISTENING_SELF_RESOLVED))
                .setStartedListeningWindUp(actions.contains(ListenerAction.START_LISTENING_WIND_UP))
                .setResumeListeningInProgress(actions.contains(ListenerAction.RESUME_LISTENING_IN_PROGRESS))
                .setResumeListeningSelfResolved(actions.contains(ListenerAction.RESUME_LISTENING_SELF_RESOLVED))
                .setResumeListeningWindUp(actions.contains(ListenerAction.RESUME_LISTENING_WIND_UP))
                .setPauseListeningInProgress(actions.contains(ListenerAction.PAUSE_LISTENING_IN_PROGRESS))
                .setPauseListeningSelfResolved(actions.contains(ListenerAction.PAUSE_LISTENING_SELF_RESOLVED))
                .setPauseListeningWindUp(actions.contains(ListenerAction.PAUSE_LISTENING_WIND_UP))
                .setRestartListeningInProgress(actions.contains(ListenerAction.RESTART_LISTENING_IN_PROGRESS))
                .setRestartListeningSelfResolved(actions.contains(ListenerAction.RESTART_LISTENING_SELF_RESOLVED))
                .setRestartListeningWindUp(actions.contains(ListenerAction.RESTART_LISTENING_WIND_UP))
                .build();
    }

    public static Mono<SendMessagesNotifications> sendFakeMessage(TorrentInfo torrentInfo,
                                                                  String downloadPath,
                                                                  Link link,
                                                                  AllocatorStore allocatorStore,
                                                                  PeerMessageType peerMessageType,
                                                                  FileSystemLink fileSystemLink,
                                                                  int pieceIndex,
                                                                  int begin,
                                                                  int blockLength,
                                                                  int pieceLength) {
        switch (peerMessageType) {
            case HaveMessage:
                return link.sendMessages().sendHaveMessage(0);
            case PortMessage:
                return link.sendMessages().sendPortMessage((short) link.getMe().getPeerPort());
            case ChokeMessage:
                return link.sendMessages().sendChokeMessage();
            case PieceMessage:
                return allocatorStore
                        .createRequestMessage(null, null, pieceIndex, begin, blockLength, pieceLength)
                        .doOnNext(requestMessage -> logger.debug("start creating fake-piece-message to send to a fake-peer. " +
                                "the details of the piece-message are coming from a fake-request-message I created: " + requestMessage))
                        .flatMap(requestMessage -> fileSystemLink.buildPieceMessage(requestMessage))
                        .doOnNext(pieceMessage -> logger.debug("end creating fake-piece-message to send to a fake-peer: " + pieceMessage))
                        .doOnNext(pieceMessage -> logger.debug("start send fake-peer-message to fake-peer: " + pieceMessage))
                        .flatMap(pieceMessage -> link.sendMessages().sendPieceMessage(pieceMessage)
                                .doOnNext(__ -> logger.debug("end send fake-peer-message to fake-peer: " + pieceMessage)));
            case CancelMessage:
                return link.sendMessages().sendCancelMessage(2, 0, 10);
            case KeepAliveMessage:
                return link.sendMessages().sendKeepAliveMessage();
            case RequestMessage:
                return link.sendMessages().sendRequestMessage(1, 0, 3);
            case ExtendedMessage:
                return null;
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

    public static Flux<? extends PeerMessage> getSpecificMessageResponseFluxByMessageType(IncomingPeerMessagesNotifier incomingPeerMessagesNotifier,
                                                                                          Link link, PeerMessageType peerMessageType) {
        switch (peerMessageType) {
            case HaveMessage:
                return incomingPeerMessagesNotifier.getHaveMessageResponse$(link);
            case PortMessage:
                return incomingPeerMessagesNotifier.getPortMessageResponse$(link);
            case ChokeMessage:
                return incomingPeerMessagesNotifier.getChokeMessageResponse$(link);
            case PieceMessage:
                return incomingPeerMessagesNotifier.getPieceMessageResponse$(link);
            case CancelMessage:
                return incomingPeerMessagesNotifier.getCancelMessageResponse$(link);
            case KeepAliveMessage:
                return incomingPeerMessagesNotifier.getKeepMessageResponse$(link);
            case RequestMessage:
                return incomingPeerMessagesNotifier.getRequestMessageResponse$(link);
            case UnchokeMessage:
                return incomingPeerMessagesNotifier.getUnchokeMessageResponse$(link);
            case BitFieldMessage:
                return incomingPeerMessagesNotifier.getBitFieldMessageResponse$(link);
            case InterestedMessage:
                return incomingPeerMessagesNotifier.getInterestedMessageResponse$(link);
            case NotInterestedMessage:
                return incomingPeerMessagesNotifier.getNotInterestedMessageResponse$(link);
            case ExtendedMessage:
                return incomingPeerMessagesNotifier.getExtendedMessageResponse$(link);
            default:
                throw new IllegalArgumentException(peerMessageType.toString());
        }
    }

    public static Mono<PieceMessage> readFromFile(AllocatorStore allocatorStore, TorrentInfo torrentInfo, String downloadPath, RequestMessage requestMessage) {
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

        return allocatorStore.createPieceMessage(requestMessage.getTo(), requestMessage.getFrom(),
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

    public static void deleteAppDownloadFolder() {
        String fullDownloadPathForRealApp = System.getProperty("user.dir") + File.separator + "torrents-test";
        deleteFolderAndItsContent(fullDownloadPathForRealApp);
    }

    public static void deleteFakePeerDownloadFolder() {
        String fullDownloadPathForFakePeer = System.getProperty("user.dir") + File.separator + "fake-peer-download-folder";
        deleteFolderAndItsContent(fullDownloadPathForFakePeer);
        Assert.assertFalse("we couldn't delete a folder. ",
                new File(fullDownloadPathForFakePeer).exists());
    }

    private static void deleteFolderAndItsContent(String fullDownloadPathForFakePeer) {
        try {
            File file = new File(fullDownloadPathForFakePeer);
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
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                    throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) {
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

    public static Flux<PieceMessage> createRandomPieceMessages(AllocatorStore allocatorStore,
                                                               TorrentInfo torrentInfo,
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
                .flatMap(smallBlock -> allocatorStore.createPieceMessage(null, null, smallBlock.getPieceIndex(), smallBlock.getFrom(), smallBlock.getLength(), pieceLength)
                        .doOnNext(pieceMessage -> Assert.assertEquals("I didn't proceed the length as good as I should have.",
                                smallBlock.getLength().longValue(),
                                pieceMessage.getAllocatedBlock().getLength())))
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
