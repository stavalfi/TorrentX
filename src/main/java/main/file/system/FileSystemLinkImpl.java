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
import reactor.core.scheduler.Schedulers;
import redux.store.Store;

import java.io.File;
import java.io.IOException;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class FileSystemLinkImpl extends TorrentInfo implements FileSystemLink {
    private static Logger logger = LoggerFactory.getLogger(FileSystemLinkImpl.class);

    private final List<ActualFile> actualFileImplList;
    private final BitSet piecesStatus;
    private final long[] downloadedBytesInPieces;
    private final String downloadPath;
    private Flux<Integer> savedPiecesFlux;
    private Flux<PieceEvent> savedBlocksFlux;
    private AllocatorStore allocatorStore;
    private Flux<TorrentStatusState> completeDownload$;
    private Flux<TorrentStatusState> removeTorrent$;
    private Flux<TorrentStatusState> removeFiles$;

    private FileSystemLinkImpl(TorrentInfo torrentInfo, String downloadPath,
                               List<ActualFile> actualFileList,
                               AllocatorStore allocatorStore,
                               Store<TorrentStatusState, TorrentStatusAction> torrentStatusStore,
                               Flux<PieceMessage> peerResponsesFlux) {
        super(torrentInfo);
        this.allocatorStore = allocatorStore;
        this.downloadPath = downloadPath;
        this.piecesStatus = new BitSet(getPieces().size());
        this.downloadedBytesInPieces = new long[getPieces().size()];
        this.actualFileImplList = actualFileList;

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

        this.savedBlocksFlux = torrentStatusStore.latestState$()
                .map(torrentStatusState -> torrentStatusState.fromAction(TorrentStatusAction.COMPLETED_DOWNLOADING_WIND_UP))
                .publishOn(Schedulers.elastic())
                .flatMapMany(isCompletedDownloading -> {
                    if (isCompletedDownloading) {
                        logger.info("Torrent: " + torrentInfo + ", the torrent download is already completed so we update our internal state that all the pieces are completed.");
                        this.piecesStatus.set(0, torrentInfo.getPieces().size());
                        return Flux.empty();
                    }
                    return peerResponsesFlux;
                })
                .doOnNext(pieceMessage -> logger.trace("start saving piece-message: " + pieceMessage))
                // If I won't switch thread then I will block Redux thread.
                .filter(pieceMessage -> !havePiece(pieceMessage.getIndex()))
                .flatMap(this::writeBlock)
                .doOnNext(pieceMessage -> logger.trace("finished saving piece-message: " + pieceMessage))
                .doOnNext(__ -> {
                    // we may come here even if we got am empty flux but the download isn't yet completed.
                    if (areAllPiecesSaved()) {
                        logger.info("Torrent: " + torrentInfo + ", we finished to download the torrent and we dispatch a comeplete notification using redux.");
                        torrentStatusStore.dispatchNonBlocking(TorrentStatusAction.COMPLETED_DOWNLOADING_IN_PROGRESS);
                    }
                })
                // takeUntil will signal the last next signal he received and then he will send complete signal.
                .takeUntil(pieceEvent -> areAllPiecesSaved())
                .publish()
                .autoConnect(0);

        this.savedPiecesFlux = this.savedBlocksFlux.filter(torrentPieceChanged -> torrentPieceChanged.getTorrentPieceStatus().equals(TorrentPieceStatus.COMPLETED))
                .map(PieceEvent::getReceivedPiece)
                .map(PieceMessage::getIndex)
                .doOnNext(pieceIndex -> logger.debug("completed saving piece-index: " + pieceIndex))
                .distinct()
                .publish()
                .autoConnect(0);
    }

    public static Mono<FileSystemLink> create(TorrentInfo torrentInfo, String downloadPath,
                                              AllocatorStore allocatorStore,
                                              Store<TorrentStatusState, TorrentStatusAction> torrentStatusStore,
                                              Flux<PieceMessage> peerResponsesFlux) {
        return Mono.just(torrentInfo)
                .doOnNext(__ -> createFolders(torrentInfo, downloadPath))
                .flatMap(__ -> createActiveTorrentFileList(torrentInfo, downloadPath))
                .map(actualFileList -> new FileSystemLinkImpl(torrentInfo, downloadPath, actualFileList, allocatorStore, torrentStatusStore, peerResponsesFlux));
    }

    @Override
    public List<ActualFile> getTorrentFiles() {
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
    public Flux<PieceEvent> savedBlockFlux() {
        return this.savedBlocksFlux;
    }

    @Override
    public Flux<Integer> savedPieceFlux() {
        return this.savedPiecesFlux;
    }

    private Mono<FileSystemLink> deleteFileOnlyMono() {
        return Flux.fromIterable(this.actualFileImplList)
                .flatMap(ActualFile::closeFileChannel)
                .collectList()
                .flatMap(activeTorrentFiles -> {
                    if (this.isSingleFileTorrent()) {
                        String singleFilePath = this.actualFileImplList.get(0).getFilePath();
                        return completelyDeleteFolder(singleFilePath);
                    }
                    String torrentDirectoryPath = this.downloadPath + this.getName();
                    return completelyDeleteFolder(torrentDirectoryPath);
                });
    }

    private boolean areAllPiecesSaved() {
        for (int i = 0; i < this.getPieces().size(); i++)
            if (!this.piecesStatus.get(i))
                return false;
        return true;
    }


    @Override
    public long[] getDownloadedBytesInPieces() {
        return this.downloadedBytesInPieces;
    }

    @Override
    public Mono<PieceMessage> buildPieceMessage(RequestMessage requestMessage) {
        if (!havePiece(requestMessage.getIndex()))
            return Mono.error(new PieceNotDownloadedYetException(requestMessage.getIndex()));

        int pieceLength = super.getPieceLength(requestMessage.getIndex());

        return this.allocatorStore.createPieceMessage(requestMessage.getTo(), requestMessage.getFrom(), requestMessage.getIndex(), requestMessage.getBegin(), requestMessage.getBlockLength(), pieceLength)
                .flatMap(pieceMessage -> {
                    long from = super.getPieceStartPosition(requestMessage.getIndex()) + requestMessage.getBegin();
                    long to = from + requestMessage.getBlockLength();
                    int freeIndexInResultArray = pieceMessage.getAllocatedBlock().getOffset();

                    for (ActualFile actualFile : this.actualFileImplList) {
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
                });
    }

    private Mono<PieceEvent> writeBlock(PieceMessage pieceMessage) {
        return Mono.<PieceEvent>create(sink -> {
            if (havePiece(pieceMessage.getIndex()) ||
                    this.downloadedBytesInPieces[pieceMessage.getIndex()] > pieceMessage.getBegin() +
                            pieceMessage.getAllocatedBlock().getLength()) {
                // I already have the received block. I don't need it.
                sink.success();
                return;
            }
            long from = super.getPieceStartPosition(pieceMessage.getIndex()) + pieceMessage.getBegin();
            long to = from + pieceMessage.getAllocatedBlock().getLength();

            // from which position the ActualFileImpl object needs to write to filesystem from the given block array.
            int arrayIndexFrom = pieceMessage.getAllocatedBlock().getOffset();

            for (ActualFile actualFile : this.actualFileImplList)
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
            if (howMuchWeWroteUntilNowInThisPiece >= pieceLength) {
                this.piecesStatus.set(pieceMessage.getIndex());
                PieceEvent pieceEvent = new PieceEvent(TorrentPieceStatus.COMPLETED, pieceMessage);
                sink.success(pieceEvent);
            } else {
                PieceEvent pieceEvent = new PieceEvent(TorrentPieceStatus.DOWNLOADING, pieceMessage);
                sink.success(pieceEvent);
            }
        }).doAfterSuccessOrError((__, ___) -> logger.trace("start cleaning-up piece-message-allocator: " + pieceMessage))
                .doAfterSuccessOrError((__, ___) -> this.allocatorStore.freeNonBlocking(pieceMessage.getAllocatedBlock()))
                .doAfterSuccessOrError((__, ___) -> logger.trace("finished cleaning-up piece-message-allocator: " + pieceMessage));
    }

    private static Mono<List<ActualFile>> createActiveTorrentFileList(TorrentInfo torrentInfo, String downloadPath) {
        String mainFolder = !torrentInfo.isSingleFileTorrent() ?
                downloadPath + torrentInfo.getName() + File.separator :
                downloadPath;

        // waitForMessage activeTorrentFile list
        long position = 0;
        List<ActualFile> actualFileList = new ArrayList<>();
        for (TorrentFile torrentFile : torrentInfo.getFileList()) {
            String filePath = torrentFile
                    .getFileDirs()
                    .stream()
                    .collect(Collectors.joining(File.separator, mainFolder, ""));
            SeekableByteChannel seekableByteChannel = null;
            try {
                seekableByteChannel = createFile(filePath);
            } catch (IOException e) {
                return Mono.error(e);
            }
            ActualFile actualFile = new ActualFileImpl(filePath, position, position + torrentFile.getFileLength(),
                    seekableByteChannel);
            actualFileList.add(actualFile);
            position += torrentFile.getFileLength();
        }
        return Mono.just(actualFileList);
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