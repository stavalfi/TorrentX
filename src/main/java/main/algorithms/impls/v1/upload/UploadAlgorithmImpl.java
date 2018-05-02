package main.algorithms.impls.v1.upload;

import main.TorrentInfo;
import main.algorithms.UploadAlgorithm;
import main.downloader.PieceEvent;
import main.downloader.TorrentPieceStatus;
import main.file.system.BlocksAllocatorImpl;
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
                .getStatus$()
                .filter(Status::isStartedUpload)
                .take(1)
                .flatMap(__ -> this.peersCommunicatorFlux)
                .flatMap(peersCommunicator ->
                        // if getRequestMessageResponseFlux is not running on unique thread, then I will block
                        // notifyWhenStartUploading() thread. Watch out.
                        peersCommunicator.receivePeerMessages().getRequestMessageResponseFlux()
                                .filter(requestMessage -> this.fileSystemLink.havePiece(requestMessage.getIndex()))
                                .flatMap(requestMessage -> this.statusChanger.getStatus$()
                                        .filter(Status::isUploading)
                                        .take(1)
                                        .flatMap(__ -> BlocksAllocatorImpl.getInstance().allocate())
                                        .flatMap(allocatedBlock ->
                                                this.fileSystemLink.buildPieceMessage(requestMessage, allocatedBlock)
                                                        .flatMap(pieceMessage ->
                                                                peersCommunicator.sendMessages().sendPieceMessage(pieceMessage.getIndex(),
                                                                        pieceMessage.getBegin(), pieceMessage.getBlockLength(), pieceMessage.getAllocatedBlock())
                                                                        .map(___ -> new PieceEvent(TorrentPieceStatus.UPLOADING, pieceMessage)))
                                                        .doOnEach(signal -> {
                                                            // TODO: assert that we didn't miss any signal type or we will have a damn bug or a memory leak!
                                                            if (signal.isOnError() || signal.isOnNext())
                                                                BlocksAllocatorImpl.getInstance().free(allocatedBlock);
                                                        }))))
                .publish()
                .autoConnect(0);
    }

    @Override
    public Flux<PieceEvent> getUploadedBlocksFlux() {
        return this.uploadedBlocksFlux;
    }
}
