package main.file.system;

import main.TorrentInfo;
import main.downloader.PieceEvent;
import main.peer.Peer;
import main.peer.peerMessages.BitFieldMessage;
import main.peer.peerMessages.PieceMessage;
import main.peer.peerMessages.RequestMessage;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.BitSet;
import java.util.List;

public interface FileSystemLink {
    String getDownloadPath();

    TorrentInfo getTorrentInfo();

    boolean havePiece(int pieceIndex);

    List<ActualFile> getTorrentFiles();

    BitSet getUpdatedPiecesStatus();

    BitFieldMessage buildBitFieldMessage(Peer from, Peer to);

    int minMissingPieceIndex();

    int maxMissingPieceIndex();

    long[] getDownloadedBytesInPieces();

    /**
     * read from the file and fill up an allocatedBlock with what it read.
     * It isn't this method responsibility to allocate an AllocatedBlock because
     * it can't know when to free it.
     *
     * @param requestMessage what to read
     * @return pieceMessage
     */
    Mono<PieceMessage> buildPieceMessage(RequestMessage requestMessage);

    Flux<PieceEvent> savedBlockFlux();

    Flux<Integer> savedPieceFlux();

    Mono<FileSystemLink> deleteActiveTorrentOnlyMono();

    Mono<FileSystemLink> deleteFileOnlyMono();

    Mono<FileSystemLink> getNotifyWhenActiveTorrentDeleted();

    Mono<FileSystemLink> getNotifyWhenFilesDeleted();
}
