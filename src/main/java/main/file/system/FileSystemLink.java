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

    Flux<ActualFile> getTorrentFiles();

    BitSet getUpdatedPiecesStatus();

    BitFieldMessage buildBitFieldMessage(Peer from, Peer to);

    int[] getDownloadedBytesInPieces();

    /**
     * read from the file and fill up an allocatedBlock with what it read.
     * It isn't this method responsibility to allocate an AllocatedBlock because
     * it can't know when to free it.
     *
     * @param requestMessage what to read
     * @return pieceMessage
     */
    Mono<PieceMessage> buildPieceMessage(RequestMessage requestMessage);

    Flux<PieceEvent> savedBlocks$();

    Flux<Integer> savedPieces$();
}
