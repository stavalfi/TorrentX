package main.file.system;

import christophedetroyer.torrent.TorrentFile;
import main.App;
import main.TorrentInfo;
import main.downloader.PieceEvent;
import main.downloader.TorrentPieceStatus;
import main.peer.Peer;
import main.peer.peerMessages.BitFieldMessage;
import main.peer.peerMessages.PieceMessage;
import main.peer.peerMessages.RequestMessage;
import main.torrent.status.TorrentStatusController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.stream.Collectors;

public class ActiveTorrent extends TorrentInfo implements TorrentFileSystemManager {

    private final List<ActiveTorrentFile> activeTorrentFileList;
    private final BitSet piecesStatus;
    private final int[] downloadedBytesInPieces;
    private final String downloadPath;
    private TorrentStatusController torrentStatusController;
    private Flux<Integer> savedPieceFlux;
    private Flux<PieceEvent> startListenForIncomingPiecesFlux;

    public ActiveTorrent(TorrentInfo torrentInfo, String downloadPath,
                         TorrentStatusController torrentStatusController,
                         Flux<PieceMessage> peerResponsesFlux) {
        super(torrentInfo);
        this.downloadPath = downloadPath;
        this.torrentStatusController = torrentStatusController;
        this.piecesStatus = new BitSet(getPieces().size());
        this.downloadedBytesInPieces = new int[getPieces().size()];

        createFolders(torrentInfo, downloadPath);
        createFiles(torrentInfo, downloadPath).block();

        this.activeTorrentFileList = createActiveTorrentFileList(torrentInfo, downloadPath);

        this.torrentStatusController.isFilesRemovedFlux()
                .filter(isFilesRemoved -> isFilesRemoved)
                // I can be here only once.
                .flatMap(__ -> deleteFileOnlyMono(torrentInfo.getTorrentInfoHash()))
                .publish()
                .autoConnect(0);

        this.torrentStatusController.isTorrentRemovedFlux()
                .filter(isTorrentRemoved -> isTorrentRemoved)
                // I can be here only once.
                .flatMap(__ -> deleteActiveTorrentOnlyMono(torrentInfo.getTorrentInfoHash()))
                .publish()
                .autoConnect(0);

        this.startListenForIncomingPiecesFlux =
                peerResponsesFlux.filter(pieceMessage -> !havePiece(pieceMessage.getIndex()))
                        .flatMap(pieceMessage -> writeBlock(pieceMessage))
                        .publish()
                        .autoConnect(0);

        this.savedPieceFlux = this.startListenForIncomingPiecesFlux
                .filter(torrentPieceChanged -> torrentPieceChanged.getTorrentPieceStatus().equals(TorrentPieceStatus.COMPLETED))
                .map(PieceEvent::getReceivedPiece)
                .map(PieceMessage::getIndex)
                .distinct()
                .publish()
                .autoConnect(0);
    }

    @Override
    public List<? extends main.file.system.TorrentFile> getTorrentFiles() {
        return this.activeTorrentFileList;
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
        return this.piecesStatus.get(pieceIndex);
    }

    @Override
    public String getDownloadPath() {
        return downloadPath;
    }

    @Override
    public Flux<PieceEvent> savedBlockFlux() {
        return this.startListenForIncomingPiecesFlux;
    }

    @Override
    public Flux<Integer> savedPieceFlux() {
        return this.savedPieceFlux;
    }

    public Mono<Boolean> deleteActiveTorrentOnlyMono(String torrentInfoHash) {
        boolean deletedActiveTorrent = ActiveTorrents.getInstance()
                .deleteActiveTorrentOnly(torrentInfoHash);
        return Mono.just(deletedActiveTorrent);
    }

    public Mono<Boolean> deleteFileOnlyMono(String torrentInfoHash) {
        return ActiveTorrents.getInstance()
                .findActiveTorrentByHashMono(torrentInfoHash)
                .map(activeTorrentOptional -> {
                    activeTorrentOptional.ifPresent(activeTorrent -> {
                        activeTorrent.getTorrentFiles()
                                .stream()
                                .map(main.file.system.TorrentFile::getFilePath)
                                .map(File::new)
                                .forEach(this::completelyDeleteFolder);
                        String filePath = activeTorrent.getDownloadPath() + "/" + activeTorrent.getName();
                        File mainFile = new File(filePath);
                        completelyDeleteFolder(mainFile);
                    });
                    return activeTorrentOptional.isPresent();
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
    public int[] getDownloadedBytesInPieces() {
        return this.downloadedBytesInPieces;
    }

    @Override
    public Mono<PieceMessage> buildPieceMessage(RequestMessage requestMessage) {
        return Mono.<PieceMessage>create(sink -> {
            if (!havePiece(requestMessage.getIndex())) {
                sink.error(new PieceNotDownloadedYetException(requestMessage.getIndex()));
                return;
            }
            byte[] result = new byte[requestMessage.getBlockLength()];
            int freeIndexInResultArray = 0;

            long from = requestMessage.getIndex() * this.getPieceLength() + requestMessage.getBegin();
            long to = requestMessage.getIndex() * this.getPieceLength()
                    + requestMessage.getBegin() + requestMessage.getBlockLength();

            for (ActiveTorrentFile activeTorrentFile : this.activeTorrentFileList) {
                if (activeTorrentFile.getFrom() <= from && from <= activeTorrentFile.getTo()) {
                    int howMuchToReadFromThisFile = (int) Math.min(requestMessage.getBlockLength(), (to - from));
                    byte[] tempResult;
                    try {
                        tempResult = activeTorrentFile.readBlock(from, howMuchToReadFromThisFile);
                    } catch (IOException e) {
                        sink.error(e);
                        return;
                    }
                    for (byte aTempResult : tempResult)
                        result[freeIndexInResultArray++] = aTempResult;

                    from += howMuchToReadFromThisFile;
                    if (from == to) {
                        PieceMessage pieceMessage = new PieceMessage(requestMessage.getTo(), requestMessage.getFrom(),
                                requestMessage.getIndex(), requestMessage.getBegin(), result);
                        sink.success(pieceMessage);
                        return;
                    }
                }
            }
        }).subscribeOn(App.MyScheduler);
    }

    private List<ActiveTorrentFile> createActiveTorrentFileList(TorrentInfo torrentInfo, String downloadPath) {
        String mainFolder = !torrentInfo.isSingleFileTorrent() ?
                downloadPath + "/" + torrentInfo.getName() + "/" :
                downloadPath + "/";

        // create activeTorrentFile list
        long position = 0;
        List<ActiveTorrentFile> activeTorrentFileList = new ArrayList<>();
        for (TorrentFile torrentFile : torrentInfo.getFileList()) {
            String filePath = torrentFile
                    .getFileDirs()
                    .stream()
                    .collect(Collectors.joining("/", mainFolder, ""));
            ActiveTorrentFile activeTorrentFile =
                    new ActiveTorrentFile(filePath, position, position + torrentFile.getFileLength());
            activeTorrentFileList.add(activeTorrentFile);
            position += torrentFile.getFileLength();
        }
        return activeTorrentFileList;
    }

    private Mono<PieceEvent> writeBlock(PieceMessage pieceMessage) {

        return Mono.<PieceEvent>create(sink -> {
            long from = pieceMessage.getIndex() * this.getPieceLength() + pieceMessage.getBegin();
            long to = pieceMessage.getIndex() * this.getPieceLength()
                    + pieceMessage.getBegin() + pieceMessage.getBlock().length;
            int arrayIndexFrom = 0; // where ActiveTorrentFile object needs to write from in the array.

            for (ActiveTorrentFile activeTorrentFile : this.activeTorrentFileList)
                if (activeTorrentFile.getFrom() <= from && from <= activeTorrentFile.getTo()) {
                    int howMuchToWriteFromArray = (int) (Math.min(to, activeTorrentFile.getTo()) - from);
                    try {
                        activeTorrentFile.writeBlock(from, pieceMessage.getBlock(), arrayIndexFrom, howMuchToWriteFromArray);
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
            // there maybe multiple writes of the same pieceRequest during one execution...
            long pieceLength = getPieceLength(pieceMessage.getIndex());
            this.downloadedBytesInPieces[pieceMessage.getIndex()] += pieceMessage.getBlock().length;
            if (pieceLength < this.downloadedBytesInPieces[pieceMessage.getIndex()])
                this.downloadedBytesInPieces[pieceMessage.getIndex()] = getPieceLength(pieceMessage.getIndex());

            long howMuchWeWroteUntilNowInThisPiece = this.downloadedBytesInPieces[pieceMessage.getIndex()];
            if (howMuchWeWroteUntilNowInThisPiece >= pieceLength) {
                // update pieces partial status array:
                // TODO: WARNING: this line *only* must be synchronized among multiple threads!
                this.piecesStatus.set(pieceMessage.getIndex());
                PieceEvent pieceEvent = new PieceEvent(TorrentPieceStatus.COMPLETED, pieceMessage);
                sink.success(pieceEvent);

                if (minMissingPieceIndex() == -1)
                    this.torrentStatusController.completedDownloading();
            } else {
                // update pieces partial status array:
                // TODO: WARNING: this line *only* must be synchronized among multiple threads!
                PieceEvent pieceEvent = new PieceEvent(TorrentPieceStatus.DOWNLOADING, pieceMessage);
                sink.success(pieceEvent);
            }
        }).subscribeOn(Schedulers.single());
    }

    private void createFolders(TorrentInfo torrentInfo, String downloadPath) {
        // create main folder for the download of the torrent.
        String mainFolder = !torrentInfo.isSingleFileTorrent() ?
                downloadPath + torrentInfo.getName() + "/" :
                downloadPath;
        createFolder(mainFolder);

        // create sub folders for the download of the torrent
        torrentInfo.getFileList()
                .stream()
                .map(christophedetroyer.torrent.TorrentFile::getFileDirs)
                .filter(folders -> folders.size() > 1)
                .map(folders -> folders.subList(0, folders.size() - 2))
                .map(List::stream)
                .map(stringStream -> stringStream.collect(Collectors.joining("/", mainFolder, "")))
                .forEach(folderPath -> createFolder(folderPath));
    }

    private Mono<ActiveTorrent> createFiles(TorrentInfo torrentInfo, String downloadPath) {
        String mainFolder = !torrentInfo.isSingleFileTorrent() ?
                downloadPath + "/" + torrentInfo.getName() + "/" :
                downloadPath + "/";
        // create files in each folder.
        return Mono.<ActiveTorrent>create(sink -> {
            for (christophedetroyer.torrent.TorrentFile torrentFile : torrentInfo.getFileList()) {
                String filePath = torrentFile
                        .getFileDirs()
                        .stream()
                        .collect(Collectors.joining("/", mainFolder, ""));
                try {
                    createFile(filePath, torrentFile.getFileLength());
                } catch (IOException e) {
                    sink.error(e);
                    return;
                }
            }
            sink.success(this);
        }).doOnError(throwable -> completelyDeleteFolder(new File(mainFolder)));
    }

    private void createFolder(String path) {
        File file = new File(path);
        File parentFile = file.getParentFile();
        parentFile.mkdirs();
        file.mkdirs();
    }

    private void createFile(String filePathToCreate, long length) throws IOException {
        RandomAccessFile randomAccessFile = new RandomAccessFile(filePathToCreate, "rw");
        randomAccessFile.setLength(length);
    }


    private boolean completelyDeleteFolder(File directoryToBeDeleted) {
        File[] allContents = directoryToBeDeleted.listFiles();
        if (allContents != null) {
            for (File file : allContents) {
                completelyDeleteFolder(file);
            }
        }
        return directoryToBeDeleted.delete();
    }
}
