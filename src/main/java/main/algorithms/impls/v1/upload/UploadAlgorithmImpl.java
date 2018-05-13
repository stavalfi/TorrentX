package main.algorithms.impls.v1.upload;

import main.TorrentInfo;
import main.algorithms.UploadAlgorithm;
import main.downloader.PieceEvent;
import main.downloader.TorrentPieceStatus;
import main.file.system.FileSystemLink;
import main.peer.Link;
import main.torrent.status.Status;
import main.torrent.status.StatusChanger;
import reactor.core.publisher.Flux;

public class UploadAlgorithmImpl implements UploadAlgorithm {
    private TorrentInfo torrentInfo;
    private StatusChanger statusChanger;
    private FileSystemLink fileSystemLink;
    private Flux<Link> peersCommunicatorFlux;

    private Flux<PieceEvent> uploadedBlocksFlux;

    public UploadAlgorithmImpl(TorrentInfo torrentInfo,
                               StatusChanger statusChanger,
                               FileSystemLink fileSystemLink,
                               Flux<Link> peersCommunicatorFlux) {
        this.torrentInfo = torrentInfo;
        this.statusChanger = statusChanger;
        this.fileSystemLink = fileSystemLink;
        this.peersCommunicatorFlux = peersCommunicatorFlux;

        this.uploadedBlocksFlux = this.statusChanger
                .getState$()
                .filter(Status::isStartedUpload)
                .take(1)
                .flatMap(__ -> this.peersCommunicatorFlux)
                .flatMap(peersCommunicator ->
                        // if getRequestMessageResponseFlux is not running on unique thread, then I will block
                        // notifyWhenStartUploading() thread. Watch out.
                        peersCommunicator.receivePeerMessages().getRequestMessageResponseFlux()
                                .filter(requestMessage -> this.fileSystemLink.havePiece(requestMessage.getIndex()))
                                .flatMap(requestMessage -> this.statusChanger.getState$()
                                        .filter(Status::isUploading)
                                        .take(1)
                                        .flatMap(PieceMessage -> this.fileSystemLink.buildPieceMessage(requestMessage))
                                        .flatMap(pieceMessage -> peersCommunicator.sendMessages().sendPieceMessage(pieceMessage)
                                                .map(___ -> new PieceEvent(TorrentPieceStatus.UPLOADING, pieceMessage)))))
                .publish()
                .autoConnect(0);
    }

    @Override
    public Flux<PieceEvent> getUploadedBlocksFlux() {
        return this.uploadedBlocksFlux;
    }
}
