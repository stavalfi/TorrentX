package main.algorithms;

import main.TorrentInfo;
import main.downloader.TorrentPieceChanged;
import main.downloader.TorrentPieceStatus;
import main.file.system.TorrentFileSystemManager;
import main.peer.Link;
import main.torrent.status.TorrentStatus;
import reactor.core.publisher.Flux;

public class UploadBittorrentAlgorithmImpl {
    private TorrentInfo torrentInfo;
    private TorrentStatus torrentStatus;
    private TorrentFileSystemManager torrentFileSystemManager;
    private Flux<Link> peersCommunicatorFlux;

    private Flux<TorrentPieceChanged> startUploadFlux;

    UploadBittorrentAlgorithmImpl(TorrentInfo torrentInfo,
                                  TorrentStatus torrentStatus,
                                  TorrentFileSystemManager torrentFileSystemManager,
                                  Flux<Link> peersCommunicatorFlux) {
        this.torrentInfo = torrentInfo;
        this.torrentStatus = torrentStatus;
        this.torrentFileSystemManager = torrentFileSystemManager;
        this.peersCommunicatorFlux = peersCommunicatorFlux;

        this.startUploadFlux = uploadFlux()
                .publish()
                .autoConnect(0);
    }

    private Flux<TorrentPieceChanged> uploadFlux() {
        return this.torrentStatus.isStartedUploadingFlux()
                .filter(isUploadingStarted -> isUploadingStarted)
                .flatMap(__ -> this.peersCommunicatorFlux.flatMap(peersCommunicator ->
                        peersCommunicator.receivePeerMessages()
                                .getRequestMessageResponseFlux()
                                .filter(requestMessage -> this.torrentFileSystemManager.havePiece(requestMessage.getIndex()))
                                .flatMap(requestMessage -> this.torrentStatus.isUploadingFlux()
                                        .filter(isUploading -> isUploading)
                                        .flatMap(___ -> this.torrentFileSystemManager.buildPieceMessage(requestMessage)))
                                .flatMap(pieceMessage ->
                                        peersCommunicator.sendMessages().sendPieceMessage(pieceMessage.getIndex(),
                                                pieceMessage.getBegin(), pieceMessage.getBlock())
                                                .map(___ -> new TorrentPieceChanged(TorrentPieceStatus.UPLOADING, pieceMessage)))));
    }

    public Flux<TorrentPieceChanged> startUploadingFlux() {
        return this.startUploadFlux;
    }
}
