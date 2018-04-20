package main.file.system;

import main.TorrentInfo;
import main.downloader.TorrentPieceChanged;
import main.peer.Peer;
import main.peer.peerMessages.BitFieldMessage;
import main.peer.peerMessages.PieceMessage;
import main.peer.peerMessages.RequestMessage;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.BitSet;
import java.util.List;

public interface TorrentFileSystemManager {
    String getDownloadPath();

    TorrentInfo getTorrentInfo();

    boolean havePiece(int pieceIndex);

    List<? extends TorrentFile> getTorrentFiles();

    BitSet getUpdatedPiecesStatus();

    BitFieldMessage buildBitFieldMessage(Peer from, Peer to);

    int minMissingPieceIndex();

    int maxMissingPieceIndex();

    int[] getPiecesEstimatedStatus();

    Mono<PieceMessage> buildPieceMessage(RequestMessage requestMessage);

    Flux<TorrentPieceChanged> savedBlockFlux();

    Flux<Integer> savedPieceFlux();

    Mono<Boolean> deleteActiveTorrentOnlyMono(String torrentInfoHash);

    Mono<Boolean> deleteFileOnlyMono(String torrentInfoHash);
}
