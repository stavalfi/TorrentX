package main.file.system;

import main.TorrentInfo;
import main.downloader.TorrentPieceChanged;
import main.peer.Peer;
import main.peer.peerMessages.BitFieldMessage;
import main.peer.peerMessages.PieceMessage;
import main.peer.peerMessages.RequestMessage;
import reactor.core.publisher.ConnectableFlux;
import reactor.core.publisher.Mono;

import java.util.List;

public interface TorrentFileSystemManager {
    String getDownloadPath();

    TorrentInfo getTorrentInfo();

    boolean havePiece(int pieceIndex);

    List<? extends TorrentFile> getTorrentFiles();

    BitFieldMessage buildBitFieldMessage(Peer from, Peer to);

    Mono<PieceMessage> buildPieceMessage(RequestMessage requestMessage);

    ConnectableFlux<TorrentPieceChanged> savedPiecesFlux();

    Mono<Boolean> deleteActiveTorrentOnlyMono(String torrentInfoHash);

    Mono<Boolean> deleteFileOnlyMono(String torrentInfoHash);

    int minMissingPieceIndex();

    int maxMissingPieceIndex();

    int[] getPiecesEstimatedStatus();
}
