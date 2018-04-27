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

public interface TorrentFileSystemManager {
	String getDownloadPath();

	TorrentInfo getTorrentInfo();

	boolean havePiece(int pieceIndex);

	List<? extends TorrentFile> getTorrentFiles();

	BitSet getUpdatedPiecesStatus();

	BitFieldMessage buildBitFieldMessage(Peer from, Peer to);

	int minMissingPieceIndex();

	int maxMissingPieceIndex();

	long[] getDownloadedBytesInPieces();

	Mono<PieceMessage> buildPieceMessage(RequestMessage requestMessage);

	Flux<PieceEvent> savedBlockFlux();

	Flux<Integer> savedPieceFlux();

	Mono<ActiveTorrent> deleteActiveTorrentOnlyMono();

	Mono<ActiveTorrent> deleteFileOnlyMono();

	Mono<ActiveTorrent> getNotifyWhenActiveTorrentDeleted();

	Mono<ActiveTorrent> getNotifyWhenFilesDeleted();
}
