package main.algorithms.impl.v1.upload;

import main.TorrentInfo;
import main.algorithms.UploadAlgorithm;
import main.downloader.TorrentPieceChanged;
import main.downloader.TorrentPieceStatus;
import main.file.system.TorrentFileSystemManager;
import main.peer.Link;
import main.torrent.status.TorrentStatus;
import reactor.core.publisher.Flux;

public class UploadAlgorithmImpl implements UploadAlgorithm {
    private TorrentInfo torrentInfo;
    private TorrentStatus torrentStatus;
    private TorrentFileSystemManager torrentFileSystemManager;
    private Flux<Link> peersCommunicatorFlux;

    private Flux<TorrentPieceChanged> uploadedBlocksFlux;

    public UploadAlgorithmImpl(TorrentInfo torrentInfo,
                        TorrentStatus torrentStatus,
                        TorrentFileSystemManager torrentFileSystemManager,
                        Flux<Link> peersCommunicatorFlux) {
        this.torrentInfo = torrentInfo;
        this.torrentStatus = torrentStatus;
        this.torrentFileSystemManager = torrentFileSystemManager;
        this.peersCommunicatorFlux = peersCommunicatorFlux;

        this.uploadedBlocksFlux =
                this.torrentStatus
                        .notifyWhenStartedUploading()
                        .flatMapMany(__ -> this.peersCommunicatorFlux)
                        .flatMap(peersCommunicator ->
                                peersCommunicator.receivePeerMessages()
                                        .getRequestMessageResponseFlux()
                                        .filter(requestMessage -> this.torrentFileSystemManager.havePiece(requestMessage.getIndex()))
                                        .flatMap(requestMessage -> this.torrentStatus.notifyWhenResumeUpload()
                                                .flatMap(___ -> this.torrentFileSystemManager.buildPieceMessage(requestMessage)))
                                        .flatMap(pieceMessage ->
                                                peersCommunicator.sendMessages().sendPieceMessage(pieceMessage.getIndex(),
                                                        pieceMessage.getBegin(), pieceMessage.getBlock())
                                                        .map(___ -> new TorrentPieceChanged(TorrentPieceStatus.UPLOADING, pieceMessage))))
                        .publish()
                        .autoConnect(0);
    }

    @Override
    public Flux<TorrentPieceChanged> getUploadedBlocksFlux() {
        return this.uploadedBlocksFlux;
    }
}
