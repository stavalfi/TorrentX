package main.file.system;

import christophedetroyer.torrent.TorrentFile;
import main.TorrentInfo;
import main.downloader.PieceEvent;
import main.downloader.TorrentPieceStatus;
import main.file.system.allocator.AllocatorStore;
import main.file.system.exceptions.PieceNotDownloadedYetException;
import main.peer.Peer;
import main.peer.peerMessages.BitFieldMessage;
import main.peer.peerMessages.PieceMessage;
import main.peer.peerMessages.RequestMessage;
import main.torrent.status.TorrentStatusAction;
import main.torrent.status.state.tree.TorrentStatusState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import redux.store.Store;

import java.io.File;
import java.io.IOException;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.BitSet;
import java.util.Collection;
import java.util.stream.Collectors;

public class FileSystemLinkImpl extends TorrentInfo implements FileSystemLink {
    private static Logger logger = LoggerFactory.getLogger(FileSystemLinkImpl.class);
    private static Scheduler saveBlockScheduler = Schedulers.newSingle("FS-SAVE-BLOCK");
    private final Flux<ActualFile> actualFileImplList;
    private final BitSet piecesStatus;
    private final int[] downloadedBytesInPieces;
    private final String downloadPath;
    private Flux<Integer> savedPieces$;
    private Flux<PieceEvent> savedBlocks$;
    private AllocatorStore allocatorStore;
    private Flux<TorrentStatusState> completeDownload$;
    private Flux<TorrentStatusState> removeTorrent$;
    private Flux<TorrentStatusState> removeFiles$;
    private String identifier;

    public FileSystemLinkImpl(TorrentInfo torrentInfo, String downloadPath,
                              AllocatorStore allocatorStore,
                              Store<TorrentStatusState, TorrentStatusAction> torrentStatusStore,
                              Flux<PieceMessage> peerResponses$,
                              String identifier) {
        super(torrentInfo);
        this.identifier = identifier;
        this.allocatorStore = allocatorStore;
        this.downloadPath = downloadPath;
        this.piecesStatus = new BitSet(getPieces().size());
        this.downloadedBytesInPieces = new int[getPieces().size()];

        createFolders(torrentInfo, downloadPath);

        // this flux will run on this thread so there won't be a chance that
        // someone ask if the files are exist when we go out this constructor and the files weren't created yet.
        // there is a test that sometimes (5%) fail because this race condition.
        this.actualFileImplList = createActiveTorrentFileList(torrentInfo, downloadPath)
                .replay()
                .autoConnect(0);

        this.completeDownload$ = torrentStatusStore.statesByAction(TorrentStatusAction.COMPLETED_DOWNLOADING_IN_PROGRESS)
                .concatMap(__ -> torrentStatusStore.dispatch(TorrentStatusAction.COMPLETED_DOWNLOADING_SELF_RESOLVED))
                .publish()
                .autoConnect(0);

        // TODO: this status is useless because we don't use ActiveTorrents class
        this.removeTorrent$ = torrentStatusStore.statesByAction(TorrentStatusAction.REMOVE_TORRENT_IN_PROGRESS)
                .concatMap(__ -> torrentStatusStore.dispatch(TorrentStatusAction.REMOVE_TORRENT_SELF_RESOLVED))
                .publish()
                .autoConnect(0);

        this.removeFiles$ = torrentStatusStore.statesByAction(TorrentStatusAction.REMOVE_FILES_IN_PROGRESS)
                .concatMap(__ -> deleteFileOnlyMono())
                .concatMap(__ -> torrentStatusStore.dispatch(TorrentStatusAction.REMOVE_FILES_SELF_RESOLVED))
                .publish()
                .autoConnect(0);

        // I must save all this incoming pieces before I exit the constructor because I subscribe to peerResponses$ **SOMETIMES**
        // after I exit the constructor because the subscription depends if COMPLETED_DOWNLOADING_WIND_UP==true.
        // TODO: find something better then this solution and check it with ubuntu 14.04.5
        Flux<PieceMessage> pieceMessageReplay$ = peerResponses$.replay()
                .autoConnect(0);

        this.savedBlocks$ = torrentStatusStore.latestState$()
                .map(torrentStatusState -> torrentStatusState.fromAction(TorrentStatusAction.COMPLETED_DOWNLOADING_WIND_UP))
                .publishOn(saveBlockScheduler)
                .flatMapMany(isCompletedDownloading -> {
                    if (isCompletedDownloading) {
                        logger.info(this.identifier + " - Torrent: " + torrentInfo.getName() + ", the torrent download is already completed so we update our internal state that all the pieces are completed.");
                        this.piecesStatus.set(0, torrentInfo.getPieces().size());
                        return Flux.empty();
                    }
                    logger.info(this.identifier + " - Torrent: " + torrentInfo.getName() + ", the torrent download is not completed so we start accepting new incoming pieces.");
                    return pieceMessageReplay$;
                })
                .doOnNext(pieceMessage -> logger.trace(this.identifier + " - start saving piece-message: " + pieceMessage))
                // If I won't switch thread then I will block Redux thread.
                .filter(pieceMessage -> !havePiece(pieceMessage.getIndex()))
                .flatMap(this::writeBlock)
                .doOnNext(pieceMessage -> logger.trace(this.identifier + " - finished saving piece-message: " + pieceMessage))
                .doOnNext(__ -> {
                    // we may come here even if we got am empty flux but the download isn't yet completed.
                    if (areAllPiecesSaved()) {
                        logger.info(this.identifier + " - Torrent: " + torrentInfo + ", we finished to download the torrent and we dispatch a comeplete notification using redux.");
                        torrentStatusStore.dispatchNonBlocking(TorrentStatusAction.COMPLETED_DOWNLOADING_IN_PROGRESS);
                    }
                })
                // takeUntil will signal the last next signal he received and then he will send complete signal.
                .takeUntil(pieceEvent -> areAllPiecesSaved())
                .publish()
                .autoConnect(0);

        this.savedPieces$ = this.savedBlocks$.filter(torrentPieceChanged -> torrentPieceChanged.getTorrentPieceStatus().equals(TorrentPieceStatus.COMPLETED))
                .map(PieceEvent::getReceivedPiece)
                .map(PieceMessage::getIndex)
                .doOnNext(pieceIndex -> logger.debug(this.identifier + " - completed saving piece-index: " + pieceIndex))
                .distinct()
                .publish()
                .autoConnect(0);
    }

    @Override
    public Flux<ActualFile> getTorrentFiles() {
        return this.actualFileImplList;
    }

    @Override
    public BitSet getUpdatedPiecesStatus() {
        return this.piecesStatus;
    }

    @Override
    public TorrentInfo getTorrentInfo() {
        return this;
    }

    @Override
    public BitFieldMessage buildBitFieldMessage(Peer from, Peer to) {
        return new BitFieldMessage(from, to, this.piecesStatus);
    }

    @Override
    public boolean havePiece(int pieceIndex) {
        assert 0 <= pieceIndex;
        assert pieceIndex <= super.getPieces().size();

        return this.piecesStatus.get(pieceIndex);
    }

    @Override
    public String getDownloadPath() {
        return downloadPath;
    }

    @Override
    public Flux<PieceEvent> savedBlocks$() {
        return this.savedBlocks$;
    }

    @Override
    public Flux<Integer> savedPieces$() {
        return this.savedPieces$;
    }

    private Mono<FileSystemLink> deleteFileOnlyMono() {

        return this.actualFileImplList.flatMap(ActualFile::closeFileChannel)
                .as(actualFile$ -> {
                    if (this.isSingleFileTorrent())
                        return actualFile$.single()
                                .map(ActualFileImpl::getFilePath)
                                .flatMap(this::completelyDeleteFolder);
                    return actualFile$.collectList()
                            .map(__ -> this.downloadPath + this.getName())
                            .flatMap(this::completelyDeleteFolder);
                });
    }

    private boolean areAllPiecesSaved() {
        for (int i = 0; i < this.getPieces().size(); i++)
            if (!this.piecesStatus.get(i))
                return false;
        return true;
    }


    @Override
    public int[] getDownloadedBytesInPieces() {
        return this.downloadedBytesInPieces;
    }

    @Override
    public Mono<PieceMessage> buildPieceMessage(RequestMessage requestMessage) {
        return Mono.<Integer>create(sink -> {
            if (!havePiece(requestMessage.getIndex())) {
                sink.error(new PieceNotDownloadedYetException(requestMessage.getIndex()));
                return;
            }
            sink.success(super.getPieceLength(requestMessage.getIndex()));
        }).flatMap(pieceLength -> this.allocatorStore.createPieceMessage(requestMessage.getTo(), requestMessage.getFrom(), requestMessage.getIndex(),
                requestMessage.getBegin(), requestMessage.getBlockLength(), pieceLength)
                .flatMap(pieceMessage -> this.actualFileImplList.collectList().flatMap(actualFiles -> {
                    long from = super.getPieceStartPosition(requestMessage.getIndex()) + requestMessage.getBegin();
                    long to = from + requestMessage.getBlockLength();
                    int freeIndexInResultArray = pieceMessage.getAllocatedBlock().getOffset();

                    for (ActualFile actualFile : actualFiles) {
                        if (from != to)
                            if (actualFile.getFrom() <= from && from <= actualFile.getTo()) {
                                // to,from are taken from the requestMessage message object so "to-from" must be valid integer.
                                int howMuchToReadFromThisFile = (int) Math.min(actualFile.getTo() - from, to - from);
                                try {
                                    actualFile.readBlock(from, howMuchToReadFromThisFile,
                                            pieceMessage.getAllocatedBlock().getBlock(),
                                            freeIndexInResultArray);
                                    freeIndexInResultArray += howMuchToReadFromThisFile;
                                } catch (IOException e) {
                                    return Mono.error(e);
                                }
                                from += howMuchToReadFromThisFile;
                            }
                    }
                    return Mono.just(pieceMessage);
                })));
    }

    private Mono<PieceEvent> writeBlock(PieceMessage pieceMessage) {
        return this.actualFileImplList.collectList()
                .flatMap(actualFiles -> Mono.<PieceEvent>create(sink -> {
                    logger.debug(this.identifier + " - start writing block to FS: " + pieceMessage);
                    if (havePiece(pieceMessage.getIndex()) ||
                            this.downloadedBytesInPieces[pieceMessage.getIndex()] >= pieceMessage.getBegin() +
                                    pieceMessage.getAllocatedBlock().getLength()) {
                        // I already have the received block. I don't need it.
                        logger.debug(this.identifier + " - I already have this block: " + pieceMessage);
                        sink.success();
                        return;
                    }
                    long from = super.getPieceStartPosition(pieceMessage.getIndex()) + pieceMessage.getBegin();
                    long to = from + pieceMessage.getAllocatedBlock().getLength();

                    // from which position the ActualFileImpl object needs to write to filesystem from the given block array.
                    int arrayIndexFrom = pieceMessage.getAllocatedBlock().getOffset();

                    for (ActualFile actualFile : actualFiles)
                        if (actualFile.getFrom() <= from && from <= actualFile.getTo()) {
                            // (to-from)<=piece.length <= file.size , request.length<= Integer.MAX_VALUE
                            // so: (Math.min(to, actualFileImpl.getLength()) - from) <= Integer.MAX_VALUE
                            int howMuchToWriteFromArray = (int) Math.min(actualFile.getTo() - from, to - from);
                            try {
                                actualFile.writeBlock(from, pieceMessage.getAllocatedBlock().getBlock(), arrayIndexFrom,
                                        howMuchToWriteFromArray);
                            } catch (IOException e) {
                                sink.error(e);
                                return;
                            }
                            // increase 'from' because next time we will write to different position.
                            from += howMuchToWriteFromArray;
                            arrayIndexFrom += howMuchToWriteFromArray;
                            if (from == to)
                                break;
                        }

                    // update pieces status:
                    // Note: The download algorithm doesn't download multiple blocks of the same piece.
                    // so we won;t update the following cell concurrently.
                    this.downloadedBytesInPieces[pieceMessage.getIndex()] += pieceMessage.getAllocatedBlock().getLength();
                    long pieceLength = getPieceLength(pieceMessage.getIndex());
                    if (pieceLength < this.downloadedBytesInPieces[pieceMessage.getIndex()])
                        this.downloadedBytesInPieces[pieceMessage.getIndex()] = getPieceLength(pieceMessage.getIndex());

                    long howMuchWeWroteUntilNowInThisPiece = this.downloadedBytesInPieces[pieceMessage.getIndex()];
                    logger.debug(this.identifier + " - end writing block to FS: " + pieceMessage);
                    if (howMuchWeWroteUntilNowInThisPiece >= pieceLength) {
                        this.piecesStatus.set(pieceMessage.getIndex());
                        PieceEvent pieceEvent = new PieceEvent(TorrentPieceStatus.COMPLETED, pieceMessage);
                        sink.success(pieceEvent);
                    } else {
                        PieceEvent pieceEvent = new PieceEvent(TorrentPieceStatus.DOWNLOADING, pieceMessage);
                        sink.success(pieceEvent);
                    }
                }).doAfterSuccessOrError((__, ___) -> logger.debug(this.identifier + " - start cleaning-up piece-message-allocator: " + pieceMessage))
                        .flatMap(pieceEvent -> this.allocatorStore.free(pieceMessage.getAllocatedBlock()).map(__ -> pieceEvent))
                        .onErrorResume(throwable -> this.allocatorStore.free(pieceMessage.getAllocatedBlock()).flatMap(__ -> Mono.empty()))
                        .doAfterSuccessOrError((__, ___) -> logger.debug(this.identifier + " - finished cleaning-up piece-message-allocator: " + pieceMessage)));
    }

    private static Flux<ActualFile> createActiveTorrentFileList(TorrentInfo torrentInfo, String downloadPath) {
        return Flux.create(sink -> {
            String mainFolder = !torrentInfo.isSingleFileTorrent() ?
                    downloadPath + torrentInfo.getName() + File.separator :
                    downloadPath;

            long position = 0;
            for (TorrentFile torrentFile : torrentInfo.getFileList()) {
                String filePath = torrentFile
                        .getFileDirs()
                        .stream()
                        .collect(Collectors.joining(File.separator, mainFolder, ""));

                try {
                    SeekableByteChannel seekableByteChannel = createFile(filePath);
                    ActualFile actualFile = new ActualFileImpl(filePath, position, position + torrentFile.getFileLength(), seekableByteChannel);
                    sink.next(actualFile);
                } catch (IOException e) {
                    sink.error(e);
                    return;
                }
                position += torrentFile.getFileLength();
            }
            sink.complete();
        });
    }

    private static SeekableByteChannel createFile(String filePathToCreate) throws IOException {
        OpenOption[] options = {
                StandardOpenOption.WRITE,
                StandardOpenOption.CREATE_NEW,
                StandardOpenOption.SPARSE,
                StandardOpenOption.READ
        };
        return Files.newByteChannel(Paths.get(filePathToCreate), options);
    }

    private static void createFolders(TorrentInfo torrentInfo, String downloadPath) {
        // waitForMessage main folder for the download of the torrent.
        String mainFolder = !torrentInfo.isSingleFileTorrent() ?
                downloadPath + torrentInfo.getName() + File.separator :
                downloadPath;
        createFolder(mainFolder);

        // waitForMessage sub folders for the download of the torrent
        torrentInfo.getFileList()
                .stream()
                .map(christophedetroyer.torrent.TorrentFile::getFileDirs)
                .filter(fileAndFolders -> fileAndFolders.size() > 1)
                .map(fileAndFolders -> fileAndFolders.subList(0, fileAndFolders.size() - 1))
                .map(Collection::stream)
                .map(stringStream -> stringStream.collect(Collectors.joining(File.separator, mainFolder, "")))
                .distinct()
                .forEach(FileSystemLinkImpl::createFolder);
    }

    private static void createFolder(String path) {
        File file = new File(path);
        File parentFile = file.getParentFile();
        parentFile.mkdirs();
        file.mkdirs();
    }

    private Mono<FileSystemLinkImpl> completelyDeleteFolder(String directoryToBeDeleted) {
        try {
            completelyDeleteFolderRecursive(new File(directoryToBeDeleted));
        } catch (IOException e) {
            return Mono.error(e);
        }
        return Mono.just(this);
    }

    private void completelyDeleteFolderRecursive(File directoryToBeDeleted) throws IOException {
        File[] allContents = directoryToBeDeleted.listFiles();
        if (allContents != null) {
            for (File file : allContents) {
                completelyDeleteFolderRecursive(file);
            }
        }
        Files.delete(directoryToBeDeleted.toPath());
    }

    public Flux<TorrentStatusState> getCompleteDownload$() {
        return completeDownload$;
    }

    public Flux<TorrentStatusState> getRemoveTorrent$() {
        return removeTorrent$;
    }

    public Flux<TorrentStatusState> getRemoveFiles$() {
        return removeFiles$;
    }
}