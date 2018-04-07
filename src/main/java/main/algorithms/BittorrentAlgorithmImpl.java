package main.algorithms;

import main.TorrentInfo;
import main.downloader.TorrentPieceChanged;
import main.downloader.TorrentPieceStatus;
import main.file.system.PieceNotDownloadedYetException;
import main.file.system.TorrentFileSystemManager;
import main.peer.PeersCommunicator;
import main.torrent.status.TorrentStatus;
import reactor.core.publisher.ConnectableFlux;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class BittorrentAlgorithmImpl implements BittorrentAlgorithm {
    private ConnectableFlux<TorrentPieceChanged> startUploadFlux;
    private ConnectableFlux<TorrentPieceChanged> startDownloadFlux;

    public BittorrentAlgorithmImpl(TorrentInfo torrentInfo,
                                   TorrentStatus torrentStatus,
                                   TorrentFileSystemManager torrentFileSystemManager,
                                   Flux<PeersCommunicator> peersCommunicatorFlux) {
        this.startUploadFlux =
                uploadFlux(torrentInfo, torrentStatus, torrentFileSystemManager, peersCommunicatorFlux)
                        .publish();
        this.startUploadFlux.connect();
    }

    private Flux<TorrentPieceChanged> uploadFlux(TorrentInfo torrentInfo,
                                                 TorrentStatus torrentStatus,
                                                 TorrentFileSystemManager torrentFileSystemManager,
                                                 Flux<PeersCommunicator> peersCommunicatorFlux) {
        return peersCommunicatorFlux.flatMap(peersCommunicator ->
                peersCommunicator.receivePeerMessages()
                        .getRequestMessageResponseFlux()
                        .filter(requestMessage -> torrentStatus.isUploading())
                        .flatMap(requestMessage -> torrentFileSystemManager.buildPieceMessage(requestMessage)
                                .onErrorResume(PieceNotDownloadedYetException.class, throwable -> Mono.empty()))
                        .flatMap(pieceMessage ->
                                peersCommunicator.sendPieceMessage(pieceMessage.getIndex(),
                                        pieceMessage.getBegin(), pieceMessage.getBlock())
                                        .map(__ -> new TorrentPieceChanged(pieceMessage.getIndex(),
                                                torrentInfo.getPieces().get(pieceMessage.getIndex()),
                                                TorrentPieceStatus.UPLOADING))));
    }

    private Flux<PeersCommunicator> downloadFlux(TorrentInfo torrentInfo,
                                                 TorrentStatus torrentStatus,
                                                 TorrentFileSystemManager torrentFileSystemManager,
                                                 Flux<PeersCommunicator> peersCommunicatorFlux) {
        return Flux.empty();
    }

    @Override
    public Flux<TorrentPieceChanged> startDownloadFlux() {
        return this.startDownloadFlux;
    }

    @Override
    public Flux<TorrentPieceChanged> startUploadingFlux() {
        return this.startUploadFlux;
    }
}
