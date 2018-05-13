package main.file.system;

import christophedetroyer.torrent.TorrentFile;
import main.TorrentInfo;
import main.downloader.PieceEvent;
import main.downloader.TorrentPieceStatus;
import main.peer.Peer;
import main.peer.peerMessages.BitFieldMessage;
import main.peer.peerMessages.PieceMessage;
import main.peer.peerMessages.RequestMessage;
import main.torrent.status.Status;
import main.torrent.status.StatusChanger;
import main.torrent.status.StatusType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.File;
import java.io.IOException;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.stream.Collectors;

public class FileSystemLinkImpl extends TorrentInfo implements FileSystemLink {

    private final List<ActualFile> actualFileImplList;
    private final BitSet piecesStatus;
    private final long[] downloadedBytesInPieces;
    private final String downloadPath;
    private StatusChanger statusChanger;
    private Flux<Integer> savedPiecesFlux;
    private Flux<PieceEvent> savedBlocksFlux;
    private Mono<FileSystemLink> notifyWhenActiveTorrentDeleted;
    private Mono<FileSystemLink> notifyWhenFilesDeleted;

    public FileSystemLinkImpl(TorrentInfo torrentInfo, String downloadPath,
                              StatusChanger statusChanger,
                              Flux<PieceMessage> peerResponsesFlux) throws IOException {
        super(torrentInfo);
        this.downloadPath = downloadPath;
        this.statusChanger = statusChanger;
        this.piecesStatus = new BitSet(getPieces().size());
        this.downloadedBytesInPieces = new long[getPieces().size()];

        createFolders(torrentInfo, downloadPath);

        this.actualFileImplList = createActiveTorrentFileList(torrentInfo, downloadPath);

        this.notifyWhenActiveTorrentDeleted = this.statusChanger
                .getState$()
                .filter(Status::isTorrentRemoved)
                .take(1)
                .flatMap(__ -> deleteFileOnlyMono())
                .replay(1)
                .autoConnect(0)
                .single();

        this.notifyWhenFilesDeleted = this.statusChanger
                .getState$()
                .filter(Status::isFilesRemoved)
                .take(1)
                .flatMap(__ -> deleteActiveTorrentOnlyMono())
                .replay(1)
                .autoConnect(0)
                .single();

        this.savedBlocksFlux = this.statusChanger
                .getLatestState$()
                .map(Status::isCompletedDownloading)
                .flatMapMany(isCompletedDownloading -> {
                    if (isCompletedDownloading) {
                        this.piecesStatus.set(0, torrentInfo.getPieces().size());
                        return Mono.empty();
                    }
                    return peerResponsesFlux;
                })
                .filter(pieceMessage -> !havePiece(pieceMessage.getIndex()))
                .flatMap(this::writeBlock)
                .flatMap(pieceEvent -> {
                    if (minMissingPieceIndex() == -1)
                        return this.statusChanger.changeState(StatusType.COMPLETED_DOWNLOADING)
                                .map(__ -> pieceEvent);
                    return Mono.just(pieceEvent);
                })
                // takeUntil will signal the last next signal he received and then he will send complete signal.
                .takeUntil(pieceEvent -> minMissingPieceIndex() == -1)
                .publish()
                .autoConnect(0);

        this.savedPiecesFlux = this.savedBlocksFlux
                .filter(torrentPieceChanged -> torrentPieceChanged.getTorrentPieceStatus().equals(TorrentPieceStatus.COMPLETED))
                .map(PieceEvent::getReceivedPiece)
                .map(PieceMessage::getIndex)
                .distinct()
                .publish()
                .autoConnect(0);
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

    public Mono<FileSystemLink> deleteActiveTorrentOnlyMono() {
        return Flux.fromIterable(this.actualFileImplList)
                .flatMap(ActualFile::closeFileChannel)
                .collectList()
                .flatMap(activeTorrentFiles -> {
                    boolean deletedActiveTorrent = ActiveTorrents.getInstance()
                            .deleteActiveTorrentOnly(getTorrentInfoHash());
                    if (deletedActiveTorrent)
                        return Mono.just(this);
                    return Mono.error(new Exception("FileSystemLinkImpl object not exist."));
                });
    }

    public Mono<FileSystemLink> deleteFileOnlyMono() {
        return Flux.fromIterable(this.actualFileImplList)
                .flatMap(ActualFile::closeFileChannel)
                .collectList()
                .flatMap(activeTorrentFiles -> {
                    if (this.isSingleFileTorrent()) {
                        String singleFilePath = this.actualFileImplList.get(0).getFilePath();
                        return completelyDeleteFolder(singleFilePath);
                    }
                    // I will delete this file at the next operator.
                    String torrentDirectoryPath = this.downloadPath + this.getName();
                    return completelyDeleteFolder(torrentDirectoryPath);
                });
    }

    @Override
    public synchronized int minMissingPieceIndex() {
        for (int i = 0; i < this.getPieces().size(); i++)
            if (!this.piecesStatus.get(i))
                return i;
        return -1;
    }

    @Override
    public int maxMissingPieceIndex() {
        for (int i = this.getPieces().size() - 1; i >= 0; i--)
            if (!this.piecesStatus.get(i))
                return i;
        return -1;
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

        return BlocksAllocatorImpl.getInstance()
                .createPieceMessage(requestMessage.getTo(), requestMessage.getFrom(),
                        requestMessage.getIndex(), requestMessage.getBegin(),
                        requestMessage.getBlockLength(), pieceLength)
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
        if (havePiece(pieceMessage.getIndex()) ||
                this.downloadedBytesInPieces[pieceMessage.getIndex()] > pieceMessage.getBegin() +
                        pieceMessage.getAllocatedBlock().getLength())
            // I already have the received block. I don't need it.
            return Mono.empty();

        return Mono.create(sink -> {
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
        });
    }

    private List<ActualFile> createActiveTorrentFileList(TorrentInfo torrentInfo, String downloadPath) throws IOException {
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
            SeekableByteChannel seekableByteChannel = createFile(filePath);
            ActualFile actualFile = new ActualFileImpl(filePath, position, position + torrentFile.getFileLength(),
                    seekableByteChannel);
            actualFileList.add(actualFile);
            position += torrentFile.getFileLength();
        }
        return actualFileList;
    }

    private SeekableByteChannel createFile(String filePathToCreate) throws IOException {
        OpenOption[] options = {
                StandardOpenOption.WRITE,
                StandardOpenOption.CREATE_NEW,
                StandardOpenOption.SPARSE,
                StandardOpenOption.READ
        };
        return Files.newByteChannel(Paths.get(filePathToCreate), options);
    }

    private void createFolders(TorrentInfo torrentInfo, String downloadPath) {
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
                .map(strings -> strings.stream())
                .map(stringStream -> stringStream.collect(Collectors.joining(File.separator, mainFolder, "")))
                .distinct()
                .forEach(this::createFolder);
    }

    private void createFolder(String path) {
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

    public Mono<FileSystemLink> getNotifyWhenActiveTorrentDeleted() {
        return this.notifyWhenActiveTorrentDeleted;
    }

    public Mono<FileSystemLink> getNotifyWhenFilesDeleted() {
        return this.notifyWhenFilesDeleted;
    }
}