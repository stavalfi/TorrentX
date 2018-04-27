package main.algorithms.impls.v1.upload;

import main.TorrentInfo;
import main.algorithms.UploadAlgorithm;
import main.downloader.PieceEvent;
import main.downloader.TorrentPieceStatus;
import main.file.system.FileSystemLink;
import main.peer.Link;
import main.torrent.status.TorrentStatus;
import reactor.core.publisher.Flux;

public class UploadAlgorithmImpl implements UploadAlgorithm {
    private TorrentInfo torrentInfo;
    private TorrentStatus torrentStatus;
    private FileSystemLink fileSystemLink;
    private Flux<Link> peersCommunicatorFlux;

    private Flux<PieceEvent> uploadedBlocksFlux;

    public UploadAlgorithmImpl(TorrentInfo torrentInfo,
                               TorrentStatus torrentStatus,
                               FileSystemLink fileSystemLink,
                               Flux<Link> peersCommunicatorFlux) {
        this.torrentInfo = torrentInfo;
        this.torrentStatus = torrentStatus;
        this.fileSystemLink = fileSystemLink;
        this.peersCommunicatorFlux = peersCommunicatorFlux;

        this.uploadedBlocksFlux = this.torrentStatus
                .notifyWhenStartUploading()
                .flatMapMany(__ -> this.peersCommunicatorFlux)
                .flatMap(peersCommunicator ->
                        // if getRequestMessageResponseFlux is not running on unique thread, then I will block
                        // notifyWhenStartUploading() thread. Watch out.
                        peersCommunicator.receivePeerMessages().getRequestMessageResponseFlux()
                                .filter(requestMessage -> this.fileSystemLink.havePiece(requestMessage.getIndex()))
                                .flatMap(requestMessage -> this.torrentStatus.notifyWhenResumeUpload()
                                        .flatMap(___ -> this.fileSystemLink.buildPieceMessage(requestMessage)))
                                .flatMap(pieceMessage ->
                                        peersCommunicator.sendMessages().sendPieceMessage(pieceMessage.getIndex(),
                                                pieceMessage.getBegin(), pieceMessage.getBlock())
                                                .map(___ -> new PieceEvent(TorrentPieceStatus.UPLOADING, pieceMessage))))
                .publish()
                .autoConnect(0);
    }

    @Override
    public Flux<PieceEvent> getUploadedBlocksFlux() {
        return this.uploadedBlocksFlux;
    }
}
