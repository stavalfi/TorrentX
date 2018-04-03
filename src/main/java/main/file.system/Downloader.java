package main.file.system;

import main.TorrentInfo;
import main.downloader.TorrentPieceChanged;
import main.peer.Peer;
import main.peer.peerMessages.BitFieldMessage;
import main.peer.peerMessages.PieceMessage;
import main.peer.peerMessages.RequestMessage;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

public interface Downloader {
    String getDownloadPath();

    Flux<TorrentPieceChanged> downloadAsync(Flux<PieceMessage> peerResponsesFlux);

    boolean havePiece(int pieceIndex);

    Mono<PieceMessage> readBlock(RequestMessage requestMessage);

    BitFieldMessage getAllPiecesStatus(Peer from, Peer to);

    List<? extends TorrentFile> getTorrentFiles();

    TorrentInfo getTorrentInfo();
}
