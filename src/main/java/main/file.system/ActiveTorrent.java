package main.file.system;

import christophedetroyer.torrent.TorrentFile;
import main.App;
import main.TorrentInfo;
import main.torrent.status.TorrentStatus;
import main.downloader.TorrentPieceChanged;
import main.downloader.TorrentPieceStatus;
import main.peer.Peer;
import main.peer.peerMessages.BitFieldMessage;
import main.peer.peerMessages.PieceMessage;
import main.peer.peerMessages.RequestMessage;
import reactor.core.publisher.ConnectableFlux;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.stream.Collectors;

public class ActiveTorrent extends TorrentInfo implements TorrentFileSystemManager {

    private final List<ActiveTorrentFile> activeTorrentFileList;
    private final BitSet piecesStatus;
    private final long[] piecesPartialStatus;
    private final String downloadPath;
    private TorrentStatus torrentStatus;
    private ConnectableFlux<TorrentPieceChanged> startListenForIncomingPiecesFlux;

    public ActiveTorrent(TorrentInfo torrentInfo, String downloadPath,
                         TorrentStatus torrentStatus,
                         Flux<PieceMessage> peerResponsesFlux) {
        super(torrentInfo);
        this.torrentStatus = torrentStatus;
        this.downloadPath = downloadPath;
        this.piecesStatus = new BitSet(getPieces().size());
        this.piecesPartialStatus = new long[getPieces().size()];
        this.activeTorrentFileList = createActiveTorrentFileList(torrentInfo, downloadPath);

        this.startListenForIncomingPiecesFlux = peerResponsesFlux
                .flatMap(pieceMessage -> writeBlock(pieceMessage))
                .publish();

        this.torrentStatus.getStatusTypeFlux()
                .subscribe(torrentStatusType -> {
                    switch (torrentStatusType) {
                        case START_DOWNLOAD:
                            this.startListenForIncomingPiecesFlux.connect();
                    }
                });
    }

    @Override
    public List<? extends main.file.system.TorrentFile> getTorrentFiles() {
        return this.activeTorrentFileList;
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
    public ConnectableFlux<TorrentPieceChanged> startListenForIncomingPiecesFlux() {
        return this.startListenForIncomingPiecesFlux;
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
                    for (int i = 0; i < tempResult.length; i++)
                        result[freeIndexInResultArray++] = tempResult[i];
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

    private Mono<ActiveTorrent> updatePieceAsCompleted(int pieceIndex) {
        return Mono.<ActiveTorrent>create(sink -> {
            this.piecesStatus.set(pieceIndex);
            Mono.just(this);
        }).doOnSuccess(activeTorrent -> {
            // update in mongo db
        });
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

    private Mono<TorrentPieceChanged> writeBlock(PieceMessage pieceMessage) {
        return Mono.<TorrentPieceChanged>create(sink -> {
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

            // update pieces partial status array:
            // WARNING: this line *only* must be synchronized among multiple threads!
            this.piecesPartialStatus[pieceMessage.getIndex()] += pieceMessage.getBlock().length;

            // update pieces status:
            // there maybe multiple writes of the same pieceRequest during one execution...
            long pieceLength = getPieceLength();
            // check if we downloaded a block from the last piece.
            // if yes, it's length can be less than a other pieces.
            if (pieceMessage.getIndex() == this.getPieces().size() - 1) {
                int lastPieceLength = (int) Math.min(getPieceLength(),
                        getTotalSize() - (getPieces().size() - 1) * getPieceLength());
                pieceLength = lastPieceLength;
            }

            long howMuchWeWroteUntilNowInThisPiece = this.piecesPartialStatus[pieceMessage.getIndex()];
            if (howMuchWeWroteUntilNowInThisPiece >= pieceLength) {
                this.piecesStatus.set(pieceMessage.getIndex());
                TorrentPieceChanged torrentPieceChanged = new TorrentPieceChanged(pieceMessage.getIndex(),
                        this.getPieces().get(pieceMessage.getIndex()),
                        TorrentPieceStatus.COMPLETED);
                sink.success(torrentPieceChanged);
            } else {
                TorrentPieceChanged torrentPieceChanged = new TorrentPieceChanged(pieceMessage.getIndex(),
                        this.getPieces().get(pieceMessage.getIndex()),
                        TorrentPieceStatus.DOWNLOADING);
                sink.success(torrentPieceChanged);
            }
        }).subscribeOn(Schedulers.single());
    }
}
